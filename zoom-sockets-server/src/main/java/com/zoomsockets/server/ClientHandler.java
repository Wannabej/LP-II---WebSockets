package com.zoomsockets.server;

import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.protocol.ProtocolStreamer;
import com.zoomsockets.server.processor.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean running = true;

    // Estado del cliente en sesión
    private Usuario usuario;
    private Room roomActivo;

    // Procesadores por dominio
    private final AuthProcessor authProcessor = new AuthProcessor(this);
    private final RoomProcessor roomProcessor = new RoomProcessor(this);
    private final ChatProcessor chatProcessor = new ChatProcessor(this);
    private final FileProcessor fileProcessor = new FileProcessor(this);
    private final MediaProcessor mediaProcessor = new MediaProcessor(this);

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
        } catch (Exception e) {
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

            switch (type) {
                case "LOGIN_REQUEST":
                    authProcessor.handleLogin(header);
                    break;
                case "REGISTER_REQUEST":
                    authProcessor.handleRegister(header);
                    break;
                case "CREATE_ROOM":
                    roomProcessor.handleCreateRoom(header);
                    break;
                case "JOIN_ROOM_REQUEST":
                    roomProcessor.handleJoinRoomRequest(header);
                    break;
                case "ADMIT_USER":
                    roomProcessor.handleAdmitUser(header);
                    break;
                case "CHAT_MESSAGE":
                    chatProcessor.handleChatMessage(header);
                    break;
                case "FILE_START":
                    fileProcessor.handleFileStart(header);
                    break;
                case "FILE_CHUNK":
                    fileProcessor.handleFileChunk(frame.getBinaryPayload());
                    break;
                case "FILE_END":
                    fileProcessor.handleFileEnd();
                    break;
                case "FILE_DOWNLOAD_REQUEST":
                    fileProcessor.handleFileDownloadRequest(header);
                    break;
                case "CAMERA_FRAME":
                    mediaProcessor.handleCameraFrame(frame);
                    break;
                case "LEAVE_ROOM":
                    roomProcessor.handleLeaveRoom();
                    break;
                case "CHANGE_NAME_REQUEST":
                    roomProcessor.handleChangeName(header);
                    break;
                default:
                    System.err.println("Tipo de comando desconocido: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error al procesar trama: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void close() {
        running = false;
        roomProcessor.handleDisconnect();
        fileProcessor.cleanup();

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
