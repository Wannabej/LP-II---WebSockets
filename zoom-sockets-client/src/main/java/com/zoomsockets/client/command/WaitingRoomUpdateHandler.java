package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public class WaitingRoomUpdateHandler implements ClientCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientListener listener) {
        listener.onWaitingRoomUpdate(header.getPendingUsers());
    }
}
