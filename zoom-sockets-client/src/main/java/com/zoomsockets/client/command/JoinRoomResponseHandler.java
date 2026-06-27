package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class JoinRoomResponseHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onJoinRoomResponse(
            header.getStatus(),
            header.getError(),
            header.getIdSala() != null ? header.getIdSala() : 0,
            header.getNombreSala()
        );
    }
}
