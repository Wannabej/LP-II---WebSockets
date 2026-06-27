package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class CreateRoomResponseHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onCreateRoomResponse(
            header.getSuccess(),
            header.getError(),
            header.getCodigoSala(),
            header.getNombreSala(),
            header.getIdSala() != null ? header.getIdSala() : 0
        );
    }
}
