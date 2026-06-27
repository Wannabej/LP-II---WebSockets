package com.zoomsockets.client;

import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.protocol.ProtocolStreamer;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;

public class ClientService {
    private static ClientService instance;
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readThread;
    private boolean connected = false;
    private ClientListener listener;
    private java.util.Map<Integer, String> pendingDownloads = new java.util.concurrent.ConcurrentHashMap<>();

    private ClientService() {}

    public static synchronized ClientService getInstance() {
        if (instance == null) {
            instance = new ClientService();
        }
        return instance;
    }

    public synchronized void connect(String host, int port) throws IOException {
        if (connected) return;

        this.socket = new Socket(host, port);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.connected = true;

        // Iniciar hilo de escucha de red
        this.readThread = new Thread(this::listen, "ZoomSockets-ReadThread");
        this.readThread.setDaemon(true);
        this.readThread.start();
        System.out.println("Conectado al servidor: " + host + ":" + port);
    }

    public synchronized void disconnect() {
        if (!connected) return;
        connected = false;

        try {
            if (in != null) in.close();
        } catch (IOException e) { /* ignore */ }
        
        try {
            if (out != null) out.close();
        } catch (IOException e) { /* ignore */ }
        
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignore */ }

        System.out.println("Desconectado del servidor.");
    }

    public synchronized void sendFrame(NetworkFrame frame) {
        if (!connected) return;
        try {
            ProtocolStreamer.writeFrame(out, frame);
        } catch (IOException e) {
            System.err.println("Error al enviar trama: " + e.getMessage());
            disconnect();
        }
    }

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    private void listen() {
        try {
            while (connected) {
                NetworkFrame frame = ProtocolStreamer.readFrame(in);
                
                // Despachar el procesamiento en el hilo de UI de Swing
                SwingUtilities.invokeLater(() -> procesarTramaRecibida(frame));
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Conexión con el servidor perdida: " + e.getMessage());
                disconnect();
                SwingUtilities.invokeLater(() -> {
                    if (listener != null) listener.onRoomTerminated();
                });
            }
        }
    }

    private void procesarTramaRecibida(NetworkFrame frame) {
        if (listener == null) return;
        
        try {
            ControlHeader header = ControlHeader.fromJson(frame.getJsonHeader());
            String type = header.getType();
            if (type == null) return;

            switch (type) {
                case "LOGIN_RESPONSE":
                    listener.onLoginResponse(
                        header.getSuccess(),
                        header.getError(),
                        header.getNombres(),
                        header.getRol(),
                        header.getIdUsuario() != null ? header.getIdUsuario() : 0
                    );
                    break;
                case "REGISTER_RESPONSE":
                    listener.onRegisterResponse(
                        header.getSuccess(),
                        header.getError()
                    );
                    break;
                case "CREATE_ROOM_RESPONSE":
                    listener.onCreateRoomResponse(
                        header.getSuccess(),
                        header.getError(),
                        header.getCodigoSala(),
                        header.getNombreSala(),
                        header.getIdSala() != null ? header.getIdSala() : 0
                    );
                    break;
                case "JOIN_ROOM_RESPONSE":
                    listener.onJoinRoomResponse(
                        header.getStatus(),
                        header.getError(),
                        header.getIdSala() != null ? header.getIdSala() : 0,
                        header.getNombreSala()
                    );
                    break;
                case "WAITING_ROOM_UPDATE":
                    listener.onWaitingRoomUpdate(header.getPendingUsers());
                    break;
                case "ROOM_MEMBERS_UPDATE":
                    listener.onRoomMembersUpdate(header.getActiveUsers());
                    break;
                case "CHAT_MESSAGE":
                    listener.onChatMessage(
                        header.getNombres(),
                        header.getContenido(),
                        header.getIdUsuario()
                    );
                    break;
                case "FILE_SHARED":
                    listener.onFileShared(
                        header.getNombres(),
                        header.getNombreArchivo(),
                        header.getContenido(), // Contiene el nombre único físico guardado en el servidor
                        header.getIdArchivo() != null ? header.getIdArchivo() : 0
                    );
                    break;
                case "FILE_DOWNLOAD_RESPONSE":
                    if (header.getError() != null) {
                        listener.onFileDownloadFailed(header.getError());
                    } else {
                        String dest = pendingDownloads.remove(header.getIdArchivo());
                        if (dest != null && frame.getBinaryPayload() != null) {
                            try (FileOutputStream fos = new FileOutputStream(dest)) {
                                fos.write(frame.getBinaryPayload());
                                listener.onFileDownloadComplete(dest);
                            } catch (IOException e) {
                                listener.onFileDownloadFailed("Error local al guardar: " + e.getMessage());
                            }
                        } else if (dest != null) {
                            listener.onFileDownloadFailed("El servidor no envió datos binarios.");
                        }
                    }
                    break;
                case "CAMERA_FRAME":
                    listener.onCameraFrame(
                        header.getIdUsuario(),
                        header.getNombres(),
                        frame.getBinaryPayload()
                    );
                    break;
                case "ROOM_TERMINATED":
                    listener.onRoomTerminated();
                    break;
                case "ROOM_CLOSED":
                    listener.onRoomClosed("El anfitrión ha finalizado la reunión.");
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error procesando trama en cliente: " + e.getMessage());
        }
    }

    /**
     * Envía un archivo local dividiéndolo en fragmentos (chunks) sobre el socket.
     */
    public void sendFile(int idSala, int idUsuario, File file) {
        new Thread(() -> {
            try {
                long fileSize = file.length();
                String fileName = file.getName();

                // 1. Enviar trama FILE_START con metadatos
                NetworkFrame startFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileStartFrame(idSala, idUsuario, fileName, fileSize);
                sendFrame(startFrame);

                // 2. Leer archivo y enviar en fragmentos de 8 KB
                byte[] buffer = new byte[8192];
                int bytesRead;
                try (FileInputStream fis = new FileInputStream(file)) {
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        byte[] chunk = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                        NetworkFrame chunkFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileChunkFrame(chunk);
                        sendFrame(chunkFrame);
                    }
                }

                // 3. Enviar trama FILE_END
                NetworkFrame endFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileEndFrame();
                sendFrame(endFrame);
                
                System.out.println("Archivo enviado completamente por partes: " + fileName);
            } catch (Exception e) {
                System.err.println("Error al enviar archivo fragmentado: " + e.getMessage());
            }
        }, "FileSenderThread").start();
    }

    public void requestFileDownload(int idArchivo, String rutaDestinoLocal) {
        if (!connected) return;
        pendingDownloads.put(idArchivo, rutaDestinoLocal);
        NetworkFrame reqFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileDownloadRequest(idArchivo);
        sendFrame(reqFrame);
    }
}
