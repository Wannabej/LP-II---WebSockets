package com.zoomsockets.model;

import java.sql.Timestamp;

public class Sala {
    private int idSala;
    private String codigoSala;
    private String nombre;
    private int idHost;
    private String estado; // 'Activa', 'Finalizada'
    private Timestamp fechaCreacion;

    public Sala() {}

    public Sala(int idSala, String codigoSala, String nombre, int idHost, String estado, Timestamp fechaCreacion) {
        this.idSala = idSala;
        this.codigoSala = codigoSala;
        this.nombre = nombre;
        this.idHost = idHost;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public int getIdSala() {
        return idSala;
    }

    public void setIdSala(int idSala) {
        this.idSala = idSala;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getIdHost() {
        return idHost;
    }

    public void setIdHost(int idHost) {
        this.idHost = idHost;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
