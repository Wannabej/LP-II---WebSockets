package com.zoomsockets.server;

import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.protocol.ProtocolStreamer;
import com.zoomsockets.protocol.CommandType;
import com.zoomsockets.server.command.*;
import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.db.SolicitudSalaDAO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = true;

    // Estado del cliente en sesión
    private Usuario usuario;
    private Room roomActivo;

    // Contexto de archivos y DAOs para desconexión
    private final FileTransferContext fileContext = new FileTransferContext();
    private final SalaDAO salaDAO = new SalaDAO();
    private final SolicitudSalaDAO solicitudDAO = new SolicitudSalaDAO();

    // Command Map
    private final Map<String, ServerCommandHandler> commandMap = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error al inicializar Streams en ClientHandler: " + e.getMessage());
            close();
        }

        // Inicializar Comandos
        commandMap.put(CommandType.LOGIN_REQUEST.name(), new LoginRequestHandler());
        commandMap.put(CommandType.REGISTER_REQUEST.name(), new RegisterRequestHandler());
        commandMap.put(CommandType.CREATE_ROOM.name(), new CreateRoomHandler());
        commandMap.put(CommandType.JOIN_ROOM_REQUEST.name(), new JoinRoomRequestHandler());
        commandMap.put(CommandType.ADMIT_USER.name(), new AdmitUserHandler());
        commandMap.put(CommandType.CHAT_MESSAGE.name(), new ChatMessageHandler());
        commandMap.put(CommandType.FILE_START.name(), new FileStartHandler(fileContext));
        commandMap.put(CommandType.FILE_CHUNK.name(), new FileChunkHandler(fileContext));
        commandMap.put(CommandType.FILE_END.name(), new FileEndHandler(fileContext));
        commandMap.put(CommandType.FILE_DOWNLOAD_REQUEST.name(), new FileDownloadRequestHandler());
        commandMap.put(CommandType.CAMERA_FRAME.name(), new CameraFrameHandler());
        commandMap.put(CommandType.LEAVE_ROOM.name(), new LeaveRoomHandler());
        commandMap.put(CommandType.CHANGE_NAME_REQUEST.name(), new ChangeNameHandler());
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Room getRoomActivo() {
        return roomActivo;
    }

    public void setRoomActivo(Room roomActivo) {
        this.roomActivo = roomActivo;
    }

    public boolean isRunning() {
        return running;
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
            System.out.println("Cliente desconectado de forma limpia: "
                    + (usuario != null ? usuario.getNombres() : socket.getRemoteSocketAddress()));
        } catch (IOException e) {
            System.err.println("Error en lectura de red del cliente ("
                    + (usuario != null ? usuario.getNombres() : "desconocido") + "): " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * Envía una trama de red al cliente de manera sincronizada sobre el
     * OutputStream.
     */
    public synchronized void sendFrame(NetworkFrame frame) {
        if (!running)
            return;
        try {
            ProtocolStreamer.writeFrame(out, frame);
        } catch (IOException e) {
            System.err.println("Error al enviar trama a " + (usuario != null ? usuario.getNombres() : "desconocido")
                    + ": " + e.getMessage());
            close();
        }
    }

    private void procesarTrama(NetworkFrame frame) {
        try {
            ControlHeader header = ControlHeader.fromJson(frame.getJsonHeader());
            String type = header.getType();
            if (type == null)
                return;

            ServerCommandHandler handler = commandMap.get(type);
            if (handler != null) {
                handler.execute(header, frame, this);
            } else {
                System.err.println("Tipo de comando desconocido: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar trama: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleDisconnect() {
        if (roomActivo != null) {
            Room salaAfectada = roomActivo;
            setRoomActivo(null);

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
                    // Delegamos a RoomManager
                    RoomManager.cerrarSala(salaAfectada.getCodigoSala());
                } else {
                    NetworkFrame chatFrame = com.zoomsockets.protocol.NetworkFrameFactory.createChatBroadcast(salaAfectada.getIdSala(), 0, "Servidor", usuario.getNombres() + " ha abandonado la reunión.");
                    salaAfectada.broadcast(chatFrame);
                    
                    List<Usuario> activos = salaAfectada.getActiveUsersList();
                    NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createRoomMembersUpdate(activos);
                    salaAfectada.broadcast(update);
                    
                    // Si el host sigue conectado, actualizar su sala de espera
                    ClientHandler host = salaAfectada.getHostHandler();
                    if (host != null && host.isRunning()) {
                        List<com.zoomsockets.model.SolicitudSala> pendientes = solicitudDAO.getPendientesPorSala(salaAfectada.getIdSala());
                        NetworkFrame updateEspera = com.zoomsockets.protocol.NetworkFrameFactory.createWaitingRoomUpdate(pendientes);
                        host.sendFrame(updateEspera);
                    }
                }
            }
        }
    }

    private void close() {
        running = false;
        handleDisconnect();
        fileContext.cleanup();

        try {
            if (in != null)
                in.close();
        } catch (IOException e) {
            /* ignored */ }

        try {
            if (out != null)
                out.close();
        } catch (IOException e) {
            /* ignored */ }

        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            /* ignored */ }

        System.out.println("Recursos de conexión liberados.");
    }
}
