package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

public class ChatMessageHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null) return;

        // Validar que pertenece a la sala actual
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) return;

        NetworkFrame relayFrame = com.zoomsockets.protocol.NetworkFrameFactory.createChatBroadcast(roomActivo.getIdSala(), usuario.getIdUsuario(), usuario.getNombres(), header.getContenido());
        roomActivo.broadcast(relayFrame);
        System.out.println("Chat [" + roomActivo.getCodigoSala() + "] " + usuario.getNombres() + ": " + header.getContenido());
    }
}
