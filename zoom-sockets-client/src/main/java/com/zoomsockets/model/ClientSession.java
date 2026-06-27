package com.zoomsockets.model;

public class ClientSession {
    private static ClientSession instance;

    private int myUserId;
    private String myName;
    private String myRole;
    private int activeRoomId;
    private String activeRoomCode;
    private String activeRoomName;

    private ClientSession() {
        // Constructor privado para Singleton
    }

    /**
     * PATRÓN SINGLETON:
     * Este método estático garantiza que exista una única instancia global de ClientSession
     * durante todo el ciclo de vida de la aplicación.
     * Si la variable estática 'instance' es nula, se crea por primera y única vez.
     * De lo contrario, se devuelve la instancia existente.
     * Se usa "synchronized" para prevenir problemas si varios hilos intentan obtenerla al mismo tiempo.
     */
    public static synchronized ClientSession getInstance() {
        if (instance == null) {
            instance = new ClientSession();
        }
        return instance;
    }

    public void limpiarSesion() {
        this.myUserId = 0;
        this.myName = null;
        this.myRole = null;
        clearRoom();
    }

    public int getMyUserId() { return myUserId; }
    public void setMyUserId(int myUserId) { this.myUserId = myUserId; }

    public String getMyName() { return myName; }
    public void setMyName(String myName) { this.myName = myName; }

    public String getMyRole() { return myRole; }
    public void setMyRole(String myRole) { this.myRole = myRole; }

    public int getActiveRoomId() { return activeRoomId; }
    public void setActiveRoomId(int activeRoomId) { this.activeRoomId = activeRoomId; }

    public String getActiveRoomCode() { return activeRoomCode; }
    public void setActiveRoomCode(String activeRoomCode) { this.activeRoomCode = activeRoomCode; }

    public String getActiveRoomName() { return activeRoomName; }
    public void setActiveRoomName(String activeRoomName) { this.activeRoomName = activeRoomName; }

    public void clearRoom() {
        this.activeRoomId = 0;
        this.activeRoomCode = null;
        this.activeRoomName = null;
    }
}
