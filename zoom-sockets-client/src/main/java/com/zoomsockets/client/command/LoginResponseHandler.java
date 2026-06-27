package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class LoginResponseHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onLoginResponse(
            header.getSuccess(),
            header.getError(),
            header.getNombres(),
            header.getRol(),
            header.getIdUsuario() != null ? header.getIdUsuario() : 0
        );
    }
}
