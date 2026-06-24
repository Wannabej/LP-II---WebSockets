package com.zoomsockets.model;

public class Usuario {
    private int idUsuario;
    private String nombres;
    private String correo;
    private String passwordHash; // Opcional en el cliente por seguridad
    private String rol;
    private boolean activo;

    public Usuario() {}

    public Usuario(int idUsuario, String nombres, String correo, String passwordHash, String rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombres = nombres;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombres + " (" + rol + ")";
    }
}
