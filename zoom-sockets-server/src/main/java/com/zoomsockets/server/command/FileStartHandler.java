package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileStartHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();
    private final FileTransferContext fileContext;

    public FileStartHandler(FileTransferContext fileContext) {
        this.fileContext = fileContext;
    }

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null) return;

        // Validar seguridad activa
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) return;

        String nombreArchivoRecibiendo = header.getNombreArchivo();
        fileContext.setNombreArchivoRecibiendo(nombreArchivoRecibiendo);
        
        // Crear carpeta física de almacenamiento si no existe
        File uploadsDir = new File("uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        // Guardar con prefijo único para evitar colisiones
        String nombreUnico = System.currentTimeMillis() + "_" + nombreArchivoRecibiendo;
        File archivoTemporal = new File(uploadsDir, nombreUnico);
        fileContext.setArchivoTemporal(archivoTemporal);

        try {
            FileOutputStream fos = new FileOutputStream(archivoTemporal);
            fileContext.setFileOutputStream(fos);
            System.out.println("Iniciando recepción de archivo: " + nombreArchivoRecibiendo + " -> Guardando en: " + archivoTemporal.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.err.println("Error al abrir stream de archivo: " + e.getMessage());
            fileContext.setFileOutputStream(null);
        }
    }
}
