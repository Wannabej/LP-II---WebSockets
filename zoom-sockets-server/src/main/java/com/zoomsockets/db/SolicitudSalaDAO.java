package com.zoomsockets.db;

import com.zoomsockets.model.SolicitudSala;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class SolicitudSalaDAO {

    public boolean registrarSolicitud(SolicitudSala solicitud) {
        String sql = "INSERT INTO SolicitudesSala (IdSala, IdUsuario, Estado, FechaSolicitud) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, solicitud.getIdSala());
            ps.setInt(2, solicitud.getIdUsuario());
            ps.setString(3, "Pendiente");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(4, now);
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        solicitud.setIdSolicitud(generatedKeys.getInt(1));
                    }
                }
                solicitud.setEstado("Pendiente");
                solicitud.setFechaSolicitud(now);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al registrar solicitud: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<SolicitudSala> getPendientesPorSala(int idSala) {
        List<SolicitudSala> lista = new ArrayList<>();
        String sql = "SELECT s.IdSolicitud, s.IdSala, s.IdUsuario, s.Estado, s.FechaSolicitud, u.Nombres " +
                     "FROM SolicitudesSala s " +
                     "JOIN Usuarios u ON s.IdUsuario = u.IdUsuario " +
                     "WHERE s.IdSala = ? AND s.Estado = 'Pendiente' " +
                     "ORDER BY s.FechaSolicitud ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SolicitudSala s = new SolicitudSala(
                        rs.getInt("IdSolicitud"),
                        rs.getInt("IdSala"),
                        rs.getInt("IdUsuario"),
                        rs.getString("Estado"),
                        rs.getTimestamp("FechaSolicitud")
                    );
                    s.setNombreUsuario(rs.getString("Nombres"));
                    lista.add(s);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener solicitudes pendientes: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    public boolean actualizarEstadoSolicitud(int idSolicitud, String estado) {
        String sql = "UPDATE SolicitudesSala SET Estado = ? WHERE IdSolicitud = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, estado);
            ps.setInt(2, idSolicitud);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al actualizar estado de solicitud: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean actualizarSolicitudPorUsuarioYSala(int idSala, int idUsuario, String estado) {
        String sql = "UPDATE SolicitudesSala SET Estado = ? WHERE IdSala = ? AND IdUsuario = ? AND Estado = 'Pendiente'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, estado);
            ps.setInt(2, idSala);
            ps.setInt(3, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al actualizar solicitud por usuario y sala: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
