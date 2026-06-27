package com.zoomsockets.server.command;

import com.zoomsockets.db.ArchivoCompartidoDAO;
import com.zoomsockets.model.ArchivoCompartido;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

import java.io.IOException;

public class FileEndHandler implements ServerCommandHandler {
    private final ArchivoCompartidoDAO archivoDAO = new ArchivoCompartidoDAO();
    private final FileTransferContext fileContext;

    public FileEndHandler(FileTransferContext fileContext) {
        this.fileContext = fileContext;
    }

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        if (fileContext.getFileOutputStream() == null) return;
        try {
            fileContext.getFileOutputStream().close();
            System.out.println("Archivo recibido exitosamente: " + fileContext.getNombreArchivoRecibiendo());

            Usuario usuario = client.getUsuario();
            Room roomActivo = client.getRoomActivo();

            // Persistir metadatos en base de datos
            ArchivoCompartido ac = new ArchivoCompartido();
            ac.setIdSala(roomActivo.getIdSala());
            ac.setIdUsuario(usuario.getIdUsuario());
            ac.setNombreArchivo(fileContext.getNombreArchivoRecibiendo());
            ac.setRutaArchivo(fileContext.getArchivoTemporal().getPath().replace("\\", "/")); // Guardar ruta relativa normalizada

            if (archivoDAO.registrarArchivo(ac)) {
                ac.setNombreUsuario(usuario.getNombres());

                // Notificar al canal sobre el nuevo archivo disponible
                NetworkFrame fileNotification = com.zoomsockets.protocol.NetworkFrameFactory.createFileSharedNotification(roomActivo.getIdSala(), usuario.getIdUsuario(), usuario.getNombres(), fileContext.getNombreArchivoRecibiendo(), fileContext.getArchivoTemporal().getName(), ac.getIdArchivo());
                roomActivo.broadcast(fileNotification);
            }
        } catch (IOException e) {
            System.err.println("Error al finalizar guardado de archivo: " + e.getMessage());
        } finally {
            fileContext.cleanup();
        }
    }
}
