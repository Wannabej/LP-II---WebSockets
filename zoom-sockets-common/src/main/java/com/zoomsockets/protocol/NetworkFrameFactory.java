package com.zoomsockets.protocol;

import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import java.util.List;

/**
 * PATRÓN FACTORY METHOD (Fábrica):
 * Centraliza y encapsula la creación de objetos NetworkFrame. En lugar de que
 * cada
 * clase cliente o servidor instancie manualmente el ControlHeader y el
 * NetworkFrame
 * configurando los campos uno por uno, llaman a estos métodos de fábrica.
 * Esto reduce la duplicación de código, evita errores al olvidar un campo
 * y hace que la creación sea más semántica y fácil de mantener.
 */
public class NetworkFrameFactory {

    // =========================================================================
    // PETICIONES DEL CLIENTE AL SERVIDOR (REQUESTS)
    // =========================================================================

    public static NetworkFrame createLoginRequest(String email, String password) {
        ControlHeader header = new ControlHeader("LOGIN_REQUEST");
        header.setEmail(email);
        header.setPassword(password);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createRegisterRequest(String nombres, String email, String password, String rol) {
        ControlHeader header = new ControlHeader("REGISTER_REQUEST");
        header.setNombres(nombres);
        header.setEmail(email);
        header.setPassword(password);
        header.setRol(rol);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createRoomRequest(String nombreSala) {
        ControlHeader header = new ControlHeader("CREATE_ROOM");
        header.setNombreSala(nombreSala);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createJoinRoomRequest(String codigoSala) {
        ControlHeader header = new ControlHeader("JOIN_ROOM_REQUEST");
        header.setCodigoSala(codigoSala);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createAdmitUserRequest(int userId, String action) {
        ControlHeader header = new ControlHeader("ADMIT_USER");
        header.setIdUsuario(userId);
        header.setAction(action); // "ACCEPT" o "REJECT"
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createChatMessage(String contenido) {
        ControlHeader header = new ControlHeader("CHAT_MESSAGE");
        header.setContenido(contenido);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createFileStartFrame(int roomId, int userId, String fileName, long fileSize) {
        ControlHeader header = new ControlHeader("FILE_START");
        header.setIdSala(roomId);
        header.setIdUsuario(userId);
        header.setNombreArchivo(fileName);
        header.setContenido(String.valueOf(fileSize));
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createFileChunkFrame(byte[] chunkData) {
        ControlHeader header = new ControlHeader("FILE_CHUNK");
        return new NetworkFrame(header.toJson(), chunkData);
    }

    public static NetworkFrame createFileEndFrame() {
        ControlHeader header = new ControlHeader("FILE_END");
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createFileDownloadRequest(int idArchivo) {
        ControlHeader header = new ControlHeader("FILE_DOWNLOAD_REQUEST");
        header.setIdArchivo(idArchivo);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createCameraFrame(byte[] imageBytes) {
        ControlHeader header = new ControlHeader("CAMERA_FRAME");
        return new NetworkFrame(header.toJson(), imageBytes);
    }

    public static NetworkFrame createCameraFrameRelay(int idUsuario, String nombres, byte[] imageBytes) {
        ControlHeader header = new ControlHeader("CAMERA_FRAME");
        header.setIdUsuario(idUsuario);
        header.setNombres(nombres);
        return new NetworkFrame(header.toJson(), imageBytes);
    }

    public static NetworkFrame createLeaveRoomRequest() {
        ControlHeader header = new ControlHeader("LEAVE_ROOM");
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createChangeNameRequest(String nuevoNombre) {
        ControlHeader header = new ControlHeader("CHANGE_NAME_REQUEST");
        header.setNombres(nuevoNombre);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createRoomClosedNotification() {
        ControlHeader header = new ControlHeader("ROOM_CLOSED");
        return new NetworkFrame(header.toJson());
    }

    // =========================================================================
    // RESPUESTAS Y NOTIFICACIONES DEL SERVIDOR AL CLIENTE (RESPONSES)
    // =========================================================================

    public static NetworkFrame createLoginResponse(boolean success, String error, Usuario user) {
        ControlHeader header = new ControlHeader("LOGIN_RESPONSE");
        header.setSuccess(success);
        if (success && user != null) {
            header.setStatus("SUCCESS");
            header.setIdUsuario(user.getIdUsuario());
            header.setNombres(user.getNombres());
            header.setRol(user.getRol());
        } else {
            header.setStatus("ERROR");
            header.setError(error);
        }
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createRegisterResponse(boolean success, String error) {
        ControlHeader header = new ControlHeader("REGISTER_RESPONSE");
        header.setSuccess(success);
        if (success) {
            header.setStatus("SUCCESS");
        } else {
            header.setStatus("ERROR");
            header.setError(error);
        }
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createCreateRoomResponse(boolean success, String error, String codigoSala, int idSala,
            String nombreSala) {
        ControlHeader header = new ControlHeader("CREATE_ROOM_RESPONSE");
        header.setSuccess(success);
        if (success) {
            header.setStatus("SUCCESS");
            header.setCodigoSala(codigoSala);
            header.setIdSala(idSala);
            header.setNombreSala(nombreSala);
        } else {
            header.setStatus("ERROR");
            header.setError(error);
        }
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createJoinRoomResponse(String status, boolean success, String error, Integer idSala,
            String nombreSala) {
        ControlHeader header = new ControlHeader("JOIN_ROOM_RESPONSE");
        header.setStatus(status);
        header.setSuccess(success);
        header.setError(error);
        if (idSala != null)
            header.setIdSala(idSala);
        if (nombreSala != null)
            header.setNombreSala(nombreSala);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createWaitingRoomUpdate(List<SolicitudSala> pendingUsers) {
        ControlHeader header = new ControlHeader("WAITING_ROOM_UPDATE");
        header.setPendingUsers(pendingUsers);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createRoomMembersUpdate(List<Usuario> activeUsers) {
        ControlHeader header = new ControlHeader("ROOM_MEMBERS_UPDATE");
        header.setActiveUsers(activeUsers);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createChatBroadcast(int idSala, int idUsuario, String nombres, String contenido) {
        ControlHeader header = new ControlHeader("CHAT_MESSAGE");
        header.setIdSala(idSala);
        header.setIdUsuario(idUsuario);
        header.setNombres(nombres);
        header.setContenido(contenido);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createFileSharedNotification(int idSala, int idUsuario, String nombres, String fileName,
            String physicalName, int idArchivo) {
        ControlHeader header = new ControlHeader("FILE_SHARED");
        header.setIdSala(idSala);
        header.setIdUsuario(idUsuario);
        header.setNombres(nombres);
        header.setNombreArchivo(fileName);
        header.setContenido(physicalName);
        header.setIdArchivo(idArchivo);
        return new NetworkFrame(header.toJson());
    }

    public static NetworkFrame createFileDownloadResponse(int idArchivo, String nombreArchivo, byte[] fileData,
            String error) {
        ControlHeader header = new ControlHeader("FILE_DOWNLOAD_RESPONSE");
        header.setIdArchivo(idArchivo);
        if (error != null) {
            header.setError(error);
            return new NetworkFrame(header.toJson());
        }
        header.setNombreArchivo(nombreArchivo);
        return new NetworkFrame(header.toJson(), fileData);
    }
}
