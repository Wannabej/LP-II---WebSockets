package com.zoomsockets.client;

import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.protocol.ProtocolStreamer;
import com.zoomsockets.protocol.CommandType;
import com.zoomsockets.client.command.*;
import javax.swing.SwingUtilities;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ClientService {
    private static ClientService instance;
    
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Thread readThread;
    private boolean connected = false;
    private ClientListener listener;
    private Map<Integer, String> pendingDownloads = new ConcurrentHashMap<>();
    
    /**
     * PATRÓN COMMAND (Comando):
     * Aquí se aplica el patrón Command (o Strategy) para encapsular la lógica de 
     * procesamiento de cada tipo de mensaje (trama) de red.
     * En lugar de tener un switch-case gigante, delegamos la acción a objetos 
     * que implementan la interfaz ClientCommandHandler.
     */
    private final Map<String, ClientCommandHandler> commandMap = new HashMap<>();

    private ClientService() {
        commandMap.put(CommandType.LOGIN_RESPONSE.name(), new LoginResponseHandler());
        commandMap.put(CommandType.REGISTER_RESPONSE.name(), new RegisterResponseHandler());
        commandMap.put(CommandType.CREATE_ROOM_RESPONSE.name(), new CreateRoomResponseHandler());
        commandMap.put(CommandType.JOIN_ROOM_RESPONSE.name(), new JoinRoomResponseHandler());
        commandMap.put(CommandType.WAITING_ROOM_UPDATE.name(), new WaitingRoomUpdateHandler());
        commandMap.put(CommandType.ROOM_MEMBERS_UPDATE.name(), new RoomMembersUpdateHandler());
        commandMap.put(CommandType.CHAT_MESSAGE.name(), new ChatMessageHandler());
        commandMap.put(CommandType.FILE_SHARED.name(), new FileSharedHandler());
        commandMap.put(CommandType.FILE_DOWNLOAD_RESPONSE.name(), new FileDownloadResponseHandler(pendingDownloads));
        commandMap.put(CommandType.CAMERA_FRAME.name(), new CameraFrameHandler());
        commandMap.put(CommandType.ROOM_TERMINATED.name(), new RoomTerminatedHandler());
        commandMap.put(CommandType.ROOM_CLOSED.name(), new RoomClosedHandler());
    }

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

            ClientCommandHandler handler = commandMap.get(type);
            if (handler != null) {
                handler.execute(header, frame, listener);
            } else {
                System.err.println("Comando no reconocido: " + type);
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
            } catch (IOException e) {
                System.err.println("Error de I/O al enviar archivo fragmentado: " + e.getMessage());
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
