package com.zoomsockets.server.command;

import com.zoomsockets.db.UsuarioDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class RegisterRequestHandler implements ServerCommandHandler {
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void execute(ControlHeader header, NetworkFrame frame, ClientHandler client) {
        NetworkFrame responseFrame;
        
        // Verificar si el correo ya existe
        Usuario existente = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        if (existente != null) {
            responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(false, "El correo ya está registrado.");
        } else {
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombres(header.getNombres());
            nuevoUsuario.setCorreo(header.getEmail());
            // Asegurarse de que el rol sea 'Docente' o 'Estudiante'
            String rol = header.getRol();
            if (rol == null || (!rol.equals("Docente") && !rol.equals("Estudiante"))) {
                rol = "Estudiante"; // Default
            }
            nuevoUsuario.setRol(rol);
            nuevoUsuario.setActivo(true);
            
            // registrarUsuario ya se encarga de hashear con BCrypt
            if (usuarioDAO.registrarUsuario(nuevoUsuario, header.getPassword())) {
                responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(true, null);
                System.out.println("Nuevo usuario registrado exitosamente: " + nuevoUsuario.getNombres() + " [" + nuevoUsuario.getRol() + "]");
            } else {
                responseFrame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(false, "Error interno al persistir el usuario.");
            }
        }
        client.sendFrame(responseFrame);
    }
}
