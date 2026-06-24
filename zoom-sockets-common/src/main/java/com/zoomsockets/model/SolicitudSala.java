package com.zoomsockets.model;

import java.sql.Timestamp;

public class SolicitudSala {
    private int idSolicitud;
    private int idSala;
    private int idUsuario;
    private String estado; // 'Pendiente', 'Aceptada', 'Rechazada'
    private Timestamp fechaSolicitud;
    
    // Campo auxiliar para mostrar el nombre del solicitante en la UI del Host
    private String nombreUsuario;

    public SolicitudSala() {}

    public SolicitudSala(int idSolicitud, int idSala, int idUsuario, String estado, Timestamp fechaSolicitud) {
        this.idSolicitud = idSolicitud;
        this.idSala = idSala;
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.fechaSolicitud = fechaSolicitud;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(int idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public int getIdSala() {
        return idSala;
    }

    public void setIdSala(int idSala) {
        this.idSala = idSala;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(Timestamp fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}
