package com.zoomsockets.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final Map<String, Room> activeRooms = new ConcurrentHashMap<>();

    public static Room crearRoom(int idSala, String codigoSala, String nombre, ClientHandler hostHandler) {
        Room room = new Room(idSala, codigoSala, nombre, hostHandler);
        activeRooms.put(codigoSala.toUpperCase(), room);
        System.out.println("Sala activa registrada en memoria: [" + codigoSala.toUpperCase() + "] - " + nombre);
        return room;
    }

    public static Room getRoom(String codigoSala) {
        if (codigoSala == null) return null;
        return activeRooms.get(codigoSala.toUpperCase());
    }

    public static void removerRoom(String codigoSala) {
        if (codigoSala != null) {
            Room removed = activeRooms.remove(codigoSala.toUpperCase());
            if (removed != null) {
                System.out.println("Sala removida de memoria: [" + codigoSala.toUpperCase() + "]");
            }
        }
    }

    /**
     * Cierra la sala por completo, notificando a todos los clientes y actualizando la BD.
     */
    public static void cerrarSala(String codigoSala) {
        if (codigoSala != null) {
            Room room = activeRooms.remove(codigoSala.toUpperCase());
            if (room != null) {
                // Notificar a todos los clientes en la sala
                com.zoomsockets.protocol.ControlHeader closeHeader = new com.zoomsockets.protocol.ControlHeader("ROOM_CLOSED");
                com.zoomsockets.protocol.NetworkFrame closeFrame = new com.zoomsockets.protocol.NetworkFrame(closeHeader.toJson());
                room.broadcast(closeFrame);

                // Finalizar la sala en la base de datos
                com.zoomsockets.db.SalaDAO salaDAO = new com.zoomsockets.db.SalaDAO();
                salaDAO.finalizarSala(room.getIdSala());

                System.out.println("Sala cerrada (Host desconectado) y notificada: [" + codigoSala.toUpperCase() + "]");
            }
        }
    }

    /**
     * Limpia la presencia de un cliente de todas las salas activas.
     * Retorna la sala de la cual fue removido (si aplica).
     */
    public static Room limpiarPresenciaCliente(ClientHandler handler) {
        Room affectedRoom = null;
        for (Room room : activeRooms.values()) {
            if (room.getParticipantes().contains(handler)) {
                room.removerParticipante(handler);
                affectedRoom = room;
                System.out.println("Usuario " + handler.getUsuario().getNombres() + " removido de participantes activos en la sala " + room.getCodigoSala());
            }
            if (room.getSalaDeEspera().contains(handler)) {
                room.removerDeSalaDeEspera(handler);
                affectedRoom = room;
                System.out.println("Usuario " + handler.getUsuario().getNombres() + " removido de sala de espera en la sala " + room.getCodigoSala());
            }
        }
        return affectedRoom;
    }
}
