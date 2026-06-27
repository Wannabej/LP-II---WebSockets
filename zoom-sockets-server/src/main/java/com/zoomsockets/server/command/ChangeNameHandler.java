package com.zoomsockets.server.command;

import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

import java.util.List;

public class ChangeNameHandler implements ServerCommandHandler {
    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();
        if (usuario == null || roomActivo == null) return;
        
        String newName = header.getNombres();
        if (newName != null && !newName.trim().isEmpty()) {
            usuario.setNombres(newName.trim());
            broadcastActiveUsersList(roomActivo);
            System.out.println("El usuario ID " + usuario.getIdUsuario() + " cambió temporalmente su nombre a: " + usuario.getNombres());
        }
    }

    private void broadcastActiveUsersList(Room room) {
        List<Usuario> activos = room.getActiveUsersList();
        NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createRoomMembersUpdate(activos);
        room.broadcast(update);
    }
}
