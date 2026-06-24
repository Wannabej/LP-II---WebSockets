package com.zoomsockets.db;

import com.zoomsockets.model.Mensaje;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MensajeDAO {

    public boolean registrarMensaje(Mensaje mensaje) {
        String sql = "INSERT INTO Mensajes (IdSala, IdUsuario, Contenido, FechaEnvio) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, mensaje.getIdSala());
            ps.setInt(2, mensaje.getIdUsuario());
            ps.setString(3, mensaje.getContenido());
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(4, now);
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        mensaje.setIdMensaje(generatedKeys.getInt(1));
                    }
                }
                mensaje.setFechaEnvio(now);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al registrar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Mensaje> getMensajesPorSala(int idSala) {
        List<Mensaje> lista = new ArrayList<>();
        String sql = "SELECT m.IdMensaje, m.IdSala, m.IdUsuario, m.Contenido, m.FechaEnvio, u.Nombres " +
                     "FROM Mensajes m " +
                     "JOIN Usuarios u ON m.IdUsuario = u.IdUsuario " +
                     "WHERE m.IdSala = ? " +
                     "ORDER BY m.FechaEnvio ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mensaje m = new Mensaje(
                        rs.getInt("IdMensaje"),
                        rs.getInt("IdSala"),
                        rs.getInt("IdUsuario"),
                        rs.getString("Contenido"),
                        rs.getTimestamp("FechaEnvio")
                    );
                    m.setNombreUsuario(rs.getString("Nombres"));
                    lista.add(m);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener mensajes por sala: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}
