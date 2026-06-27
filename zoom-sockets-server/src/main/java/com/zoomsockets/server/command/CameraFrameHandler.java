package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

public class CameraFrameHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null)
            return;

        // Validar seguridad activa
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario()))
            return;

        // Transmisión en tiempo real: Retransmitir inmediatamente a los demás participantes activos
        NetworkFrame relayFrame = com.zoomsockets.protocol.NetworkFrameFactory
                .createCameraFrameRelay(usuario.getIdUsuario(), usuario.getNombres(), frame.getBinaryPayload());
        roomActivo.broadcastExcept(relayFrame, client);
    }
}
