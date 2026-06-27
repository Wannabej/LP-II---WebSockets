package com.zoomsockets.server.command;

import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

import java.io.FileOutputStream;
import java.io.IOException;

public class FileChunkHandler implements ServerCommandHandler {
    private final FileTransferContext fileContext;

    public FileChunkHandler(FileTransferContext fileContext) {
        this.fileContext = fileContext;
    }

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        FileOutputStream fileOutputStream = fileContext.getFileOutputStream();
        if (fileOutputStream == null) return;
        try {
            fileOutputStream.write(frame.getBinaryPayload());
        } catch (IOException e) {
            System.err.println("Error al escribir bloque de archivo: " + e.getMessage());
        }
    }
}
