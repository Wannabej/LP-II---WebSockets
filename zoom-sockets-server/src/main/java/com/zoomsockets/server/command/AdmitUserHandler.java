package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.db.SolicitudSalaDAO;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;

import java.util.List;

public class AdmitUserHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();
    private final SolicitudSalaDAO solicitudDAO = new SolicitudSalaDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null) return;

        // Validar que el remitente sea el Host de la sala activa
        if (roomActivo.getHostHandler() != client) {
            System.err.println("Intento de admisión no autorizado de: " + usuario.getNombres());
            return;
        }

        int idInvitado = header.getIdUsuario();
        String action = header.getAction(); // "ACCEPT" o "REJECT"

        // Encontrar el ClientHandler del invitado en la sala de espera
        ClientHandler invitadoHandler = null;
        synchronized (roomActivo.getSalaDeEspera()) {
            for (ClientHandler h : roomActivo.getSalaDeEspera()) {
                if (h.getUsuario() != null && h.getUsuario().getIdUsuario() == idInvitado) {
                    invitadoHandler = h;
                    break;
                }
            }
        }

        if (invitadoHandler == null) {
            System.err.println("El invitado no se encuentra en la sala de espera de memoria.");
            return;
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            // Actualizar DB a 'Aceptada'
            solicitudDAO.actualizarSolicitudPorUsuarioYSala(roomActivo.getIdSala(), idInvitado, "Aceptada");
            // Agregar participante activo a la BD
            salaDAO.agregarParticipante(roomActivo.getIdSala(), idInvitado);
            // Actualizar memoria
            roomActivo.admitirParticipante(invitadoHandler);

            // Notificar al invitado que ha sido admitido
            NetworkFrame joinResponse = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("SUCCESS", true, null, roomActivo.getIdSala(), roomActivo.getNombre());
            invitadoHandler.sendFrame(joinResponse);

            // Enviar notificación a todos sobre el nuevo participante
            broadcastActiveUsersList(roomActivo);
            
            // Enviar mensaje automático al chat
            enviarMensajeServidor(roomActivo, invitadoHandler.getUsuario().getNombres() + " se ha unido a la reunión.");
            System.out.println("Host admitió a: " + invitadoHandler.getUsuario().getNombres());
        } else {
            // Actualizar DB a 'Rechazada'
            solicitudDAO.actualizarSolicitudPorUsuarioYSala(roomActivo.getIdSala(), idInvitado, "Rechazada");
            // Remover de la cola de memoria
            roomActivo.removerDeSalaDeEspera(invitadoHandler);

            // Notificar al invitado del rechazo
            NetworkFrame joinResponse = com.zoomsockets.protocol.NetworkFrameFactory.createJoinRoomResponse("REJECTED", false, "El host ha rechazado tu solicitud de ingreso.", null, null);
            invitadoHandler.sendFrame(joinResponse);
            System.out.println("Host rechazó a usuario id: " + idInvitado);
        }

        // Actualizar la lista de la sala de espera para el Host
        notificarHostDeEspera(roomActivo);
    }

    private void notificarHostDeEspera(Room room) {
        ClientHandler host = room.getHostHandler();
        if (host != null && host.isRunning()) {
            List<SolicitudSala> pendientes = solicitudDAO.getPendientesPorSala(room.getIdSala());
            NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createWaitingRoomUpdate(pendientes);
            host.sendFrame(update);
        }
    }

    private void broadcastActiveUsersList(Room room) {
        List<Usuario> activos = room.getActiveUsersList();
        NetworkFrame update = com.zoomsockets.protocol.NetworkFrameFactory.createRoomMembersUpdate(activos);
        room.broadcast(update);
    }

    private void enviarMensajeServidor(Room room, String mensaje) {
        NetworkFrame chatFrame = com.zoomsockets.protocol.NetworkFrameFactory.createChatBroadcast(room.getIdSala(), 0, "Servidor", mensaje);
        room.broadcast(chatFrame);
    }
}
