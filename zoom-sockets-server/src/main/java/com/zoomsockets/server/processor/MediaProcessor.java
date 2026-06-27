package com.zoomsockets.server.processor;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.server.Room;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class MediaProcessor {
    private final ClientHandler client;
    private final SalaDAO salaDAO = new SalaDAO();

    public MediaProcessor(ClientHandler client) {
        this.client = client;
    }

    public void handleCameraFrame(NetworkFrame frame) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null)
            return;

        // Validar seguridad activa
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario()))
            return;

        // Transmisión en tiempo real: Retransmitir inmediatamente a los demás
        // participantes activos
        NetworkFrame relayFrame = com.zoomsockets.protocol.NetworkFrameFactory
                .createCameraFrameRelay(usuario.getIdUsuario(), usuario.getNombres(), frame.getBinaryPayload());
        roomActivo.broadcastExcept(relayFrame, client);
    }
}
