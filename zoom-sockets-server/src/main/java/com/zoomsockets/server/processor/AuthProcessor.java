package com.zoomsockets.server.processor;

import com.zoomsockets.db.UsuarioDAO;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.server.ClientHandler;

public class AuthProcessor {
    private final ClientHandler client;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public AuthProcessor(ClientHandler client) {
        this.client = client;
    }

    public void handleLogin(ControlHeader header) {
        Usuario user = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        NetworkFrame frame;
        
        if (user != null && usuarioDAO.verificarPassword(header.getPassword(), user.getPasswordHash())) {
            if (user.isActivo()) {
                client.setUsuario(user);
                frame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(true, null, user);
                System.out.println("Autenticación exitosa: " + user.getNombres() + " [" + user.getRol() + "]");
            } else {
                frame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(false, "El usuario está inactivo.", null);
            }
        } else {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createLoginResponse(false, "Credenciales incorrectas.", null);
        }
        client.sendFrame(frame);
    }

    public void handleRegister(ControlHeader header) {
        NetworkFrame frame;
        
        // Verificar si el correo ya existe
        Usuario existente = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        if (existente != null) {
            frame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(false, "El correo ya está registrado.");
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
                frame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(true, null);
                System.out.println("Nuevo usuario registrado exitosamente: " + nuevoUsuario.getNombres() + " [" + nuevoUsuario.getRol() + "]");
            } else {
                frame = com.zoomsockets.protocol.NetworkFrameFactory.createRegisterResponse(false, "Error interno al persistir el usuario.");
            }
        }
        client.sendFrame(frame);
    }
}
