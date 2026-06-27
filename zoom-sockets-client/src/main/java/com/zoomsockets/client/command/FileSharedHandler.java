package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class FileSharedHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onFileShared(
            header.getNombres(),
            header.getNombreArchivo(),
            header.getContenido(), // Contiene el nombre único físico guardado en el servidor
            header.getIdArchivo() != null ? header.getIdArchivo() : 0
        );
    }
}
