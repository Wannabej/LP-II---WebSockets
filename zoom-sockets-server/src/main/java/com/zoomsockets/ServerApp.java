package com.zoomsockets;

import com.zoomsockets.db.DatabaseConnection;
import com.zoomsockets.server.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final int PORT = 8080;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool;
    private static boolean running = true;

    public static void main(String[] args) {
        System.out.println("=== SERVIDOR DE SOCKETS ZOOM-SOCKETS ===");
        
        // 1. Inicializar Base de Datos al arranque
        try {
            System.out.println("Cargando conexión a Base de Datos...");
            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("Base de Datos lista y conectada.");
            }
        } catch (Exception e) {
            System.err.println("ERROR CRÍTICO: No se pudo inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // 2. Registrar gancho de apagado ordenado (Shutdown Hook)
        Runtime.getRuntime().addShutdownHook(new Thread(ServerApp::shutdown));

        // 3. Inicializar Thread Pool
        threadPool = Executors.newCachedThreadPool();

        // 4. Iniciar ServerSocket
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor de red escuchando en el puerto: " + PORT);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Despachar el manejo del cliente al pool de hilos
                    threadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (!running) {
                        System.out.println("Servidor detenido.");
                    } else {
                        System.err.println("Error al aceptar conexión de cliente: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el ServerSocket en el puerto " + PORT + ": " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private static synchronized void shutdown() {
        if (!running) return;
        running = false;
        System.out.println("\nApagando servidor de sockets ordenadamente...");
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar ServerSocket: " + e.getMessage());
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
            System.out.println("Pool de hilos finalizado.");
        }
        
        System.out.println("Servidor apagado.");
    }
}
