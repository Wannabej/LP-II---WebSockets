package com.zoomsockets.server.command;

import com.zoomsockets.db.SalaDAO;
import com.zoomsockets.model.Sala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;
import com.zoomsockets.server.Room;
import com.zoomsockets.server.RoomManager;

import java.util.UUID;

public class CreateRoomHandler implements ServerCommandHandler {
    private final SalaDAO salaDAO = new SalaDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario usuario = client.getUsuario();
        if (usuario == null) return;

        // Generar código de sala único
        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Sala sala = new Sala();
        sala.setCodigoSala(codigo);
        sala.setNombre(header.getNombreSala());
        sala.setIdHost(usuario.getIdUsuario());

        NetworkFrame responseFrame;
        if (salaDAO.crearSala(sala)) {
            // Guardar host como participante activo en DB
            salaDAO.agregarParticipante(sala.getIdSala(), usuario.getIdUsuario());

            // Registrar en memoria
            Room room = RoomManager.crearRoom(sala.getIdSala(), codigo, sala.getNombre(), client);
            client.setRoomActivo(room);
            
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createCreateRoomResponse(true, null, codigo, sala.getIdSala(), sala.getNombre());
            System.out.println("Sala creada en BD y memoria: " + codigo + " por Host: " + usuario.getNombres());
        } else {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createCreateRoomResponse(false, "No se pudo persistir la sala en la base de datos.", null, 0, null);
        }
        client.sendFrame(responseFrame);
    }
}
