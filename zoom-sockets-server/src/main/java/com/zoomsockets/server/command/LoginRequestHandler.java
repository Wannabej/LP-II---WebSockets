package com.zoomsockets.server.command;

import com.zoomsockets.db.UsuarioDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class LoginRequestHandler implements ServerCommandHandler {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        Usuario user = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        NetworkFrame responseFrame;
        
        if (user != null && usuarioDAO.verificarPassword(header.getPassword(), user.getPasswordHash())) {
            if (user.isActivo()) {
                client.setUsuario(user);
                responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(true, null, user);
                System.out.println("Autenticación exitosa: " + user.getNombres() + " [" + user.getRol() + "]");
            } else {
                responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(false, "El usuario está inactivo.", null);
            }
        } else {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(false, "Credenciales incorrectas.", null);
        }
        client.sendFrame(responseFrame);
    }
}
