package com.zoomsockets.server.command;

import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class LeaveRoomHandler implements ServerCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        if (usuario == null || client.getRoomActivo() == null) return;
        System.out.println("El usuario solicita salir: " + usuario.getNombres());
        client.handleDisconnect();
    }
}
