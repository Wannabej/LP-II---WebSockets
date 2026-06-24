package com.zoomsockets.model;

import java.sql.Timestamp;

public class ArchivoCompartido {
    private int idArchivo;
    private int idSala;
    private int idUsuario;
    private String nombreArchivo;
    private String rutaArchivo;
    private Timestamp fechaEnvio;
    
    // Campo auxiliar para mostrar el nombre del que envió el archivo
    private String nombreUsuario;

    public ArchivoCompartido() {}

    public ArchivoCompartido(int idArchivo, int idSala, int idUsuario, String nombreArchivo, String rutaArchivo, Timestamp fechaEnvio) {
        this.idArchivo = idArchivo;
        this.idSala = idSala;
        this.idUsuario = idUsuario;
        this.nombreArchivo = nombreArchivo;
        this.rutaArchivo = rutaArchivo;
        this.fechaEnvio = fechaEnvio;
    }

    public int getIdArchivo() {
        return idArchivo;
    }

    public void setIdArchivo(int idArchivo) {
        this.idArchivo = idArchivo;
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

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
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
