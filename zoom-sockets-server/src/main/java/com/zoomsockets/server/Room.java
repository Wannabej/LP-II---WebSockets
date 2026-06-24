package com.zoomsockets.server;

import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.NetworkFrame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
    private final int idSala;
    private final String codigoSala;
    private final String nombre;
    private final ClientHandler hostHandler;
    
    // Hilos de clientes conectados y admitidos activamente
    private final List<ClientHandler> participantes = Collections.synchronizedList(new ArrayList<>());
    
    // Hilos de clientes conectados en espera de admisión
    private final List<ClientHandler> salaDeEspera = Collections.synchronizedList(new ArrayList<>());

    public Room(int idSala, String codigoSala, String nombre, ClientHandler hostHandler) {
        this.idSala = idSala;
        this.codigoSala = codigoSala;
        this.nombre = nombre;
        this.hostHandler = hostHandler;
        // El host se auto-agrega como participante activo
        this.participantes.add(hostHandler);
    }

    public int getIdSala() {
        return idSala;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public String getNombre() {
        return nombre;
    }

    public ClientHandler getHostHandler() {
        return hostHandler;
    }

    public List<ClientHandler> getParticipantes() {
        return new ArrayList<>(participantes);
    }

    public List<ClientHandler> getSalaDeEspera() {
        return new ArrayList<>(salaDeEspera);
    }

    public void agregarASalaDeEspera(ClientHandler client) {
        if (!salaDeEspera.contains(client)) {
            salaDeEspera.add(client);
        }
    }

    public void removerDeSalaDeEspera(ClientHandler client) {
        salaDeEspera.remove(client);
    }

    public void admitirParticipante(ClientHandler client) {
        salaDeEspera.remove(client);
        if (!participantes.contains(client)) {
            participantes.add(client);
        }
    }

    public void removerParticipante(ClientHandler client) {
        participantes.remove(client);
        salaDeEspera.remove(client);
    }

    /**
     * Envía una trama de red a todos los participantes activos de la sala.
     */
    public void broadcast(NetworkFrame frame) {
        synchronized (participantes) {
            for (ClientHandler handler : participantes) {
                handler.sendFrame(frame);
            }
        }
    }

    /**
     * Envía una trama de red a todos los participantes ACTIVOS excepto al remitente.
     */
    public void broadcastExcept(NetworkFrame frame, ClientHandler sender) {
        synchronized (participantes) {
            for (ClientHandler handler : participantes) {
                if (handler != sender) {
                    handler.sendFrame(frame);
                }
            }
        }
    }

    /**
     * Retorna una lista con la información básica de los usuarios activos.
     */
    public List<Usuario> getActiveUsersList() {
        List<Usuario> list = new ArrayList<>();
        synchronized (participantes) {
            for (ClientHandler h : participantes) {
                if (h.getUsuario() != null) {
                    list.add(h.getUsuario());
                }
            }
        }
        return list;
    }
}
