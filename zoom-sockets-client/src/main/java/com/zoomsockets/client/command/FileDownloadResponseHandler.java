package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class FileDownloadResponseHandler implements ClientCommandHandler {
    private final Map<Integer, String> pendingDownloads;

    public FileDownloadResponseHandler(Map<Integer, String> pendingDownloads) {
        this.pendingDownloads = pendingDownloads;
    }

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
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
    }
}
