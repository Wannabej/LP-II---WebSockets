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
        ControlHeader response = new ControlHeader("LOGIN_RESPONSE");
        Usuario user = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        
        if (user != null && usuarioDAO.verificarPassword(header.getPassword(), user.getPasswordHash())) {
            if (user.isActivo()) {
                client.setUsuario(user);
                response.setStatus("SUCCESS");
                response.setSuccess(true);
                response.setIdUsuario(user.getIdUsuario());
                response.setNombres(user.getNombres());
                response.setRol(user.getRol());
                System.out.println("Autenticación exitosa: " + user.getNombres() + " [" + user.getRol() + "]");
            } else {
                response.setStatus("ERROR");
                response.setSuccess(false);
                response.setError("El usuario está inactivo.");
            }
        } else {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("Credenciales incorrectas.");
        }
        client.sendFrame(new NetworkFrame(response.toJson()));
    }

    public void handleRegister(ControlHeader header) {
        ControlHeader response = new ControlHeader("REGISTER_RESPONSE");
        
        // Verificar si el correo ya existe
        Usuario existente = usuarioDAO.findUsuarioByCorreo(header.getEmail());
        if (existente != null) {
            response.setStatus("ERROR");
            response.setSuccess(false);
            response.setError("El correo ya está registrado.");
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
                response.setStatus("SUCCESS");
                response.setSuccess(true);
                System.out.println("Nuevo usuario registrado exitosamente: " + nuevoUsuario.getNombres() + " [" + nuevoUsuario.getRol() + "]");
            } else {
                response.setStatus("ERROR");
                response.setSuccess(false);
                response.setError("Error interno al persistir el usuario.");
            }
        }
        client.sendFrame(new NetworkFrame(response.toJson()));
    }
}
