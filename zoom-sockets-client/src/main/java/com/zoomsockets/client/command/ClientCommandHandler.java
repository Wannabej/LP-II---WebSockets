package com.zoomsockets.client.command;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;

public interface ClientCommandHandler {
    void execute(ControlHeader header, NetworkFrame frame, ClientListener listener);
}
