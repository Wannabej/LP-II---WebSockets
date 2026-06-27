package com.zoomsockets.server.processor;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.db.SolicitudSalaDAO;
import com.zoomsockets.server.Room;
import com.zoomsockets.model.Sala;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.RoomManager;

import java.util.List;
import java.util.UUID;

public class RoomProcessor {
    private final ClientHandler client;
    private final SalaDAO salaDAO = new SalaDAO();
    private final SolicitudSalaDAO solicitudDAO = new SolicitudSalaDAO();

    public RoomProcessor(ClientHandler client) {
        this.client = client;
    }

    public void handleCreateRoom(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        if (usuario == null) return;

        // Generar código de sala único
        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Sala sala = new Sala();
        sala.setCodigoSala(codigo);
        sala.setNombre(header.getNombreSala());
        sala.setIdHost(usuario.getIdUsuario());

        NetworkFrame frame;
        if (salaDAO.crearSala(sala)) {
            // Guardar host como participante activo en DB
            salaDAO.agregarParticipante(sala.getIdSala(), usuario.getIdUsuario());

            // Registrar en memoria
            Room room = RoomManager.crearRoom(sala.getIdSala(), codigo, sala.getNombre(), client);
            client.setRoomActivo(room);
            
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createCreateRoomResponse(true, null, codigo, sala.getIdSala(), sala.getNombre());
            System.out.println("Sala creada en BD y memoria: " + codigo + " por Host: " + usuario.getNombres());
        } else {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createCreateRoomResponse(false, "No se pudo persistir la sala en la base de datos.", null, 0, null);
        }
        client.sendFrame(frame);
    }

    public void handleJoinRoomRequest(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        if (usuario == null) return;

        String codigo = header.getCodigoSala();
        NetworkFrame frame;
        
        Sala sala = salaDAO.findSalaByCodigo(codigo);
        if (sala == null) {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "La sala no existe o no se encuentra activa.", null, null);
            client.sendFrame(frame);
            return;
        }

        Room roomMemoria = RoomManager.getRoom(codigo);
        if (roomMemoria == null) {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "La sala no está activa en el servidor de red.", null, null);
            client.sendFrame(frame);
            return;
        }

        // Registrar solicitud de acceso en DB como 'Pendiente'
        SolicitudSala solicitud = new SolicitudSala();
        solicitud.setIdSala(sala.getIdSala());
        solicitud.setIdUsuario(usuario.getIdUsuario());
        
        if (solicitudDAO.registrarSolicitud(solicitud)) {
            // Guardar en la cola de espera de memoria
            roomMemoria.agregarASalaDeEspera(client);
            client.setRoomActivo(roomMemoria);

            frame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("PENDING", true, null, sala.getIdSala(), sala.getNombre());
            client.sendFrame(frame);

            // Notificar al Host de la sala en tiempo real con la lista actualizada
            notificarHostDeEspera(roomMemoria);
            System.out.println("Usuario " + usuario.getNombres() + " solicita unirse a " + codigo + " (En Sala de Espera)");
        } else {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "Error al registrar la solicitud de acceso.", null, null);
            client.sendFrame(frame);
        }
    }

    public void handleAdmitUser(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null) return;

        // Validar que el remitente sea el Host de la sala activa
        if (roomActivo.getHostHandler() != client) {
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
            NetworkFrame joinResponse = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("SUCCESS", true, null, roomActivo.getIdSala(), roomActivo.getNombre());
            invitadoHandler.sendFrame(joinResponse);

            // Enviar notificación a todos sobre el nuevo participante
            broadcastActiveUsersList(roomActivo);
            
            // Enviar mensaje automático al chat
            ChatProcessor.enviarMensajeServidor(roomActivo, invitadoHandler.getUsuario().getNombres() + " se ha unido a la reunión.");
            System.out.println("Host admitió a: " + invitadoHandler.getUsuario().getNombres());
        } else {
            // Actualizar DB a 'Rechazada'
            solicitudDAO.actualizarSolicitudPorUsuarioYSala(roomActivo.getIdSala(), idInvitado, "Rechazada");
            // Remover de la cola de memoria
            roomActivo.removerDeSalaDeEspera(invitadoHandler);

            // Notificar al invitado del rechazo
            NetworkFrame joinResponse = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("REJECTED", false, "El host ha rechazado tu solicitud de ingreso.", null, null);
            invitadoHandler.sendFrame(joinResponse);
            System.out.println("Host rechazó a usuario id: " + idInvitado);
        }

        // Actualizar la lista de la sala de espera para el Host
        notificarHostDeEspera(roomActivo);
    }

    public void handleLeaveRoom() {
        Usuario usuario = client.getUsuario();
        if (usuario == null || client.getRoomActivo() == null) return;
        System.out.println("El usuario solicita salir: " + usuario.getNombres());
        handleDisconnect();
    }

    public void handleChangeName(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();
        if (usuario == null || roomActivo == null) return;
        
        String newName = header.getNombres();
        if (newName != null && !newName.trim().isEmpty()) {
            usuario.setNombres(newName.trim());
            broadcastActiveUsersList(roomActivo);
            System.out.println("El usuario ID " + usuario.getIdUsuario() + " cambió temporalmente su nombre a: " + usuario.getNombres());
        }
    }

    public void notificarHostDeEspera(Room room) {
        ClientHandler host = room.getHostHandler();
        if (host != null && host.isRunning()) {
            List<SolicitudSala> pendientes = solicitudDAO.getPendientesPorSala(room.getIdSala());
            NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createWaitingRoomUpdate(pendientes);
            host.sendFrame(update);
        }
    }

    public void broadcastActiveUsersList(Room room) {
        List<Usuario> activos = room.getActiveUsersList();
        NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createRoomMembersUpdate(activos);
        room.broadcast(update);
    }

    public void handleDisconnect() {
        Room roomActivo = client.getRoomActivo();
        Usuario usuario = client.getUsuario();

        if (roomActivo != null) {
            Room salaAfectada = roomActivo;
            client.setRoomActivo(null);

            // Desactivar de la base de datos
            if (usuario != null) {
                salaDAO.removerParticipante(salaAfectada.getIdSala(), usuario.getIdUsuario());
                solicitudDAO.actualizarSolicitudPorUsuarioYSala(salaAfectada.getIdSala(), usuario.getIdUsuario(), "Rechazada");
            }

            // Desvincular de memoria
            salaAfectada.removerParticipante(client);
            salaAfectada.removerDeSalaDeEspera(client);

            if (usuario != null) {
                // Si la sala queda vacía o el Host se desconecta, desactivamos o notificamos
                if (salaAfectada.getHostHandler() == client) {
                    // Delegamos a RoomManager
                    RoomManager.cerrarSala(salaAfectada.getCodigoSala());
                } else {
                    ChatProcessor.enviarMensajeServidor(salaAfectada, usuario.getNombres() + " ha abandonado la reunión.");
                    broadcastActiveUsersList(salaAfectada);
                    // Si el host sigue conectado, actualizar su sala de espera
                    notificarHostDeEspera(salaAfectada);
                }
            }
        }
    }
}
