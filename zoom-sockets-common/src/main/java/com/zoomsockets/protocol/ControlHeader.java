package com.zoomsockets.protocol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import java.util.List;

public class ControlHeader {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    // Campos Generales
    private String type;
    private String status;
    private String error;
    private Boolean success;

    // Campos de Autenticación
    private String email;
    private String password;
    private Integer idUsuario;
    private String nombres;
    private String rol;

    // Campos de Sala
    private Integer idSala;
    private String codigoSala;
    private String nombreSala;
    private String action; // "ACCEPT" o "REJECT"

    // Campos de Chat
    private String contenido;

    // Campos de Archivos
    private String nombreArchivo;
    private Integer idArchivo;
    private Long tamanoArchivo;
    private Integer chunkIndex;
    private Integer chunkCount;

    // Listas para actualizaciones en tiempo real
    private List<SolicitudSala> pendingUsers;
    private List<Usuario> activeUsers;

    public ControlHeader() {
    }

    public ControlHeader(String type) {
        this.type = type;
    }

    // --- Métodos de Serialización ---
    public String toJson() {
        return GSON.toJson(this);
    }

    public static ControlHeader fromJson(String json) {
        return GSON.fromJson(json, ControlHeader.class);
    }

    // --- Getters y Setters ---
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Integer getIdSala() {
        return idSala;
    }

    public void setIdSala(Integer idSala) {
        this.idSala = idSala;
    }

    public String getCodigoSala() {
        return codigoSala;
    }

    public void setCodigoSala(String codigoSala) {
        this.codigoSala = codigoSala;
    }

    public String getNombreSala() {
        return nombreSala;
    }

    public void setNombreSala(String nombreSala) {
        this.nombreSala = nombreSala;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public Integer getIdArchivo() {
        return idArchivo;
    }

    public void setIdArchivo(Integer idArchivo) {
        this.idArchivo = idArchivo;
    }

    public Long getTamanoArchivo() {
        return tamanoArchivo;
    }

    public void setTamanoArchivo(Long tamanoArchivo) {
        this.tamanoArchivo = tamanoArchivo;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public List<SolicitudSala> getPendingUsers() {
        return pendingUsers;
    }

    public void setPendingUsers(List<SolicitudSala> pendingUsers) {
        this.pendingUsers = pendingUsers;
    }

    public List<Usuario> getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(List<Usuario> activeUsers) {
        this.activeUsers = activeUsers;
    }
}
