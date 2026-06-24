package com.zoomsockets.model;

import java.sql.Timestamp;

public class Mensaje {
    private int idMensaje;
    private int idSala;
    private int idUsuario;
    private String contenido;
    private Timestamp fechaEnvio;
    
    // Campo auxiliar para mostrar el nombre del remitente en el cliente
    private String nombreUsuario;

    public Mensaje() {}

    public Mensaje(int idMensaje, int idSala, int idUsuario, String contenido, Timestamp fechaEnvio) {
        this.idMensaje = idMensaje;
        this.idSala = idSala;
        this.idUsuario = idUsuario;
        this.contenido = contenido;
        this.fechaEnvio = fechaEnvio;
    }

    public int getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(int idMensaje) {
        this.idMensaje = idMensaje;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Timestamp getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(Timestamp fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}
