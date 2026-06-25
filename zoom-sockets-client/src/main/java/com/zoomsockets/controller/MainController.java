package com.zoomsockets.controller;

import com.zoomsockets.client.ClientListener;
import com.zoomsockets.client.ClientService;
import com.zoomsockets.model.ClientSession;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.zoomsockets.view.ClientAppFrame;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController implements ClientListener {

    private ClientAppFrame frame;
    private ClientSession session;

    public MainController() {
        this.session = new ClientSession();
        ClientService.getInstance().setListener(this);
    }

    public void setFrame(ClientAppFrame frame) {
        this.frame = frame;
    }

    public ClientSession getSession() {
        return session;
    }

    public ClientAppFrame getFrame() {
        return frame;
    }

    // ==========================================
    // ACTIONS FROM VIEWS
    // ==========================================

    public void performLogin(String ip, int port, String correo, String password) {
        try {
            ClientService.getInstance().connect(ip, port);

            ControlHeader loginReq = new ControlHeader("LOGIN_REQUEST");
            loginReq.setEmail(correo);
            loginReq.setPassword(password);

            ClientService.getInstance().sendFrame(new NetworkFrame(loginReq.toJson()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "No se pudo conectar al servidor de sockets en " + ip + ":" + port + "\nDetalle: " + e.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            frame.getLoginPanel().enableLoginButton();
        }
    }

    public void performRegister(String ip, int port, String nombres, String correo, String password, String rol) {
        try {
            ClientService.getInstance().connect(ip, port);

            ControlHeader registerReq = new ControlHeader("REGISTER_REQUEST");
            registerReq.setNombres(nombres);
            registerReq.setEmail(correo);
            registerReq.setPassword(password);
            registerReq.setRol(rol);

            ClientService.getInstance().sendFrame(new NetworkFrame(registerReq.toJson()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "No se pudo conectar al servidor de sockets en " + ip + ":" + port + "\nDetalle: " + e.getMessage(),
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            if (frame.getRegisterPanel() != null) {
                frame.getRegisterPanel().enableRegisterButton();
            }
        }
    }

    public void createRoom(String nombre) {
        if (nombre == null || nombre.isEmpty())
            return;

        ControlHeader createReq = new ControlHeader("CREATE_ROOM");
        createReq.setNombreSala(nombre);
        ClientService.getInstance().sendFrame(new NetworkFrame(createReq.toJson()));
    }

    public void joinRoom(String codigo) {
        if (codigo == null || codigo.isEmpty())
            return;

        ControlHeader joinReq = new ControlHeader("JOIN_ROOM_REQUEST");
        joinReq.setCodigoSala(codigo);
        ClientService.getInstance().sendFrame(new NetworkFrame(joinReq.toJson()));
    }

    public void changeName(String newName) {
        if (newName == null || newName.trim().isEmpty())
            return;

        ControlHeader changeReq = new ControlHeader("CHANGE_NAME_REQUEST");
        changeReq.setNombres(newName.trim());

        ClientService.getInstance().sendFrame(new NetworkFrame(changeReq.toJson()));
    }

    public void logout() {
        ClientService.getInstance().disconnect();
        session.clearSession();
        frame.showCard("LOGIN");
        frame.getLoginPanel().enableLoginButton();
    }

    public void leaveRoom() {
        if (session.getActiveRoomId() > 0) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Estás seguro de que deseas salir de la sala de reunión?",
                    "Confirmar salida", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                ControlHeader leaveHeader = new ControlHeader("LEAVE_ROOM");
                ClientService.getInstance().sendFrame(new NetworkFrame(leaveHeader.toJson()));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }

                // Do not disconnect entirely, just go back to Welcome
                session.clearRoom();
                frame.getRoomPanel().stopCameraLocal();
                frame.showCard("WELCOME");
            }
        }
    }

    public void exitApp() {
        if (session.getActiveRoomId() > 0) {
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "¿Estás seguro de que deseas salir de la sala de reunión y cerrar la aplicación?",
                    "Confirmar salida", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                ControlHeader leaveHeader = new ControlHeader("LEAVE_ROOM");
                ClientService.getInstance().sendFrame(new NetworkFrame(leaveHeader.toJson()));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
                ClientService.getInstance().disconnect();
                System.exit(0);
            }
        } else {
            ClientService.getInstance().disconnect();
            System.exit(0);
        }
    }

    public void cancelWaitingRoom() {
        ControlHeader leaveHeader = new ControlHeader("LEAVE_ROOM");
        ClientService.getInstance().sendFrame(new NetworkFrame(leaveHeader.toJson()));
        frame.getWelcomePanel().enableJoinButton();
    }

    public void sendChatMessage(String msg) {
        if (msg == null || msg.isEmpty())
            return;

        ControlHeader chat = new ControlHeader("CHAT_MESSAGE");
        chat.setContenido(msg);

        ClientService.getInstance().sendFrame(new NetworkFrame(chat.toJson()));
    }

    public void shareFile(File selectedFile) {
        if (selectedFile.length() > 15 * 1024 * 1024) {
            JOptionPane.showMessageDialog(frame, "El archivo excede el límite permitido de 15MB.", "Archivo pesado",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ClientService.getInstance().sendFile(session.getActiveRoomId(), session.getMyUserId(), selectedFile);
    }

    public void respondWaitingRequest(int userId, String action) {
        ControlHeader admit = new ControlHeader("ADMIT_USER");
        admit.setIdUsuario(userId);
        admit.setAction(action);
        ClientService.getInstance().sendFrame(new NetworkFrame(admit.toJson()));
    }

    public void sendCameraFrame(byte[] jpegBytes) {
        ControlHeader frameHeader = new ControlHeader("CAMERA_FRAME");
        ClientService.getInstance().sendFrame(new NetworkFrame(frameHeader.toJson(), jpegBytes));
    }

    public void notifyCameraOff() {
        ControlHeader camHeader = new ControlHeader("CAMERA_FRAME");
        ClientService.getInstance().sendFrame(new NetworkFrame(camHeader.toJson(), new byte[0]));
    }

    // ==========================================
    // IMPLEMENTATION OF CLIENTLISTENER
    // ==========================================

    @Override
    public void onLoginResponse(boolean success, String error, String name, String role, int idUsuario) {
        if (success) {
            session.setMyUserId(idUsuario);
            session.setMyName(name);
            session.setMyRole(role);

            frame.getWelcomePanel().setWelcomeMessage(name, role);
            frame.showCard("WELCOME");
        } else {
            JOptionPane.showMessageDialog(frame, "Error de autenticación: " + error, "Login fallido",
                    JOptionPane.ERROR_MESSAGE);
            ClientService.getInstance().disconnect();
            frame.getLoginPanel().enableLoginButton();
        }
    }

    @Override
    public void onRegisterResponse(boolean success, String error) {
        // Al terminar el registro (sea éxito o error), debemos cerrar la conexión
        // socket temporal
        // para asegurar un estado limpio en el siguiente login.
        ClientService.getInstance().disconnect();

        if (success) {
            JOptionPane.showMessageDialog(frame, "Cuenta creada exitosamente. Ahora puedes iniciar sesión.",
                    "Registro Exitoso", JOptionPane.INFORMATION_MESSAGE);
            frame.showCard("LOGIN");
            if (frame.getRegisterPanel() != null) {
                frame.getRegisterPanel().enableRegisterButton();
                frame.getRegisterPanel().clearForm();
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Error en el registro: " + error, "Registro Fallido",
                    JOptionPane.ERROR_MESSAGE);
            if (frame.getRegisterPanel() != null) {
                frame.getRegisterPanel().enableRegisterButton();
            }
        }
    }

    @Override
    public void onCreateRoomResponse(boolean success, String error, String codigoSala, String nombreSala, int idSala) {
        if (success) {
            session.setActiveRoomId(idSala);
            session.setActiveRoomCode(codigoSala);
            session.setActiveRoomName(nombreSala);

            frame.getRoomPanel().setupHostRoom(codigoSala, nombreSala, session);
            frame.showCard("ROOM");
        } else {
            JOptionPane.showMessageDialog(frame, "No se pudo crear la sala:\n" + error, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onJoinRoomResponse(String status, String error, int idSala, String nombreSala) {
        if ("PENDING".equalsIgnoreCase(status)) {
            frame.getWelcomePanel().showWaitingRoomDialog(nombreSala, this);
        } else if ("SUCCESS".equalsIgnoreCase(status)) {
            frame.getWelcomePanel().closeWaitingRoomDialog();
            frame.getWelcomePanel().enableJoinButton();

            session.setActiveRoomId(idSala);
            // El codigo ya fue enviado por el input
            session.setActiveRoomName(nombreSala);

            frame.getRoomPanel().setupParticipantRoom(nombreSala, session);
            frame.showCard("ROOM");
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            frame.getWelcomePanel().closeWaitingRoomDialog();
            frame.getWelcomePanel().enableJoinButton();
            JOptionPane.showMessageDialog(frame, "Solicitud rechazada: " + error, "Acceso Denegado",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            frame.getWelcomePanel().closeWaitingRoomDialog();
            frame.getWelcomePanel().enableJoinButton();
            JOptionPane.showMessageDialog(frame, "No se pudo unir a la sala: " + error, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onWaitingRoomUpdate(List<SolicitudSala> pendingUsers) {
        frame.getRoomPanel().updateWaitingList(pendingUsers);
    }

    @Override
    public void onRoomMembersUpdate(List<Usuario> activeUsers) {
        frame.getRoomPanel().updateRoomMembers(activeUsers, session.getMyUserId());
    }

    @Override
    public void onChatMessage(String senderName, String content, int senderId) {
        frame.getRoomPanel().addChatMessage(senderName, content);
    }

    @Override
    public void onFileShared(String senderName, String filename, String physicalName, int idArchivo) {
        frame.getRoomPanel().addSharedFile(senderName, filename, idArchivo);
    }

    @Override
    public void onFileDownloadComplete(String destPath) {
        JOptionPane.showMessageDialog(frame, "Archivo guardado exitosamente en:\n" + destPath);
    }

    @Override
    public void onFileDownloadFailed(String error) {
        JOptionPane.showMessageDialog(frame, "Error en la descarga: " + error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onCameraFrame(int userId, String userName, byte[] imageBytes) {
        frame.getRoomPanel().updateParticipantCamera(userId, userName, imageBytes);
    }

    @Override
    public void onRoomTerminated() {
        frame.getWelcomePanel().closeWaitingRoomDialog();
        frame.getRoomPanel().stopCameraLocal();
        session.clearRoom();

        frame.getWelcomePanel().enableJoinButton();

        JOptionPane.showMessageDialog(frame, "La reunión ha finalizado o la conexión con el servidor se ha cerrado.",
                "Reunión Finalizada", JOptionPane.INFORMATION_MESSAGE);
        frame.showCard("WELCOME");
    }

    public void requestFileDownload(int archivoId, String rutaDestinoLocal) {
        ClientService.getInstance().requestFileDownload(archivoId, rutaDestinoLocal);
    }
}
