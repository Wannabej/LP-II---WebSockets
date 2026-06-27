package com.zoomsockets.server.command;

import com.zoomsockets.db.ArchivoCompartidoDAO;
import com.zoomsockets.model.ArchivoCompartido;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileDownloadRequestHandler implements ServerCommandHandler {
    private final ArchivoCompartidoDAO archivoDAO = new ArchivoCompartidoDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        if (header.getIdArchivo() == null) return;
        
        ArchivoCompartido ac = archivoDAO.getArchivoPorId(header.getIdArchivo());
        NetworkFrame responseFrame;
        
        if (ac != null) {
            File file = new File(ac.getRutaArchivo());
            if (file.exists()) {
                try {
                    byte[] data = Files.readAllBytes(file.toPath());
                    responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileDownloadResponse(header.getIdArchivo(), ac.getNombreArchivo(), data, null);
                    client.sendFrame(responseFrame);
                    return;
                } catch (IOException e) {
                    responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileDownloadResponse(header.getIdArchivo(), null, null, "Error al leer el archivo físico en el servidor.");
                }
            } else {
                responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileDownloadResponse(header.getIdArchivo(), null, null, "El archivo no existe físicamente en el servidor.");
            }
        } else {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createFileDownloadResponse(header.getIdArchivo(), null, null, "El archivo no se encontró en la base de datos.");
        }
        client.sendFrame(responseFrame);
    }
}
