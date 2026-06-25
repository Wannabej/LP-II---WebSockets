package com.zoomsockets.db;

import com.zoomsockets.model.ArchivoCompartido;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ArchivoCompartidoDAO {

    public boolean registrarArchivo(ArchivoCompartido archivo) {
        String sql = "INSERT INTO ArchivosCompartidos (IdSala, IdUsuario, NombreArchivo, RutaArchivo, FechaEnvio) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, archivo.getIdSala());
            ps.setInt(2, archivo.getIdUsuario());
            ps.setString(3, archivo.getNombreArchivo());
            ps.setString(4, archivo.getRutaArchivo());
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(5, now);
            
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        archivo.setIdArchivo(generatedKeys.getInt(1));
                    }
                }
                archivo.setFechaEnvio(now);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error al registrar archivo compartido: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public ArchivoCompartido getArchivoPorId(int idArchivo) {
        String sql = "SELECT * FROM ArchivosCompartidos WHERE IdArchivo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idArchivo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ArchivoCompartido(
                        rs.getInt("IdArchivo"),
                        rs.getInt("IdSala"),
                        rs.getInt("IdUsuario"),
                        rs.getString("NombreArchivo"),
                        rs.getString("RutaArchivo"),
                        rs.getTimestamp("FechaEnvio")
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener archivo por ID: " + e.getMessage());
        }
        return null;
    }

    public List<ArchivoCompartido> getArchivosPorSala(int idSala) {
        List<ArchivoCompartido> lista = new ArrayList<>();
        String sql = "SELECT a.IdArchivo, a.IdSala, a.IdUsuario, a.NombreArchivo, a.RutaArchivo, a.FechaEnvio, u.Nombres " +
                     "FROM ArchivosCompartidos a " +
                     "JOIN Usuarios u ON a.IdUsuario = u.IdUsuario " +
                     "WHERE a.IdSala = ? " +
                     "ORDER BY a.FechaEnvio ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, idSala);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ArchivoCompartido a = new ArchivoCompartido(
                        rs.getInt("IdArchivo"),
                        rs.getInt("IdSala"),
                        rs.getInt("IdUsuario"),
                        rs.getString("NombreArchivo"),
                        rs.getString("RutaArchivo"),
                        rs.getTimestamp("FechaEnvio")
                    );
                    a.setNombreUsuario(rs.getString("Nombres"));
                    lista.add(a);
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener archivos por sala: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }
}
