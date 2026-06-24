package com.zoomsockets.server;

import com.zoomsockets.db.*;
import com.zoomsockets.model.*;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.protocol.ProtocolStreamer;
import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = true;

    // Estado del cliente en sesión
    private Usuario usuario;
    private Room roomActivo;

    // DAOs para persistencia
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final SalaDAO salaDAO = new SalaDAO();
    private final MensajeDAO mensajeDAO = new MensajeDAO();
    private final ArchivoCompartidoDAO archivoDAO = new ArchivoCompartidoDAO();
    private final SolicitudSalaDAO solicitudDAO = new SolicitudSalaDAO();

    // Estado temporal para la recepción de archivos en este hilo
    private FileOutputStream fileOutputStream;
    private String nombreArchivoRecibiendo;
    private File archivoTemporal;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error al inicializar Streams en ClientHandler: " + e.getMessage());
            close();
        }
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public void run() {
        System.out.println("Cliente conectado desde: " + socket.getRemoteSocketAddress());
        try {
            while (running) {
                // Leer trama enmarcada
                NetworkFrame frame = ProtocolStreamer.readFrame(in);
                
                // Procesar trama
                procesarTrama(frame);
            }
        } catch (EOFException e) {
            System.out.println("Cliente desconectado de forma limpia: " + (usuario != null ? usuario.getNombres() : socket.getRemoteSocketAddress()));
        } catch (Exception e) {
            System.err.println("Error en lectura de red del cliente (" + (usuario != null ? usuario.getNombres() : "desconocido") + "): " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Envía una trama de red al cliente de manera sincronizada sobre el OutputStream.
     */
    public synchronized void sendFrame(NetworkFrame frame) {
        if (!running) return;
        try {
            ProtocolStreamer.writeFrame(out, frame);
        } catch (IOException e) {
            System.err.println("Error al enviar trama a " + (usuario != null ? usuario.getNombres() : "desconocido") + ": " + e.getMessage());
            close();
        }
    }

    private void procesarTrama(NetworkFrame frame) {
        try {
            ControlHeader header = ControlHeader.fromJson(frame.getJsonHeader());
            String type = header.getType();
            if (type == null) return;

            switch (type) {
                case "LOGIN_REQUEST":
                    handleLogin(header);
                    break;
                case "CREATE_ROOM":
                    handleCreateRoom(header);
                    break;
                case "JOIN_ROOM_REQUEST":
                    handleJoinRoomRequest(header);
                    break;
                case "ADMIT_USER":
                    handleAdmitUser(header);
                    break;
                case "CHAT_MESSAGE":
                    handleChatMessage(header);
                    break;
                case "FILE_START":
                    handleFileStart(header);
                    break;
                case "FILE_CHUNK":
                    handleFileChunk(frame.getBinaryPayload());
                    break;
                case "FILE_END":
                    handleFileEnd();
                    break;
                case "CAMERA_FRAME":
                    handleCameraFrame(frame);
                    break;
                case "LEAVE_ROOM":
                    handleLeaveRoom();
                    break;
                default:
                    System.err.println("Tipo de comando desconocido: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar trama: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogin(ControlHeader header) {
        ControlHeader response = new ControlHeader("LOGIN_RESPONSE");
        Usuario user = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        
        if (user != null && usuarioDAO.verificarPassword(header.getPassword(), user.getPasswordHash())) {
            if (user.isActivo()) {
                this.usuario = user;
                response.setStatus("SUCCESS");
                response.setSuccess(true);
                response.setIdUsuario(user.getIdUsuario());
                response.setNombres(user.getNombres());
                response.setRol(user.getRol());
                System.out.println("Autenticación exitosa: " + user.getNombres() + " [" + user.getRol() + "]");
            } else {
                response.setStatus("ERROR");
                response.setSuccess(false);
                response.setError("El usuario está inactivo.");
            }
        } else {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("Credenciales incorrectas.");
        }
        sendFrame(new NetworkFrame(response.toJson()));
    }

    private void handleCreateRoom(ControlHeader header) {
        if (usuario == null) return;

        // Generar código de sala único
        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Sala sala = new Sala();
        sala.setCodigoSala(codigo);
        sala.setNombre(header.getNombreSala());
        sala.setIdHost(usuario.getIdUsuario());

        ControlHeader response = new ControlHeader("CREATE_ROOM_RESPONSE");
        if (salaDAO.crearSala(sala)) {
            // Guardar host como participante activo en DB
            salaDAO.agregarParticipante(sala.getIdSala(), usuario.getIdUsuario());

            // Registrar en memoria
            this.roomActivo = RoomManager.crearRoom(sala.getIdSala(), codigo, sala.getNombre(), this);
            
            response.setStatus("SUCCESS");
            response.setSuccess(true);
            response.setCodigoSala(codigo);
            response.setIdSala(sala.getIdSala());
            response.setNombreSala(sala.getNombre());
            System.out.println("Sala creada en BD y memoria: " + codigo + " por Host: " + usuario.getNombres());
        } else {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("No se pudo persistir la sala en la base de datos.");
        }
        sendFrame(new NetworkFrame(response.toJson()));
    }

    private void handleJoinRoomRequest(ControlHeader header) {
        if (usuario == null) return;

        String codigo = header.getCodigoSala();
        ControlHeader response = new ControlHeader("JOIN_ROOM_RESPONSE");
        
        Sala sala = salaDAO.findSalaByCodigo(codigo);
        if (sala == null) {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("La sala no existe o no se encuentra activa.");
            sendFrame(new NetworkFrame(response.toJson()));
            return;
        }

        Room roomMemoria = RoomManager.getRoom(codigo);
        if (roomMemoria == null) {
            // Si la sala está activa en DB pero no en memoria (ej. por reinicio del servidor), la reconstruimos
            // Para propósitos académicos, asumimos que siempre está en memoria si fue creada en esta ejecución.
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("La sala no está activa en el servidor de red.");
            sendFrame(new NetworkFrame(response.toJson()));
            return;
        }

        // Registrar solicitud de acceso en DB como 'Pendiente'
        SolicitudSala solicitud = new SolicitudSala();
        solicitud.setIdSala(sala.getIdSala());
        solicitud.setIdUsuario(usuario.getIdUsuario());
        
        if (solicitudDAO.registrarSolicitud(solicitud)) {
            // Guardar en la cola de espera de memoria
            roomMemoria.agregarASalaDeEspera(this);
            this.roomActivo = roomMemoria;

            response.setStatus("PENDING");
            response.setSuccess(true); // Indica que la solicitud fue procesada y está en cola
            response.setIdSala(sala.getIdSala());
            response.setNombreSala(sala.getNombre());
            sendFrame(new NetworkFrame(response.toJson()));

            // Notificar al Host de la sala en tiempo real con la lista actualizada
            notificarHostDeEspera(roomMemoria);
            System.out.println("Usuario " + usuario.getNombres() + " solicita unirse a " + codigo + " (En Sala de Espera)");
        } else {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("Error al registrar la solicitud de acceso.");
            sendFrame(new NetworkFrame(response.toJson()));
        }
    }

    private void handleAdmitUser(ControlHeader header) {
        if (usuario == null || roomActivo == null) return;

        // Validar que el remitente sea el Host de la sala activa
        if (roomActivo.getHostHandler() != this) {
            System.err.println("Intento de admisión no autorizado de: " + usuario.getNombres());
            return;
        }

        int idInvitado = header.getIdUsuario();
        String action = header.getAction(); // "ACCEPT" o "REJECT"

        // Encontrar el ClientHandler del invitado en la sala de espera
        ClientHandler invitadoHandler = null;
        synchronized (roomActivo.getSalaDeEspera()) {
            for (ClientHandler h : roomActivo.getSalaDeEspera()) {
                if (h.getUsuario() != null && h.getUsuario().getIdUsuario() == idInvitado) {
                    invitadoHandler = h;
                    break;
                }
            }
        }

        if (invitadoHandler == null) {
            System.err.println("El invitado no se encuentra en la sala de espera de memoria.");
            return;
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            // Actualizar DB a 'Aceptada'
            solicitudDAO.actualizarSolicitudPorUsuarioYSala(roomActivo.getIdSala(), idInvitado, "Aceptada");
            // Agregar participante activo a la BD
            salaDAO.agregarParticipante(roomActivo.getIdSala(), idInvitado);
            // Actualizar memoria
            roomActivo.admitirParticipante(invitadoHandler);

            // Notificar al invitado que ha sido admitido
            ControlHeader joinResponse = new ControlHeader("JOIN_ROOM_RESPONSE");
            joinResponse.setStatus("SUCCESS");
            joinResponse.setSuccess(true);
            joinResponse.setIdSala(roomActivo.getIdSala());
            joinResponse.setNombreSala(roomActivo.getNombre());
            invitadoHandler.sendFrame(new NetworkFrame(joinResponse.toJson()));

            // Enviar notificación a todos sobre el nuevo participante
            broadcastActiveUsersList(roomActivo);
            
            // Enviar mensaje automático al chat
            enviarMensajeServidor(roomActivo, invitadoHandler.getUsuario().getNombres() + " se ha unido a la reunión.");
            System.out.println("Host admitió a: " + invitadoHandler.getUsuario().getNombres());
        } else {
            // Actualizar DB a 'Rechazada'
            solicitudDAO.actualizarSolicitudPorUsuarioYSala(roomActivo.getIdSala(), idInvitado, "Rechazada");
            // Remover de la cola de memoria
            roomActivo.removerDeSalaDeEspera(invitadoHandler);

            // Notificar al invitado del rechazo
            ControlHeader joinResponse = new ControlHeader("JOIN_ROOM_RESPONSE");
            joinResponse.setStatus("REJECTED");
            joinResponse.setSuccess(false);
            joinResponse.setError("El host ha rechazado tu solicitud de ingreso.");
            invitadoHandler.sendFrame(new NetworkFrame(joinResponse.toJson()));
            System.out.println("Host rechazó a usuario id: " + idInvitado);
        }

        // Actualizar la lista de la sala de espera para el Host
        notificarHostDeEspera(roomActivo);
    }

    private void handleChatMessage(ControlHeader header) {
        if (usuario == null || roomActivo == null) return;

        // SEGURIDAD CRÍTICA: Validar que el remitente pertenezca activamente a la sala en DB
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) {
            System.err.println("Bloqueado intento de chat de participante inactivo o rechazado: " + usuario.getNombres());
            return;
        }

        // Persistir en la Base de Datos
        Mensaje msg = new Mensaje();
        msg.setIdSala(roomActivo.getIdSala());
        msg.setIdUsuario(usuario.getIdUsuario());
        msg.setContenido(header.getContenido());
        
        if (mensajeDAO.registrarMensaje(msg)) {
            msg.setNombreUsuario(usuario.getNombres());
            
            // Difundir sincrónicamente a todos los miembros de la sala
            ControlHeader chatBroadcast = new ControlHeader("CHAT_MESSAGE");
            chatBroadcast.setIdSala(roomActivo.getIdSala());
            chatBroadcast.setIdUsuario(usuario.getIdUsuario());
            chatBroadcast.setNombres(usuario.getNombres());
            chatBroadcast.setContenido(msg.getContenido());
            
            roomActivo.broadcast(new NetworkFrame(chatBroadcast.toJson()));
        }
    }

    private void handleFileStart(ControlHeader header) {
        if (usuario == null || roomActivo == null) return;

        // Validar seguridad activa
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) return;

        this.nombreArchivoRecibiendo = header.getNombreArchivo();
        
        // Crear carpeta física de almacenamiento si no existe
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        // Guardar con prefijo único para evitar colisiones
        String nombreUnico = System.currentTimeMillis() + "_" + nombreArchivoRecibiendo;
        this.archivoTemporal = new File(uploadsDir, nombreUnico);

        try {
            this.fileOutputStream = new FileOutputStream(archivoTemporal);
            System.out.println("Iniciando recepción de archivo: " + nombreArchivoRecibiendo + " -> Guardando en: " + archivoTemporal.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.err.println("Error al abrir stream de archivo: " + e.getMessage());
            this.fileOutputStream = null;
        }
    }

    private void handleFileChunk(byte[] chunkData) {
        if (fileOutputStream == null) return;
        try {
            fileOutputStream.write(chunkData);
        } catch (IOException e) {
            System.err.println("Error al escribir bloque de archivo: " + e.getMessage());
        }
    }

    private void handleFileEnd() {
        if (fileOutputStream == null) return;
        try {
            fileOutputStream.close();
            System.out.println("Archivo recibido exitosamente: " + nombreArchivoRecibiendo);

            // Persistir metadatos en base de datos
            ArchivoCompartido ac = new ArchivoCompartido();
            ac.setIdSala(roomActivo.getIdSala());
            ac.setIdUsuario(usuario.getIdUsuario());
            ac.setNombreArchivo(nombreArchivoRecibiendo);
            ac.setRutaArchivo(archivoTemporal.getPath().replace("\\", "/")); // Guardar ruta relativa normalizada

            if (archivoDAO.registrarArchivo(ac)) {
                ac.setNombreUsuario(usuario.getNombres());

                // Notificar al canal sobre el nuevo archivo disponible
                ControlHeader fileNotification = new ControlHeader("FILE_SHARED");
                fileNotification.setIdSala(roomActivo.getIdSala());
                fileNotification.setIdUsuario(usuario.getIdUsuario());
                fileNotification.setNombres(usuario.getNombres());
                fileNotification.setNombreArchivo(nombreArchivoRecibiendo);
                fileNotification.setContenido(archivoTemporal.getName()); // Usar el nombre físico único para descargas

                roomActivo.broadcast(new NetworkFrame(fileNotification.toJson()));
            }
        } catch (IOException e) {
            System.err.println("Error al finalizar guardado de archivo: " + e.getMessage());
        } finally {
            fileOutputStream = null;
            nombreArchivoRecibiendo = null;
            archivoTemporal = null;
        }
    }

    private void handleCameraFrame(NetworkFrame frame) {
        if (usuario == null || roomActivo == null) return;

        // Validar seguridad activa
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) return;

        // Transmisión en tiempo real: Retransmitir inmediatamente a los demás participantes activos
        // Agregamos al JSON el id y nombre de quien envía el frame para que el cliente sepa a qué panel corresponde
        ControlHeader jsonHeader = ControlHeader.fromJson(frame.getJsonHeader());
        jsonHeader.setIdUsuario(usuario.getIdUsuario());
        jsonHeader.setNombres(usuario.getNombres());

        NetworkFrame relayFrame = new NetworkFrame(jsonHeader.toJson(), frame.getBinaryPayload());
        roomActivo.broadcastExcept(relayFrame, this);
    }

    private void handleLeaveRoom() {
        if (usuario == null || roomActivo == null) return;
        System.out.println("El usuario solicita salir: " + usuario.getNombres());
        limpiarYNotificarSalida();
    }

    private void notificarHostDeEspera(Room room) {
        ClientHandler host = room.getHostHandler();
        if (host != null && host.running) {
            List<SolicitudSala> pendientes = solicitudDAO.getPendientesPorSala(room.getIdSala());
            
            ControlHeader update = new ControlHeader("WAITING_ROOM_UPDATE");
            update.setPendingUsers(pendientes);
            host.sendFrame(new NetworkFrame(update.toJson()));
        }
    }

    private void broadcastActiveUsersList(Room room) {
        List<Usuario> activos = room.getActiveUsersList();
        ControlHeader update = new ControlHeader("ROOM_MEMBERS_UPDATE");
        update.setActiveUsers(activos);
        room.broadcast(new NetworkFrame(update.toJson()));
    }

    private void enviarMensajeServidor(Room room, String contenido) {
        ControlHeader serverMsg = new ControlHeader("CHAT_MESSAGE");
        serverMsg.setIdSala(room.getIdSala());
        serverMsg.setIdUsuario(0); // ID 0 representa al servidor
        serverMsg.setNombres("SISTEMA");
        serverMsg.setContenido(contenido);
        room.broadcast(new NetworkFrame(serverMsg.toJson()));
    }

    private void limpiarYNotificarSalida() {
        if (roomActivo != null) {
            Room salaAfectada = roomActivo;
            this.roomActivo = null;

            // Desactivar de la base de datos
            if (usuario != null) {
                salaDAO.removerParticipante(salaAfectada.getIdSala(), usuario.getIdUsuario());
                solicitudDAO.actualizarSolicitudPorUsuarioYSala(salaAfectada.getIdSala(), usuario.getIdUsuario(), "Rechazada");
            }

            // Desvincular de memoria
            salaAfectada.removerParticipante(this);
            salaAfectada.removerDeSalaDeEspera(this);

            if (usuario != null) {
                // Si la sala queda vacía o el Host se desconecta, desactivamos o notificamos
                if (salaAfectada.getHostHandler() == this) {
                    enviarMensajeServidor(salaAfectada, "El host (" + usuario.getNombres() + ") ha finalizado la reunión.");
                    salaDAO.finalizarSala(salaAfectada.getIdSala());
                    RoomManager.removerRoom(salaAfectada.getCodigoSala());
                    
                    // Notificar a todos para salir de la sala
                    ControlHeader endRoom = new ControlHeader("ROOM_TERMINATED");
                    salaAfectada.broadcast(new NetworkFrame(endRoom.toJson()));
                } else {
                    enviarMensajeServidor(salaAfectada, usuario.getNombres() + " ha abandonado la reunión.");
                    broadcastActiveUsersList(salaAfectada);
                    // Si el host sigue conectado, actualizar su sala de espera
                    notificarHostDeEspera(salaAfectada);
                }
            }
        }
    }

    private void close() {
        running = false;
        limpiarYNotificarSalida();
        
        try {
            if (fileOutputStream != null) fileOutputStream.close();
        } catch (IOException e) { /* ignored */ }
        
        try {
            if (in != null) in.close();
        } catch (IOException e) { /* ignored */ }
        
        try {
            if (out != null) out.close();
        } catch (IOException e) { /* ignored */ }
        
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignored */ }
        
        System.out.println("Recursos de conexión liberados.");
    }
}
