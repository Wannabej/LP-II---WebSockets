package com.zoomsockets.db;

import com.zoomsockets.model.Sala;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class SalaDAO {

    public boolean crearSala(Sala sala) {
        String sql = "INSERT INTO Salas (CodigoSala, Nombre, IdHost, Estado, FechaCreacion) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, sala.getCodigoSala());
            ps.setString(2, sala.getNombre());
            ps.setInt(3, sala.getIdHost());
            ps.setString(4, "Activa");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(5, now);
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        sala.setIdSala(generatedKeys.getInt(1));
                    }
                }
                sala.setEstado("Activa");
                sala.setFechaCreacion(now);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al crear sala: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public Sala findSalaByCodigo(String codigoSala) {
        String sql = "SELECT IdSala, CodigoSala, Nombre, IdHost, Estado, FechaCreacion FROM Salas WHERE CodigoSala = ? AND Estado = 'Activa'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, codigoSala);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Sala(
                        rs.getInt("IdSala"),
                        rs.getString("CodigoSala"),
                        rs.getString("Nombre"),
                        rs.getInt("IdHost"),
                        rs.getString("Estado"),
                        rs.getTimestamp("FechaCreacion")
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar sala por código: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean finalizarSala(int idSala) {
        String sql = "UPDATE Salas SET Estado = 'Finalizada' WHERE IdSala = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idSala);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al finalizar sala: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean agregarParticipante(int idSala, int idUsuario) {
        // Primero verificamos si ya existe el registro para evitar duplicados
        String checkSql = "SELECT IdParticipante FROM ParticipantesSala WHERE IdSala = ? AND IdUsuario = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            boolean exists = false;
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setInt(1, idSala);
                psCheck.setInt(2, idUsuario);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String updateSql = "UPDATE ParticipantesSala SET Estado = 'Activo', FechaIngreso = ? WHERE IdSala = ? AND IdUsuario = ?";
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                    psUpdate.setInt(2, idSala);
                    psUpdate.setInt(3, idUsuario);
                    return psUpdate.executeUpdate() > 0;
                }
            } else {
                String insertSql = "INSERT INTO ParticipantesSala (IdSala, IdUsuario, Estado, FechaIngreso) VALUES (?, ?, 'Activo', ?)";
                try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
                    psInsert.setInt(1, idSala);
                    psInsert.setInt(2, idUsuario);
                    psInsert.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    return psInsert.executeUpdate() > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error al agregar/activar participante: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean removerParticipante(int idSala, int idUsuario) {
        String sql = "UPDATE ParticipantesSala SET Estado = 'Inactivo' WHERE IdSala = ? AND IdUsuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idSala);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error al desactivar participante: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isParticipanteActivo(int idSala, int idUsuario) {
        // El Host de la sala siempre se considera participante activo de la misma
        String hostSql = "SELECT IdSala FROM Salas WHERE IdSala = ? AND IdHost = ? AND Estado = 'Activa'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(hostSql)) {
                ps.setInt(1, idSala);
                ps.setInt(2, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }

            // Si no es Host, verificar en ParticipantesSala
            String partSql = "SELECT IdParticipante FROM ParticipantesSala WHERE IdSala = ? AND IdUsuario = ? AND Estado = 'Activo'";
            try (PreparedStatement ps = conn.prepareStatement(partSql)) {
                ps.setInt(1, idSala);
                ps.setInt(2, idUsuario);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (Exception e) {
            System.err.println("Error al verificar participación activa: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
