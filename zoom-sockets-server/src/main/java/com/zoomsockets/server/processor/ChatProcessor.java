package com.zoomsockets.server.processor;

import com.zoomsockets.db.MensajeDAO;
import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.Mensaje;
import com.zoomsockets.server.Room;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class ChatProcessor {
    private final ClientHandler client;
    private final MensajeDAO mensajeDAO = new MensajeDAO();
    private final SalaDAO salaDAO = new SalaDAO();

    public ChatProcessor(ClientHandler client) {
        this.client = client;
    }

    public void handleChatMessage(ControlHeader header) {
        Usuario usuario = client.getUsuario();
        Room roomActivo = client.getRoomActivo();

        if (usuario == null || roomActivo == null) return;

        // SEGURIDAD CRÍTICA: Validar que el remitente pertenezca activamente a la sala en DB
        if (!salaDAO.isParticipanteActivo(roomActivo.getIdSala(), usuario.getIdUsuario())) {
            System.err.println("Bloqueado intento de chat de participante inactivo o rechazado: " + usuario.getNombres());
            return;
        }

        // Persistir en la Base de Datos
        Mensaje msg = new Mensaje();
        msg.setIdSala(roomActivo.getIdSala());
        msg.setIdUsuario(usuario.getIdUsuario());
        msg.setContenido(header.getContenido());
        
        if (mensajeDAO.registrarMensaje(msg)) {
            msg.setNombreUsuario(usuario.getNombres());
            
            // Difundir sincrónicamente a todos los miembros de la sala
            NetworkFrame chatBroadcast = com.zoomsockets.protocol.NetworkFrameFactory.createChatBroadcast(roomActivo.getIdSala(), usuario.getIdUsuario(), usuario.getNombres(), msg.getContenido());
            roomActivo.broadcast(chatBroadcast);
        }
    }

    public static void enviarMensajeServidor(Room room, String contenido) {
        NetworkFrame serverMsg = com.zoomsockets.protocol.NetworkFrameFactory.createChatBroadcast(room.getIdSala(), 0, "SISTEMA", contenido);
        room.broadcast(serverMsg);
    }
}
