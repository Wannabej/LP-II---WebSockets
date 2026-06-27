package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.db.SolicitudSalaDAO;
import com.zoomsockets.model.Sala;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;
import com.zoomsockets.server.RoomManager;

import java.util.List;

public class JoinRoomRequestHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();
    private final SolicitudSalaDAO solicitudDAO = new SolicitudSalaDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        if (usuario == null) return;

        String codigo = header.getCodigoSala();
        NetworkFrame responseFrame;
        
        Sala sala = salaDAO.findSalaByCodigo(codigo);
        if (sala == null) {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "La sala no existe o no se encuentra activa.", null, null);
            client.sendFrame(responseFrame);
            return;
        }

        Room roomMemoria = RoomManager.getRoom(codigo);
        if (roomMemoria == null) {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "La sala no está activa en el servidor de red.", null, null);
            client.sendFrame(responseFrame);
            return;
        }

        // Registrar solicitud de acceso en DB como 'Pendiente'
        SolicitudSala solicitud = new SolicitudSala();
        solicitud.setIdSala(sala.getIdSala());
        solicitud.setIdUsuario(usuario.getIdUsuario());
        
        if (solicitudDAO.registrarSolicitud(solicitud)) {
            // Guardar en la cola de espera de memoria
            roomMemoria.agregarASalaDeEspera(client);
            client.setRoomActivo(roomMemoria);

            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("PENDING", true, null, sala.getIdSala(), sala.getNombre());
            client.sendFrame(responseFrame);

            // Notificar al Host de la sala en tiempo real con la lista actualizada
            notificarHostDeEspera(roomMemoria);
            System.out.println("Usuario " + usuario.getNombres() + " solicita unirse a " + codigo + " (En Sala de Espera)");
        } else {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("ERROR", false, "Error al registrar la solicitud de acceso.", null, null);
            client.sendFrame(responseFrame);
        }
    }

    private void notificarHostDeEspera(Room room) {
        ClientHandler host = room.getHostHandler();
        if (host != null && host.isRunning()) {
            List<SolicitudSala> pendientes = solicitudDAO.getPendientesPorSala(room.getIdSala());
            NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createWaitingRoomUpdate(pendientes);
            host.sendFrame(update);
        }
    }
}
