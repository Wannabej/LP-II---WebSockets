package com.zoomsockets.db;

import com.zoomsockets.model.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuarioDAO {

    public Usuario findUsuarioByCorreo(String correo) {
        String sql = "SELECT IdUsuario, Nombres, Correo, PasswordHash, Rol, Activo FROM Usuarios WHERE Correo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                        rs.getInt("IdUsuario"),
                        rs.getString("Nombres"),
                        rs.getString("Correo"),
                        rs.getString("PasswordHash"),
                        rs.getString("Rol"),
                        rs.getBoolean("Activo")
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar usuario por correo: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean registrarUsuario(Usuario usuario, String passwordPlana) {
        String sql = "INSERT INTO Usuarios (Nombres, Correo, PasswordHash, Rol, Activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            String hashed = BCrypt.hashpw(passwordPlana, BCrypt.gensalt());
            ps.setString(1, usuario.getNombres());
            ps.setString(2, usuario.getCorreo());
            ps.setString(3, hashed);
            ps.setString(4, usuario.getRol());
            ps.setBoolean(5, usuario.isActivo());
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        usuario.setIdUsuario(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean verificarPassword(String passwordPlana, String passwordHash) {
        try {
            return BCrypt.checkpw(passwordPlana, passwordHash);
        } catch (Exception e) {
            System.err.println("Error al verificar contraseña: " + e.getMessage());
            return false;
        }
    }
}
