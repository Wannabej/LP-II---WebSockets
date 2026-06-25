package com.zoomsockets.client;

import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import java.util.List;

public interface ClientListener {
    void onLoginResponse(boolean success, String error, String name, String role, int idUsuario);
    
    void onRegisterResponse(boolean success, String error);
    
    void onCreateRoomResponse(boolean success, String error, String codigoSala, String nombreSala, int idSala);
    
    void onJoinRoomResponse(String status, String error, int idSala, String nombreSala);
    
    void onWaitingRoomUpdate(List<SolicitudSala> pendingUsers);
    
    void onRoomMembersUpdate(List<Usuario> activeUsers);
    
    void onChatMessage(String senderName, String content, int senderId);
    
    void onFileShared(String senderName, String filename, String physicalName);
    
    void onCameraFrame(int userId, String userName, byte[] imageBytes);
    
    void onRoomTerminated();
}
