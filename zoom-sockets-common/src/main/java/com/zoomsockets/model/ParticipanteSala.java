package com.zoomsockets.model;

import java.sql.Timestamp;

public class ParticipanteSala {
    private int idParticipante;
    private int idSala;
    private int idUsuario;
    private String estado; // 'Activo', 'Inactivo'
    private Timestamp fechaIngreso;

    public ParticipanteSala() {}

    public ParticipanteSala(int idParticipante, int idSala, int idUsuario, String estado, Timestamp fechaIngreso) {
        this.idParticipante = idParticipante;
        this.idSala = idSala;
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.fechaIngreso = fechaIngreso;
    }

    public int getIdParticipante() {
        return idParticipante;
    }

    public void setIdParticipante(int idParticipante) {
        this.idParticipante = idParticipante;
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

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}
