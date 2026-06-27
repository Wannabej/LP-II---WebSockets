package com.zoomsockets.server.command;

import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public interface ServerCommandHandler {
    void execute(ControlHeader header, NetworkFrame frame, ClientHandler client);
}
