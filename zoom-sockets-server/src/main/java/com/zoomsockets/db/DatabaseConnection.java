package com.zoomsockets.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import java.io.File;

public class DatabaseConnection {
    private static String dbType = "sqlite";
    private static String dbUrl = "jdbc:sqlite:zoom_sockets.db";
    private static String dbUser = "";
    private static String dbPassword = "";
    private static boolean isInitialized = false;

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties prop = new Properties();
            if (input != null) {
                prop.load(input);
                dbType = prop.getProperty("db.type", "sqlite").trim().toLowerCase();
                if ("sqlite".equals(dbType)) {
                    dbUrl = prop.getProperty("db.sqlite.url", "jdbc:sqlite:zoom_sockets.db").trim();
                } else if ("postgresql".equals(dbType)) {
                    dbUrl = prop.getProperty("db.postgresql.url").trim();
                    dbUser = prop.getProperty("db.postgresql.user", "").trim();
                    dbPassword = prop.getProperty("db.postgresql.password", "").trim();
                } else if ("mysql".equals(dbType)) {
                    dbUrl = prop.getProperty("db.mysql.url").trim();
                    dbUser = prop.getProperty("db.mysql.user", "").trim();
                    dbPassword = prop.getProperty("db.mysql.password", "").trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo cargar db.properties, usando SQLite por defecto: " + e.getMessage());
        }
    }

    public static synchronized Connection getConnection() throws Exception {
        Connection conn;
        if ("sqlite".equals(dbType)) {
            // Verificar si la BD ya existe físicamente para evitar reinicializaciones redundantes
            boolean dbFileExists = new File("zoom_sockets.db").exists();
            
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(dbUrl);
            
            // Habilitar claves foráneas en SQLite
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            if (!dbFileExists || !isInitialized) {
                initializeDatabase(conn);
                isInitialized = true;
            }
        } else if ("postgresql".equals(dbType)) {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            if (!isInitialized) {
                initializeDatabase(conn);
                isInitialized = true;
            }
        } else { // mysql
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            if (!isInitialized) {
                initializeDatabase(conn);
                isInitialized = true;
            }
        }
        return conn;
    }

    private static void initializeDatabase(Connection conn) {
        try (InputStream schemaStream = DatabaseConnection.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (schemaStream == null) {
                System.err.println("Error: No se encontró schema.sql en resources.");
                return;
            }
            System.out.println("Inicializando base de datos...");
            BufferedReader reader = new BufferedReader(new InputStreamReader(schemaStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Omitir líneas de comentarios
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sb.append(line).append("\n");
            }
            
            // Dividir los comandos por punto y coma (;)
            String[] commands = sb.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String command : commands) {
                    String cmdTrimmed = command.trim();
                    if (!cmdTrimmed.isEmpty()) {
                        stmt.execute(cmdTrimmed);
                    }
                }
            }
            System.out.println("Base de datos inicializada correctamente con tablas y datos semilla.");
        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
