package com.zoomsockets.view.panels;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.zoomsockets.controller.MainController;
import com.zoomsockets.model.ClientSession;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.view.components.ParticipantVideoPanel;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomPanel extends JPanel {

    private MainController controller;

    private JLabel lblRoomTitle, lblRoomCode, lblMyStatus;
    private JPanel panelVideoGrid;
    private JTextArea areaChat;
    private JTextField txtChatMessage;
    private JButton btnSendChat;
    private JButton btnToggleCam;
    private JButton btnShareFile;
    private JButton btnLeave;
    private JPanel panelWaitingRoom;
    private DefaultListModel<String> modelWaitingList;
    private JList<String> listWaitingUsers;
    private Map<Integer, SolicitudSala> pendingMap = new HashMap<>();

    private DefaultListModel<String> modelFilesList;
    private JList<String> listSharedFiles;
    private Map<String, String> physicalFilesMap = new HashMap<>(); 

    private final Map<Integer, ParticipantVideoPanel> participantPanels = new HashMap<>();

    private Thread cameraThread;
    private boolean isCameraOn = false;
    private Webcam selectedWebcam = null;

    public RoomPanel(MainController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBorder(new EmptyBorder(10, 15, 10, 15));
        panelHeader.setBackground(new Color(30, 30, 30));

        JPanel panelLeftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelLeftHeader.setOpaque(false);
        lblRoomTitle = new JLabel("Sala de Reunión");
        lblRoomTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblRoomTitle.setForeground(Color.WHITE);
        panelLeftHeader.add(lblRoomTitle);

        lblRoomCode = new JLabel("CÓDIGO: -");
        lblRoomCode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRoomCode.setForeground(new Color(30, 144, 255));
        panelLeftHeader.add(lblRoomCode);

        JButton btnCopyCode = new JButton("Copiar");
        btnCopyCode.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnCopyCode.addActionListener(e -> {
            String code = controller.getSession().getActiveRoomCode();
            if (code != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
                JOptionPane.showMessageDialog(this, "Código de sala copiado al portapapeles.");
            }
        });
        panelLeftHeader.add(btnCopyCode);
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);

        JPanel panelRightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelRightHeader.setOpaque(false);
        lblMyStatus = new JLabel("Yo: Nombre (Rol)");
        lblMyStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMyStatus.setForeground(Color.LIGHT_GRAY);
        panelRightHeader.add(lblMyStatus);

        JButton btnRename = new JButton("Renombrar");
        btnRename.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnRename.addActionListener(e -> {
            String newName = JOptionPane.showInputDialog(this, "Introduce tu nuevo nombre para esta reunión:", controller.getSession().getMyName());
            if (newName != null && !newName.trim().isEmpty()) {
                controller.changeName(newName.trim());
            }
        });
        panelRightHeader.add(btnRename);

        panelHeader.add(panelRightHeader, BorderLayout.EAST);

        add(panelHeader, BorderLayout.NORTH);

        panelVideoGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        panelVideoGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelVideoGrid.setBackground(new Color(18, 18, 18));
        add(panelVideoGrid, BorderLayout.CENTER);

        JPanel panelSidebar = new JPanel(new BorderLayout());
        panelSidebar.setPreferredSize(new Dimension(320, 0));
        panelSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(60, 63, 65)));

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel panelChatTab = new JPanel(new BorderLayout());
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        areaChat.setWrapStyleWord(true);
        areaChat.setBackground(new Color(25, 25, 25));
        areaChat.setForeground(new Color(220, 220, 220));
        areaChat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaChat.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JScrollPane scrollChat = new JScrollPane(areaChat);
        panelChatTab.add(scrollChat, BorderLayout.CENTER);

        JPanel panelChatInput = new JPanel(new BorderLayout(5, 5));
        panelChatInput.setBorder(new EmptyBorder(8, 8, 8, 8));
        txtChatMessage = new JTextField();
        txtChatMessage.putClientProperty("JTextField.placeholderText", "Escribe un mensaje aquí...");
        txtChatMessage.putClientProperty("JTextField.showClearButton", true);
        txtChatMessage.addActionListener(e -> sendChatMessage());
        btnSendChat = new JButton("Enviar");
        btnSendChat.putClientProperty("JButton.buttonType", "roundRect");
        btnSendChat.addActionListener(e -> sendChatMessage());
        panelChatInput.add(txtChatMessage, BorderLayout.CENTER);
        panelChatInput.add(btnSendChat, BorderLayout.EAST);
        panelChatTab.add(panelChatInput, BorderLayout.SOUTH);

        tabbedPane.addTab("Chat de Texto", panelChatTab);

        JPanel panelFilesTab = new JPanel(new BorderLayout());
        panelFilesTab.setBorder(new EmptyBorder(8, 8, 8, 8));

        modelFilesList = new DefaultListModel<>();
        listSharedFiles = new JList<>(modelFilesList);
        listSharedFiles.setBackground(new Color(25, 25, 25));
        JScrollPane scrollFiles = new JScrollPane(listSharedFiles);
        panelFilesTab.add(scrollFiles, BorderLayout.CENTER);

        JPanel panelFilesButtons = new JPanel(new GridLayout(2, 1, 5, 5));
        panelFilesButtons.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        btnShareFile = new JButton("Compartir Documento");
        btnShareFile.setBackground(new Color(46, 139, 87));
        btnShareFile.setForeground(Color.WHITE);
        btnShareFile.putClientProperty("JButton.buttonType", "roundRect");
        btnShareFile.addActionListener(e -> performShareFile());
        
        JButton btnDownloadFile = new JButton("Descargar Archivo Seleccionado");
        btnDownloadFile.putClientProperty("JButton.buttonType", "roundRect");
        btnDownloadFile.addActionListener(e -> performDownloadFile());

        panelFilesButtons.add(btnShareFile);
        panelFilesButtons.add(btnDownloadFile);
        panelFilesTab.add(panelFilesButtons, BorderLayout.SOUTH);

        tabbedPane.addTab("Documentos", panelFilesTab);

        panelSidebar.add(tabbedPane, BorderLayout.CENTER);

        panelWaitingRoom = new JPanel(new BorderLayout());
        panelWaitingRoom.setPreferredSize(new Dimension(320, 160));
        panelWaitingRoom.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(65, 68, 75)), "Sala de Espera (Invitados Pendientes)"));
        panelWaitingRoom.setVisible(false);

        modelWaitingList = new DefaultListModel<>();
        listWaitingUsers = new JList<>(modelWaitingList);
        listWaitingUsers.setBackground(new Color(25, 25, 25));
        JScrollPane scrollWaiting = new JScrollPane(listWaitingUsers);
        panelWaitingRoom.add(scrollWaiting, BorderLayout.CENTER);

        JPanel panelWaitingButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        panelWaitingButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JButton btnAcceptUser = new JButton("Admitir");
        btnAcceptUser.setBackground(new Color(46, 139, 87));
        btnAcceptUser.setForeground(Color.WHITE);
        btnAcceptUser.putClientProperty("JButton.buttonType", "roundRect");
        btnAcceptUser.addActionListener(e -> respondWaitingRequest("ACCEPT"));

        JButton btnRejectUser = new JButton("Rechazar");
        btnRejectUser.setBackground(new Color(178, 34, 34));
        btnRejectUser.setForeground(Color.WHITE);
        btnRejectUser.putClientProperty("JButton.buttonType", "roundRect");
        btnRejectUser.addActionListener(e -> respondWaitingRequest("REJECT"));

        panelWaitingButtons.add(btnAcceptUser);
        panelWaitingButtons.add(btnRejectUser);
        panelWaitingRoom.add(panelWaitingButtons, BorderLayout.SOUTH);

        panelSidebar.add(panelWaitingRoom, BorderLayout.SOUTH);

        add(panelSidebar, BorderLayout.EAST);

        JPanel panelControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelControls.setBackground(new Color(30, 30, 30));

        btnToggleCam = new JButton("Iniciar Cámara");
        btnToggleCam.setIcon(UIManager.getIcon("FileView.computerIcon"));
        btnToggleCam.setBackground(new Color(70, 70, 70));
        btnToggleCam.putClientProperty("JButton.buttonType", "roundRect");
        btnToggleCam.addActionListener(e -> toggleCamera());

        btnLeave = new JButton("Abandonar");
        btnLeave.setBackground(new Color(178, 34, 34));
        btnLeave.setForeground(Color.WHITE);
        btnLeave.putClientProperty("JButton.buttonType", "roundRect");
        btnLeave.addActionListener(e -> controller.leaveRoom());

        panelControls.add(btnToggleCam);
        panelControls.add(btnLeave);
        add(panelControls, BorderLayout.SOUTH);
    }

    public void setupHostRoom(String codigoSala, String nombreSala, ClientSession session) {
        lblRoomTitle.setText(nombreSala);
        lblRoomCode.setText("CÓDIGO: " + codigoSala);
        lblMyStatus.setText("Yo: " + session.getMyName() + " (" + session.getMyRole() + ")");

        panelWaitingRoom.setVisible(true);
        modelWaitingList.clear();
        pendingMap.clear();

        resetRoomUI(session);
    }

    public void setupParticipantRoom(String nombreSala, ClientSession session) {
        lblRoomTitle.setText(nombreSala);
        lblRoomCode.setText("CÓDIGO: " + session.getActiveRoomCode());
        lblMyStatus.setText("Yo: " + session.getMyName() + " (" + session.getMyRole() + ")");

        panelWaitingRoom.setVisible(false);

        resetRoomUI(session);
    }

    private void resetRoomUI(ClientSession session) {
        panelVideoGrid.removeAll();
        participantPanels.clear();
        addOrUpdateParticipantVideo(session.getMyUserId(), session.getMyName(), null);

        modelFilesList.clear();
        physicalFilesMap.clear();
        areaChat.setText("");
    }

    private void sendChatMessage() {
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;

        controller.sendChatMessage(msg);
        txtChatMessage.setText("");
    }

    private void performShareFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecciona un documento para compartir");
        int retVal = fc.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            controller.shareFile(selectedFile);
        }
    }

    private void performDownloadFile() {
        String selected = listSharedFiles.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un archivo de la lista.", "Descarga", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String physicalName = physicalFilesMap.get(selected);
        if (physicalName == null) return;

        File serverFile = new File("uploads", physicalName);
        if (!serverFile.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo físico no se encuentra en el servidor.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(selected.split(" - ")[0]));
        int ret = fc.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File dest = fc.getSelectedFile();
            try (FileInputStream in = new FileInputStream(serverFile);
                 FileOutputStream out = new FileOutputStream(dest)) {
                
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente en:\n" + dest.getAbsolutePath());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void respondWaitingRequest(String action) {
        int selectedIndex = listWaitingUsers.getSelectedIndex();
        if (selectedIndex == -1) return;

        String val = listWaitingUsers.getSelectedValue();
        int userId = -1;
        for (Map.Entry<Integer, SolicitudSala> entry : pendingMap.entrySet()) {
            if (val.contains(entry.getValue().getNombreUsuario())) {
                userId = entry.getKey();
                break;
            }
        }

        if (userId != -1) {
            controller.respondWaitingRequest(userId, action);
        }
    }

    public void updateWaitingList(List<SolicitudSala> pendingUsers) {
        modelWaitingList.clear();
        pendingMap.clear();
        if (pendingUsers != null) {
            for (SolicitudSala s : pendingUsers) {
                pendingMap.put(s.getIdUsuario(), s);
                modelWaitingList.addElement(s.getNombreUsuario() + " (Solicitante)");
            }
        }
    }

    public void updateRoomMembers(List<Usuario> activeUsers, int myUserId) {
        if (activeUsers == null) return;
        
        // Actualizar mi propio nombre
        for (Usuario u : activeUsers) {
            if (u.getIdUsuario() == myUserId) {
                controller.getSession().setMyName(u.getNombres());
                lblMyStatus.setText("Yo: " + u.getNombres() + " (" + controller.getSession().getMyRole() + ")");
                ParticipantVideoPanel p = participantPanels.get(myUserId);
                if (p != null) {
                    p.setUserName(u.getNombres());
                }
                break;
            }
        }

        participantPanels.entrySet().removeIf(entry -> {
            int userId = entry.getKey();
            if (userId == myUserId) return false; 

            boolean stillActive = false;
            for (Usuario u : activeUsers) {
                if (u.getIdUsuario() == userId) {
                    stillActive = true;
                    break;
                }
            }

            if (!stillActive) {
                panelVideoGrid.remove(entry.getValue());
                panelVideoGrid.revalidate();
                panelVideoGrid.repaint();
                return true;
            }
            return false;
        });

        for (Usuario u : activeUsers) {
            if (u.getIdUsuario() != myUserId) {
                addOrUpdateParticipantVideo(u.getIdUsuario(), u.getNombres(), null);
                ParticipantVideoPanel p = participantPanels.get(u.getIdUsuario());
                if (p != null) p.setUserName(u.getNombres());
            }
        }
    }

    private void addOrUpdateParticipantVideo(int userId, String name, byte[] frameBytes) {
        ParticipantVideoPanel p = participantPanels.get(userId);
        if (p == null) {
            p = new ParticipantVideoPanel(name);
            participantPanels.put(userId, p);
            panelVideoGrid.add(p);
            panelVideoGrid.revalidate();
            panelVideoGrid.repaint();
        }

        if (frameBytes != null && frameBytes.length > 0) {
            p.updateFrame(frameBytes);
        } else {
            p.showAvatar();
        }
    }

    public void addChatMessage(String senderName, String content) {
        areaChat.append("[" + senderName + "]: " + content + "\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    public void addSharedFile(String senderName, String filename, String physicalName) {
        areaChat.append("[SISTEMA]: " + senderName + " ha compartido el archivo '" + filename + "'\n");
        
        String listValue = filename + " - Enviado por " + senderName;
        modelFilesList.addElement(listValue);
        physicalFilesMap.put(listValue, physicalName);
    }

    public void updateParticipantCamera(int userId, String userName, byte[] imageBytes) {
        addOrUpdateParticipantVideo(userId, userName, imageBytes);
    }

    private void toggleCamera() {
        if (!isCameraOn) {
            int opt = JOptionPane.showConfirmDialog(
                this,
                "¿Desea permitir que la aplicación acceda a su cámara local?",
                "Permiso de Cámara",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (opt == JOptionPane.YES_OPTION) {
                List<Webcam> webcams = Webcam.getWebcams();

                if (webcams.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "No se detectó ninguna cámara de hardware.\nSe usará la simulación de video.",
                        "Sin cámara", JOptionPane.WARNING_MESSAGE);
                    selectedWebcam = null;
                } else if (webcams.size() == 1) {
                    selectedWebcam = webcams.get(0);
                } else {
                    String[] nombres = webcams.stream()
                        .map(Webcam::getName)
                        .toArray(String[]::new);
                    String elegida = (String) JOptionPane.showInputDialog(
                        this,
                        "Selecciona la cámara a usar:",
                        "Selección de Cámara",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        nombres,
                        nombres[0]);
                    if (elegida == null) return; 
                    selectedWebcam = webcams.stream()
                        .filter(w -> w.getName().equals(elegida))
                        .findFirst().orElse(webcams.get(0));
                }
            } else {
                selectedWebcam = null;
            }

            isCameraOn = true;
            btnToggleCam.setText("Detener Cámara");
            btnToggleCam.setBackground(new Color(178, 34, 34));

            ClientSession session = controller.getSession();
            addOrUpdateParticipantVideo(session.getMyUserId(), session.getMyName(), null);

            if (selectedWebcam != null) {
                cameraThread = new Thread(this::runRealCamera, "RealCamera-Thread");
            } else {
                cameraThread = new Thread(this::runSimulatedCamera, "SimulatedCamera-Thread");
            }
            cameraThread.setDaemon(true);
            cameraThread.start();
        } else {
            stopCameraLocal();
            controller.notifyCameraOff();
        }
    }

    public void stopCameraLocal() {
        isCameraOn = false;
        btnToggleCam.setText("Iniciar Cámara");
        btnToggleCam.setBackground(new Color(70, 70, 70));

        if (cameraThread != null) {
            cameraThread.interrupt();
            cameraThread = null;
        }

        if (selectedWebcam != null && selectedWebcam.isOpen()) {
            selectedWebcam.close();
        }
        selectedWebcam = null;

        ClientSession session = controller.getSession();
        if (session != null) {
            ParticipantVideoPanel p = participantPanels.get(session.getMyUserId());
            if (p != null) {
                p.showAvatar();
            }
        }
    }

    private void runRealCamera() {
        selectedWebcam.setCustomViewSizes(WebcamResolution.QVGA.getSize()); 
        selectedWebcam.setViewSize(WebcamResolution.QVGA.getSize());

        ImageWriter jpegWriter = null;
        try {
            if (!selectedWebcam.open()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No se pudo abrir la cámara seleccionada.\nVerifica que no esté en uso.",
                    "Error de Cámara", JOptionPane.ERROR_MESSAGE));
                return;
            }

            jpegWriter = ImageIO.getImageWritersByFormatName("jpg").next();
            JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
            jpegParams.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
            jpegParams.setCompressionQuality(0.5f);

            while (isCameraOn && !Thread.currentThread().isInterrupted()) {
                BufferedImage img = selectedWebcam.getImage();
                if (img == null) {
                    Thread.sleep(200);
                    continue;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
                    jpegWriter.setOutput(mcios);
                    jpegWriter.write(null, new javax.imageio.IIOImage(img, null, null), jpegParams);
                }
                byte[] jpegBytes = baos.toByteArray();

                SwingUtilities.invokeLater(() -> {
                    ParticipantVideoPanel p = participantPanels.get(controller.getSession().getMyUserId());
                    if (p != null) p.updateFrame(jpegBytes);
                });

                controller.sendCameraFrame(jpegBytes);

                Thread.sleep(200); 
            }
        } catch (InterruptedException e) {
            // normal
        } catch (IOException e) {
            System.err.println("Error al capturar/enviar frame de cámara real: " + e.getMessage());
        } finally {
            if (jpegWriter != null) {
                jpegWriter.dispose();
            }
            if (selectedWebcam != null && selectedWebcam.isOpen()) {
                selectedWebcam.close();
            }
        }
    }

    private void runSimulatedCamera() {
        int width = 320;
        int height = 240;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        
        int circleX = width / 2;
        int circleY = height / 2;
        int speedX = 4;
        int speedY = 3;
        int radius = 25;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        try {
            while (isCameraOn && !Thread.currentThread().isInterrupted()) {
                GradientPaint gp = new GradientPaint(0, 0, new Color(24, 28, 36), width, height, new Color(48, 56, 72));
                g2.setPaint(gp);
                g2.fillRect(0, 0, width, height);

                g2.setColor(new Color(255, 255, 255, 20));
                for (int i = 0; i < width; i += 40) g2.drawLine(i, 0, i, height);
                for (int i = 0; i < height; i += 40) g2.drawLine(0, i, width, i);

                circleX += speedX;
                circleY += speedY;
                if (circleX - radius < 0 || circleX + radius > width) speedX = -speedX;
                if (circleY - radius < 0 || circleY + radius > height) speedY = -speedY;

                g2.setColor(new Color(30, 144, 255, 180));
                g2.fillOval(circleX - radius, circleY - radius, radius * 2, radius * 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(circleX - radius, circleY - radius, radius * 2, radius * 2);

                ClientSession session = controller.getSession();
                String myName = session.getMyName() != null ? session.getMyName() : "Unknown";
                String myRole = session.getMyRole() != null ? session.getMyRole() : "User";

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                g2.drawString("LIVE: " + myName.toUpperCase(), 15, 25);
                g2.drawString("ROL : " + myRole, 15, 42);
                
                g2.setColor(Color.GREEN);
                g2.drawString("CAM : ON (320x240 @ 5fps)", 15, 60);

                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString(sdf.format(new Date()), 15, height - 20);

                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g2.setColor(Color.RED);
                    g2.fillOval(width - 30, 15, 10, 10);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                byte[] jpegBytes = baos.toByteArray();

                SwingUtilities.invokeLater(() -> {
                    ParticipantVideoPanel p = participantPanels.get(session.getMyUserId());
                    if (p != null) {
                        p.updateFrame(jpegBytes);
                    }
                });

                controller.sendCameraFrame(jpegBytes);

                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            // normal
        } catch (IOException e) {
            System.err.println("Error al serializar fotograma de cámara simulada: " + e.getMessage());
        } finally {
            g2.dispose();
        }
    }
}
