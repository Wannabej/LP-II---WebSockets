package com.zoomsockets.server.processor;

import com.zoomsockets.db.ArchivoCompartidoDAO;
import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.ArchivoCompartido;
import com.zoomsockets.server.Room;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class FileProcessor {
    private final ClientHandler client;
    private final ArchivoCompartidoDAO archivoDAO = new ArchivoCompartidoDAO();
    private final SalaDAO salaDAO = new SalaDAO();

    // Estado temporal para la recepción de archivos en este hilo
    private FileOutputStream fileOutputStream;
    private String nombreArchivoRecibiendo;
    private File archivoTemporal;

    public FileProcessor(ClientHandler client) {
        this.client = client;
    }

    public void handleFileStart(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

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

    public void handleFileChunk(byte[] chunkData) {
        if (fileOutputStream == null) return;
        try {
            fileOutputStream.write(chunkData);
        } catch (IOException e) {
            System.err.println("Error al escribir bloque de archivo: " + e.getMessage());
        }
    }

    public void handleFileEnd() {
        if (fileOutputStream == null) return;
        try {
            fileOutputStream.close();
            System.out.println("Archivo recibido exitosamente: " + nombreArchivoRecibiendo);

            Usuario usuario = client.getUsuario();
            Room roomActivo = client.getRoomActivo();

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
                fileNotification.setIdArchivo(ac.getIdArchivo());

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

    public void handleFileDownloadRequest(ControlHeader header) {
        if (header.getIdArchivo() == null) return;
        
        ArchivoCompartido ac = archivoDAO.getArchivoPorId(header.getIdArchivo());
        ControlHeader res = new ControlHeader("FILE_DOWNLOAD_RESPONSE");
        res.setIdArchivo(header.getIdArchivo());
        
        if (ac != null) {
            File file = new File(ac.getRutaArchivo());
            if (file.exists()) {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    res.setNombreArchivo(ac.getNombreArchivo());
                    client.sendFrame(new NetworkFrame(res.toJson(), data));
                    return;
                } catch (IOException e) {
                    res.setError("Error al leer el archivo físico en el servidor.");
                }
            } else {
                res.setError("El archivo no existe físicamente en el servidor.");
            }
        } else {
            res.setError("El archivo no se encontró en la base de datos.");
        }
        client.sendFrame(new NetworkFrame(res.toJson()));
    }

    public void cleanup() {
        try {
            if (fileOutputStream != null) fileOutputStream.close();
        } catch (IOException e) { /* ignored */ }
    }
}
