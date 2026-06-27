package com.zoomsockets.client;

import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import java.util.List;

/**
 * PATRÓN OBSERVER: 
 * Esta interfaz actúa como el "Observer" (Observador) dentro del patrón de diseño Observer.
 * Define el contrato o los métodos que deben ser implementados por aquellos componentes 
 * (como el MainController, que sería el "Concrete Observer") que desean ser notificados 
 * sobre eventos de red asíncronos que recibe el "Subject" (en este caso, ClientService).
 * 
 * Cuando ClientService (Subject) recibe un evento del servidor, itera/llama a la 
 * implementación de ClientListener registrada, notificándole los cambios de estado.
 */
public interface ClientListener {
    void onLoginResponse(boolean success, String error, String name, String role, int idUsuario);
    
    void onRegisterResponse(boolean success, String error);
    
    void onCreateRoomResponse(boolean success, String error, String codigoSala, String nombreSala, int idSala);
    
    void onJoinRoomResponse(String status, String error, int idSala, String nombreSala);
    
    void onWaitingRoomUpdate(List<SolicitudSala> pendingUsers);
    
    void onRoomMembersUpdate(List<Usuario> activeUsers);
    
    void onChatMessage(String senderName, String content, int senderId);
    
    void onFileShared(String senderName, String filename, String physicalName, int idArchivo);
    
    void onFileDownloadComplete(String destPath);
    
    void onFileDownloadFailed(String error);
    
    void onCameraFrame(int userId, String userName, byte[] imageBytes);
    
    void onRoomTerminated();
    
    void onRoomClosed(String mensaje);
}
