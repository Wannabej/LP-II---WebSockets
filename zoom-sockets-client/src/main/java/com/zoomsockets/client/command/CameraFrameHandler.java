package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class CameraFrameHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onCameraFrame(
            header.getIdUsuario(),
            header.getNombres(),
            frame.getBinaryPayload()
        );
    }
}
