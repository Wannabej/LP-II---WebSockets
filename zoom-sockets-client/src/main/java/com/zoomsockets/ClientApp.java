package com.zoomsockets;

import com.formdev.flatlaf.FlatDarkLaf;
import com.zoomsockets.client.ClientListener;
import com.zoomsockets.client.ClientService;
import com.zoomsockets.model.SolicitudSala;
import com.zoomsockets.model.Usuario;
import com.zoomsockets.protocol.ControlHeader;
import com.zoomsockets.protocol.NetworkFrame;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

public class ClientApp extends JFrame implements ClientListener {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    // Datos de sesión local
    private int myUserId;
    private String myName;
    private String myRole;
    private int activeRoomId;
    private String activeRoomCode;
    private String activeRoomName;

    // Componentes del Panel 1: Login
    private JTextField txtIp, txtPort, txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    // Componentes del Panel 2: Bienvenida (Crear/Unirse)
    private JLabel lblWelcomeMsg;
    private JTextField txtCrearNombreSala, txtUnirseCodigoSala;
    private JButton btnCrearSala, btnUnirseSala;

    // Componentes del Panel 3: Sala de Videoconferencia
    private JLabel lblRoomTitle, lblRoomCode, lblMyStatus;
    private JPanel panelVideoGrid;
    private JTextArea areaChat;
    private JTextField txtChatMessage;
    private JButton btnSendChat;
    private JButton btnToggleCam;
    private JButton btnShareFile;
    private JButton btnLeave;
    private JPanel panelWaitingRoom; // Barra lateral para el host
    private DefaultListModel<String> modelWaitingList;
    private JList<String> listWaitingUsers;
    private Map<Integer, SolicitudSala> pendingMap = new HashMap<>();

    // Ventana dedicada de sala de espera (participante en espera de admisión)
    private JDialog waitingRoomDialog;
    private javax.swing.Timer spinnerTimer;

    // Descargas
    private DefaultListModel<String> modelFilesList;
    private JList<String> listSharedFiles;
    private Map<String, String> physicalFilesMap = new HashMap<>(); // Nombre visible -> Nombre fisico en servidor

    // Grid de participantes y sus correspondientes paneles visuales
    private final Map<Integer, ParticipantVideoPanel> participantPanels = new HashMap<>();

    // Hilos de la cámara simulada local
    private Thread cameraThread;
    private boolean isCameraOn = false;
    private Webcam selectedWebcam = null; // Cámara real seleccionada (null = usar simulación)

    public ClientApp() {
        super("Zoom-Sockets - Prototipo Académico");
        setupUI();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        // Al cerrar el JFrame, desconectar socket de manera limpia
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // Configurar los paneles principales en el CardLayout
        mainContainer.add(buildLoginPanel(), "LOGIN");
        mainContainer.add(buildWelcomePanel(), "WELCOME");
        mainContainer.add(buildRoomPanel(), "ROOM");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");

        ClientService.getInstance().setListener(this);
    }

    private void handleExit() {
        if (activeRoomId > 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas salir de la sala de reunión?",
                    "Confirmar salida", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                ControlHeader leaveHeader = new ControlHeader("LEAVE_ROOM");
                ClientService.getInstance().sendFrame(new NetworkFrame(leaveHeader.toJson()));
                try { Thread.sleep(200); } catch (InterruptedException ex) {}
                ClientService.getInstance().disconnect();
                System.exit(0);
            }
        } else {
            ClientService.getInstance().disconnect();
            System.exit(0);
        }
    }

    // ==========================================
    // 1. PANEL DE LOGIN (Flat Dark Theme)
    // ==========================================
    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 21, 24));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 52, 58), 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));
        formCard.setBackground(new Color(32, 33, 37));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Título de la tarjeta
        JLabel lblTitle = new JLabel("Iniciar Sesión", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(240, 240, 240));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblTitle, gbc);

        // Separador estético
        JSeparator sep = new JSeparator();
        gbc.gridy = 1; gbc.insets = new Insets(5, 8, 15, 8);
        formCard.add(sep, gbc);

        // Campos de conexión
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formCard.add(new JLabel("Servidor IP:"), gbc);
        txtIp = new JTextField("localhost", 12);
        txtIp.putClientProperty("JTextField.placeholderText", "IP del Servidor");
        txtIp.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtIp, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formCard.add(new JLabel("Puerto:"), gbc);
        txtPort = new JTextField("8080", 12);
        txtPort.putClientProperty("JTextField.placeholderText", "Puerto");
        txtPort.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtPort, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formCard.add(new JLabel("Correo:"), gbc);
        txtCorreo = new JTextField("host@zoom.com", 12); // Semilla por defecto
        txtCorreo.putClientProperty("JTextField.placeholderText", "correo@ejemplo.com");
        txtCorreo.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtCorreo, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        formCard.add(new JLabel("Contraseña:"), gbc);
        txtPassword = new JPasswordField("password123", 12);
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtPassword.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtPassword, gbc);

        // Botón Iniciar Sesión
        btnLogin = new JButton("Conectar e Ingresar");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(30, 144, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.addActionListener(e -> performLogin());
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        formCard.add(btnLogin, gbc);

        // Nota aclaratoria
        JLabel lblHelp = new JLabel("Semillas: host@zoom.com, juan@zoom.com, maria@zoom.com (clave: password123)", JLabel.CENTER);
        lblHelp.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblHelp.setForeground(Color.GRAY);
        gbc.gridy = 7; gbc.insets = new Insets(10, 8, 0, 8);
        formCard.add(lblHelp, gbc);

        panel.add(formCard);
        return panel;
    }

    private void performLogin() {
        String ip = txtIp.getText().trim();
        String portStr = txtPort.getText().trim();
        String correo = txtCorreo.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (ip.isEmpty() || portStr.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            btnLogin.setEnabled(false);
            btnLogin.setText("Conectando...");

            // Conectar al socket
            ClientService.getInstance().connect(ip, port);

            // Enviar login_request
            ControlHeader loginReq = new ControlHeader("LOGIN_REQUEST");
            loginReq.setEmail(correo);
            loginReq.setPassword(password);

            ClientService.getInstance().sendFrame(new NetworkFrame(loginReq.toJson()));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Conectar e Ingresar");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor de sockets en " + ip + ":" + portStr + "\nDetalle: " + e.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Conectar e Ingresar");
        }
    }

    // ==========================================
    // 2. PANEL DE BIENVENIDA (CREAR / UNIRSE)
    // ==========================================
    private JPanel buildWelcomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 21, 24));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 52, 58), 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));
        formCard.setBackground(new Color(32, 33, 37));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        lblWelcomeMsg = new JLabel("Bienvenido, Usuario", JLabel.CENTER);
        lblWelcomeMsg.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblWelcomeMsg.setForeground(new Color(30, 144, 255));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblWelcomeMsg, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = 1; gbc.insets = new Insets(5, 10, 20, 10);
        formCard.add(sep, gbc);

        // Sección Crear Sala (Lado Izquierdo)
        JPanel panelCrear = new JPanel(new BorderLayout(8, 8));
        panelCrear.setOpaque(false);
        panelCrear.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(65, 68, 75)), "Anfitrión: Crear Sala"));
        
        txtCrearNombreSala = new JTextField("Sala de Redes y Concurrencia", 15);
        txtCrearNombreSala.putClientProperty("JTextField.placeholderText", "Nombre de la sala");
        txtCrearNombreSala.putClientProperty("JTextField.showClearButton", true);
        panelCrear.add(txtCrearNombreSala, BorderLayout.CENTER);
        
        btnCrearSala = new JButton("Iniciar Sala");
        btnCrearSala.setBackground(new Color(46, 139, 87));
        btnCrearSala.setForeground(Color.WHITE);
        btnCrearSala.putClientProperty("JButton.buttonType", "roundRect");
        btnCrearSala.addActionListener(e -> {
            String nombre = txtCrearNombreSala.getText().trim();
            if (nombre.isEmpty()) return;
            
            ControlHeader createReq = new ControlHeader("CREATE_ROOM");
            createReq.setNombreSala(nombre);
            ClientService.getInstance().sendFrame(new NetworkFrame(createReq.toJson()));
        });
        panelCrear.add(btnCrearSala, BorderLayout.SOUTH);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        formCard.add(panelCrear, gbc);

        // Sección Unirse a Sala (Lado Derecho)
        JPanel panelUnirse = new JPanel(new BorderLayout(8, 8));
        panelUnirse.setOpaque(false);
        panelUnirse.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(65, 68, 75)), "Participante: Unirse"));
        
        txtUnirseCodigoSala = new JTextField("", 15);
        txtUnirseCodigoSala.putClientProperty("JTextField.placeholderText", "Código de Sala");
        txtUnirseCodigoSala.putClientProperty("JTextField.showClearButton", true);
        txtUnirseCodigoSala.setToolTipText("Introduce el código de 8 caracteres");
        panelUnirse.add(txtUnirseCodigoSala, BorderLayout.CENTER);
        
        btnUnirseSala = new JButton("Unirse a la Reunión");
        btnUnirseSala.setBackground(new Color(30, 144, 255));
        btnUnirseSala.setForeground(Color.WHITE);
        btnUnirseSala.putClientProperty("JButton.buttonType", "roundRect");
        btnUnirseSala.addActionListener(e -> {
            String codigo = txtUnirseCodigoSala.getText().trim().toUpperCase();
            if (codigo.isEmpty()) return;
            
            btnUnirseSala.setEnabled(false);
            btnUnirseSala.setText("Esperando al host...");

            ControlHeader joinReq = new ControlHeader("JOIN_ROOM_REQUEST");
            joinReq.setCodigoSala(codigo);
            ClientService.getInstance().sendFrame(new NetworkFrame(joinReq.toJson()));
        });
        panelUnirse.add(btnUnirseSala, BorderLayout.SOUTH);

        gbc.gridx = 1;
        formCard.add(panelUnirse, gbc);

        // Botón de Cerrar Sesión
        JButton btnLogout = new JButton("Cerrar Sesión");
        btnLogout.putClientProperty("JButton.buttonType", "roundRect");
        btnLogout.addActionListener(e -> {
            ClientService.getInstance().disconnect();
            cardLayout.show(mainContainer, "LOGIN");
            btnLogin.setEnabled(true);
            btnLogin.setText("Conectar e Ingresar");
        });
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(20, 10, 10, 10);
        formCard.add(btnLogout, gbc);

        panel.add(formCard);
        return panel;
    }

    // ==========================================
    // 3. PANEL DE LA SALA DE VIDEOCONFERENCIA
    // ==========================================
    private JPanel buildRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // A. BARRA SUPERIOR (HEADER)
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
            if (activeRoomCode != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(activeRoomCode), null);
                JOptionPane.showMessageDialog(this, "Código de sala copiado al portapapeles.");
            }
        });
        panelLeftHeader.add(btnCopyCode);
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);

        lblMyStatus = new JLabel("Yo: Nombre (Rol)");
        lblMyStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMyStatus.setForeground(Color.LIGHT_GRAY);
        panelHeader.add(lblMyStatus, BorderLayout.EAST);

        panel.add(panelHeader, BorderLayout.NORTH);

        // B. ÁREA CENTRAL: GRID DE VIDEO (Participantes)
        panelVideoGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        panelVideoGrid.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelVideoGrid.setBackground(new Color(18, 18, 18));
        panel.add(panelVideoGrid, BorderLayout.CENTER);

        // C. BARRA LATERAL DERECHA: CHAT, ARCHIVOS Y SALA DE ESPERA (HOST)
        JPanel panelSidebar = new JPanel(new BorderLayout());
        panelSidebar.setPreferredSize(new Dimension(320, 0));
        panelSidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(60, 63, 65)));

        // Pestañas (TabbedPane) para Chat y Compartición de Archivos
        JTabbedPane tabbedPane = new JTabbedPane();

        // C1. Sub-panel: CHAT
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

        // C2. Sub-panel: ARCHIVOS
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

        // C3. Sub-panel: SALA DE ESPERA (Solo visible para Host)
        panelWaitingRoom = new JPanel(new BorderLayout());
        panelWaitingRoom.setPreferredSize(new Dimension(320, 160));
        panelWaitingRoom.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(65, 68, 75)), "Sala de Espera (Invitados Pendientes)"));
        panelWaitingRoom.setVisible(false); // Por defecto oculto, se activa si soy Host

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

        panel.add(panelSidebar, BorderLayout.EAST);

        // D. BARRA DE CONTROL INFERIOR (CONTROLES)
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
        btnLeave.addActionListener(e -> handleExit());

        panelControls.add(btnToggleCam);
        panelControls.add(btnLeave);
        panel.add(panelControls, BorderLayout.SOUTH);

        return panel;
    }

    private void sendChatMessage() {
        String msg = txtChatMessage.getText().trim();
        if (msg.isEmpty()) return;

        ControlHeader chat = new ControlHeader("CHAT_MESSAGE");
        chat.setContenido(msg);
        
        ClientService.getInstance().sendFrame(new NetworkFrame(chat.toJson()));
        txtChatMessage.setText("");
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
            ControlHeader admit = new ControlHeader("ADMIT_USER");
            admit.setIdUsuario(userId);
            admit.setAction(action);
            ClientService.getInstance().sendFrame(new NetworkFrame(admit.toJson()));
        }
    }

    // ==========================================
    // SALA DE ESPERA — Ventana Dedicada para Participante
    // ==========================================
    private void showWaitingRoomDialog(String roomName) {
        if (waitingRoomDialog != null && waitingRoomDialog.isVisible()) {
            waitingRoomDialog.dispose();
        }

        waitingRoomDialog = new JDialog(this, "Sala de Espera", false);
        waitingRoomDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitingRoomDialog.setSize(440, 360);
        waitingRoomDialog.setLocationRelativeTo(this);
        waitingRoomDialog.setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(new Color(20, 21, 24));
        content.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Ícono
        JLabel lblIcon = new JLabel("\u23F3", JLabel.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        gbc.gridy = 0;
        content.add(lblIcon, gbc);

        // Título
        JLabel lblTitle = new JLabel("Sala de Espera", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 144, 255));
        gbc.gridy = 1;
        content.add(lblTitle, gbc);

        // Nombre de la sala
        JLabel lblSalaName = new JLabel(roomName, JLabel.CENTER);
        lblSalaName.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblSalaName.setForeground(new Color(150, 150, 150));
        gbc.gridy = 2;
        content.add(lblSalaName, gbc);

        // Separador
        JSeparator sep = new JSeparator();
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 15, 0);
        content.add(sep, gbc);
        gbc.insets = new Insets(8, 0, 8, 0);

        // Mensaje
        JLabel lblMsg = new JLabel(
            "<html><div style='text-align:center;'>Aguarda mientras el anfitrión<br>" +
            "revisa tu solicitud de ingreso.</div></html>", JLabel.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMsg.setForeground(new Color(200, 200, 200));
        gbc.gridy = 4;
        content.add(lblMsg, gbc);

        // Spinner animado
        String[] spinnerFrames = { "|", "/", "\u2014", "\\" };
        JLabel lblSpinner = new JLabel(spinnerFrames[0] + "  Esperando respuesta del host...", JLabel.CENTER);
        lblSpinner.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblSpinner.setForeground(new Color(30, 144, 255));
        gbc.gridy = 5;
        content.add(lblSpinner, gbc);

        // Timer para animar el spinner en el EDT
        int[] frameIdx = { 0 };
        if (spinnerTimer != null) spinnerTimer.stop();
        spinnerTimer = new javax.swing.Timer(150, e -> {
            frameIdx[0] = (frameIdx[0] + 1) % spinnerFrames.length;
            lblSpinner.setText(spinnerFrames[frameIdx[0]] + "  Esperando respuesta del host...");
        });
        spinnerTimer.start();

        // Botón cancelar
        JButton btnCancel = new JButton("Cancelar solicitud");
        btnCancel.setBackground(new Color(178, 34, 34));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> {
            // Notificar al servidor que abandona la sala de espera
            ControlHeader leaveHeader = new ControlHeader("LEAVE_ROOM");
            ClientService.getInstance().sendFrame(new NetworkFrame(leaveHeader.toJson()));
            closeWaitingRoomDialog();
            // Restaurar botón en panel de bienvenida
            SwingUtilities.invokeLater(() -> {
                btnUnirseSala.setEnabled(true);
                btnUnirseSala.setText("Unirse a la Reunión");
            });
        });
        gbc.gridy = 6;
        gbc.insets = new Insets(20, 0, 0, 0);
        content.add(btnCancel, gbc);

        waitingRoomDialog.setContentPane(content);
        waitingRoomDialog.setVisible(true);
    }

    private void closeWaitingRoomDialog() {
        if (spinnerTimer != null) {
            spinnerTimer.stop();
            spinnerTimer = null;
        }
        if (waitingRoomDialog != null) {
            waitingRoomDialog.dispose();
            waitingRoomDialog = null;
        }
    }

    private void performShareFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecciona un documento para compartir");
        int retVal = fc.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            // Validar tamaño máximo de 15MB para seguridad de red
            if (selectedFile.length() > 15 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, "El archivo excede el límite permitido de 15MB.", "Archivo pesado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            ClientService.getInstance().sendFile(activeRoomId, myUserId, selectedFile);
        }
    }

    private void performDownloadFile() {
        String selected = listSharedFiles.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un archivo de la lista.", "Descarga", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // El valor visible tiene formato "Nombre - Enviado por X"
        // Necesitamos recuperar el nombre físico único en el servidor
        String physicalName = physicalFilesMap.get(selected);
        if (physicalName == null) return;

        // En un Zoom real, solicitaríamos el archivo al servidor mediante sockets.
        // Dado que es un prototipo con almacenamiento físico local en el mismo host (el servidor corre en local),
        // simularemos la descarga copiando el archivo de la carpeta física de subidas a la ubicación seleccionada por el usuario.
        // Esto mantiene la arquitectura del prototipo simple, directa y visualmente funcional en la demo.
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

    // ==========================================
    // 4. CÁMARA SIMULADA LOCAL (HILO ANIMADO)
    // ==========================================
    private void toggleCamera() {
        if (!isCameraOn) {
            // Solicitar permiso de acceso a la cámara local
            int opt = JOptionPane.showConfirmDialog(
                this,
                "¿Desea permitir que la aplicación acceda a su cámara local?",
                "Permiso de Cámara",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (opt == JOptionPane.YES_OPTION) {
                // Detectar cámaras disponibles
                List<Webcam> webcams = Webcam.getWebcams();

                if (webcams.isEmpty()) {
                    // Sin hardware: avisar y usar simulación como fallback
                    JOptionPane.showMessageDialog(this,
                        "No se detectó ninguna cámara de hardware.\nSe usará la simulación de video.",
                        "Sin cámara", JOptionPane.WARNING_MESSAGE);
                    selectedWebcam = null;
                } else if (webcams.size() == 1) {
                    // Una sola cámara: usarla directamente
                    selectedWebcam = webcams.get(0);
                } else {
                    // Múltiples cámaras: mostrar diálogo de selección
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
                    if (elegida == null) return; // El usuario canceló
                    selectedWebcam = webcams.stream()
                        .filter(w -> w.getName().equals(elegida))
                        .findFirst().orElse(webcams.get(0));
                }
            } else {
                // Permiso denegado: usar simulación directamente
                selectedWebcam = null;
            }

            isCameraOn = true;
            btnToggleCam.setText("Detener Cámara");
            btnToggleCam.setBackground(new Color(178, 34, 34));

            // Agregar nuestro propio panel de video local en la grid
            addOrUpdateParticipantVideo(myUserId, myName, null);

            // Iniciar hilo de captura real o simulación
            if (selectedWebcam != null) {
                cameraThread = new Thread(this::runRealCamera, "RealCamera-Thread");
            } else {
                cameraThread = new Thread(this::runSimulatedCamera, "SimulatedCamera-Thread");
            }
            cameraThread.setDaemon(true);
            cameraThread.start();
        } else {
            stopCameraLocal();

            // Enviar frame vacío para notificar a los demás que apagamos cámara
            ControlHeader camHeader = new ControlHeader("CAMERA_FRAME");
            ClientService.getInstance().sendFrame(new NetworkFrame(camHeader.toJson(), new byte[0]));
        }
    }

    private void stopCameraLocal() {
        isCameraOn = false;
        btnToggleCam.setText("Iniciar Cámara");
        btnToggleCam.setBackground(new Color(70, 70, 70));

        if (cameraThread != null) {
            cameraThread.interrupt();
            cameraThread = null;
        }

        // Cerrar la webcam real si estaba abierta
        if (selectedWebcam != null && selectedWebcam.isOpen()) {
            selectedWebcam.close();
        }
        selectedWebcam = null;

        // Mostrar avatar por defecto para mí
        ParticipantVideoPanel p = participantPanels.get(myUserId);
        if (p != null) {
            p.showAvatar();
        }
    }

    /**
     * Captura frames de la webcam real, los comprime a JPEG y los envía por socket.
     * Resolución: 320x240. FPS objetivo: 5 (200ms por frame).
     */
    private void runRealCamera() {
        // Configurar resolución reducida para no saturar la red
        selectedWebcam.setCustomViewSizes(WebcamResolution.QVGA.getSize()); // 320x240
        selectedWebcam.setViewSize(WebcamResolution.QVGA.getSize());

        ImageWriter jpegWriter = null;
        try {
            if (!selectedWebcam.open()) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "No se pudo abrir la cámara seleccionada.\nVerifica que no esté en uso.",
                    "Error de Cámara", JOptionPane.ERROR_MESSAGE));
                return;
            }

            // Preparar compresor JPEG con calidad reducida (0.5) para ahorrar ancho de banda
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

                // Comprimir a JPEG con calidad controlada
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos)) {
                    jpegWriter.setOutput(mcios);
                    jpegWriter.write(null, new javax.imageio.IIOImage(img, null, null), jpegParams);
                }
                byte[] jpegBytes = baos.toByteArray();

                // Actualizar panel local en la UI
                SwingUtilities.invokeLater(() -> {
                    ParticipantVideoPanel p = participantPanels.get(myUserId);
                    if (p != null) p.updateFrame(jpegBytes);
                });

                // Enviar por socket a los demás participantes
                ControlHeader frameHeader = new ControlHeader("CAMERA_FRAME");
                ClientService.getInstance().sendFrame(new NetworkFrame(frameHeader.toJson(), jpegBytes));

                Thread.sleep(200); // 5 FPS
            }
        } catch (InterruptedException e) {
            // Detención del hilo normal
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
        
        // Coordenadas para animación del salvapantallas de la simulación
        int circleX = width / 2;
        int circleY = height / 2;
        int speedX = 4;
        int speedY = 3;
        int radius = 25;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        try {
            while (isCameraOn && !Thread.currentThread().isInterrupted()) {
                // RENDERIZAR FOTOGRAMA
                // Fondo gradiente moderno
                GradientPaint gp = new GradientPaint(0, 0, new Color(24, 28, 36), width, height, new Color(48, 56, 72));
                g2.setPaint(gp);
                g2.fillRect(0, 0, width, height);

                // Cuadrícula de simulación técnica
                g2.setColor(new Color(255, 255, 255, 20));
                for (int i = 0; i < width; i += 40) g2.drawLine(i, 0, i, height);
                for (int i = 0; i < height; i += 40) g2.drawLine(0, i, width, i);

                // Dibujar círculo rebotante simulando live-feed
                circleX += speedX;
                circleY += speedY;
                if (circleX - radius < 0 || circleX + radius > width) speedX = -speedX;
                if (circleY - radius < 0 || circleY + radius > height) speedY = -speedY;

                g2.setColor(new Color(30, 144, 255, 180));
                g2.fillOval(circleX - radius, circleY - radius, radius * 2, radius * 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(circleX - radius, circleY - radius, radius * 2, radius * 2);

                // Superponer textos informativos del stream
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                g2.drawString("LIVE: " + myName.toUpperCase(), 15, 25);
                g2.drawString("ROL : " + myRole, 15, 42);
                
                g2.setColor(Color.GREEN);
                g2.drawString("CAM : ON (320x240 @ 5fps)", 15, 60);

                // Timestamp dinámico
                g2.setColor(Color.LIGHT_GRAY);
                g2.drawString(sdf.format(new Date()), 15, height - 20);

                // Pequeño indicador rojo de grabación pulsante
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g2.setColor(Color.RED);
                    g2.fillOval(width - 30, 15, 10, 10);
                }

                // Renderizar frame local de inmediato en nuestra interfaz
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                byte[] jpegBytes = baos.toByteArray();

                // Actualizar nuestro panel local en la UI (EDT)
                SwingUtilities.invokeLater(() -> {
                    ParticipantVideoPanel p = participantPanels.get(myUserId);
                    if (p != null) {
                        p.updateFrame(jpegBytes);
                    }
                });

                // ENVIAR POR SOCKET A LOS DEMÁS PARTICIPANTES
                ControlHeader frameHeader = new ControlHeader("CAMERA_FRAME");
                ClientService.getInstance().sendFrame(new NetworkFrame(frameHeader.toJson(), jpegBytes));

                // Controlar FPS (5 fotogramas por segundo = 200 ms por cuadro)
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            // Detención del hilo normal
        } catch (IOException e) {
            System.err.println("Error al serializar fotograma de cámara simulada: " + e.getMessage());
        } finally {
            g2.dispose();
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

    private void removeParticipantVideo(int userId) {
        ParticipantVideoPanel p = participantPanels.remove(userId);
        if (p != null) {
            panelVideoGrid.remove(p);
            panelVideoGrid.revalidate();
            panelVideoGrid.repaint();
        }
    }

    // ==========================================
    // 5. IMPLEMENTACIÓN DE CLIENTLISTENER (RED)
    // ==========================================
    @Override
    public void onLoginResponse(boolean success, String error, String name, String role, int idUsuario) {
        if (success) {
            this.myUserId = idUsuario;
            this.myName = name;
            this.myRole = role;
            lblWelcomeMsg.setText("Bienvenido, " + name + " (" + role + ")");
            lblMyStatus.setText("Yo: " + name + " (" + role + ")");
            cardLayout.show(mainContainer, "WELCOME");
        } else {
            JOptionPane.showMessageDialog(this, "Error de autenticación: " + error, "Login fallido", JOptionPane.ERROR_MESSAGE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Conectar e Ingresar");
        }
    }

    @Override
    public void onCreateRoomResponse(boolean success, String error, String codigoSala, String nombreSala, int idSala) {
        if (success) {
            activeRoomId = idSala;
            activeRoomCode = codigoSala;
            activeRoomName = nombreSala;

            lblRoomTitle.setText(nombreSala);
            lblRoomCode.setText("CÓDIGO: " + codigoSala);

            // Dado que creé la sala, yo soy el Host. Mostrar sección de sala de espera
            panelWaitingRoom.setVisible(true);
            modelWaitingList.clear();
            pendingMap.clear();

            // Configurar grid limpia
            panelVideoGrid.removeAll();
            participantPanels.clear();
            addOrUpdateParticipantVideo(myUserId, myName, null);

            modelFilesList.clear();
            physicalFilesMap.clear();
            areaChat.setText("");

            cardLayout.show(mainContainer, "ROOM");
            System.out.println("Ingresado a la sala en rol Host. Código: " + codigoSala);
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo crear la sala:\n" + error, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onJoinRoomResponse(String status, String error, int idSala, String nombreSala) {
        if ("PENDING".equalsIgnoreCase(status)) {
            // Mantener botón deshabilitado mientras se espera; mostrar ventana dedicada
            // (el botón se rehabilita cuando se cierra el dialog por cancelación, admisión o rechazo)
            showWaitingRoomDialog(nombreSala != null ? nombreSala : txtUnirseCodigoSala.getText().trim().toUpperCase());
        } else if ("SUCCESS".equalsIgnoreCase(status)) {
            // Cerrar la ventana de espera (si sigue abierta) antes de entrar a la sala
            closeWaitingRoomDialog();

            btnUnirseSala.setEnabled(true);
            btnUnirseSala.setText("Unirse a la Reunión");

            activeRoomId = idSala;
            activeRoomCode = txtUnirseCodigoSala.getText().trim().toUpperCase();
            activeRoomName = nombreSala;

            lblRoomTitle.setText(nombreSala);
            lblRoomCode.setText("CÓDIGO: " + activeRoomCode);

            // Ocultar sección de sala de espera (solo es para el Host)
            panelWaitingRoom.setVisible(false);

            // Configurar grid limpia
            panelVideoGrid.removeAll();
            participantPanels.clear();
            addOrUpdateParticipantVideo(myUserId, myName, null);

            modelFilesList.clear();
            physicalFilesMap.clear();
            areaChat.setText("");

            cardLayout.show(mainContainer, "ROOM");
            System.out.println("Ingresado a la sala en rol Participante. Código: " + activeRoomCode);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            closeWaitingRoomDialog();
            btnUnirseSala.setEnabled(true);
            btnUnirseSala.setText("Unirse a la Reunión");
            JOptionPane.showMessageDialog(this,
                "Solicitud rechazada: " + error,
                "Acceso Denegado", JOptionPane.WARNING_MESSAGE);
        } else {
            closeWaitingRoomDialog();
            btnUnirseSala.setEnabled(true);
            btnUnirseSala.setText("Unirse a la Reunión");
            JOptionPane.showMessageDialog(this,
                "No se pudo unir a la sala: " + error,
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onWaitingRoomUpdate(List<SolicitudSala> pendingUsers) {
        modelWaitingList.clear();
        pendingMap.clear();
        if (pendingUsers != null) {
            for (SolicitudSala s : pendingUsers) {
                pendingMap.put(s.getIdUsuario(), s);
                modelWaitingList.addElement(s.getNombreUsuario() + " (Solicitante)");
            }
        }
    }

    @Override
    public void onRoomMembersUpdate(List<Usuario> activeUsers) {
        if (activeUsers == null) return;
        
        // Quitar participantes que ya no están activos
        participantPanels.entrySet().removeIf(entry -> {
            int userId = entry.getKey();
            if (userId == myUserId) return false; // Conservar yo mismo

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

        // Agregar nuevos participantes visuales
        for (Usuario u : activeUsers) {
            if (u.getIdUsuario() != myUserId) {
                addOrUpdateParticipantVideo(u.getIdUsuario(), u.getNombres(), null);
            }
        }
    }

    @Override
    public void onChatMessage(String senderName, String content, int senderId) {
        areaChat.append("[" + senderName + "]: " + content + "\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    @Override
    public void onFileShared(String senderName, String filename, String physicalName) {
        areaChat.append("[SISTEMA]: " + senderName + " ha compartido el archivo '" + filename + "'\n");
        
        String listValue = filename + " - Enviado por " + senderName;
        modelFilesList.addElement(listValue);
        physicalFilesMap.put(listValue, physicalName);
    }

    @Override
    public void onCameraFrame(int userId, String userName, byte[] imageBytes) {
        // Recibo frame de otro usuario
        addOrUpdateParticipantVideo(userId, userName, imageBytes);
    }

    @Override
    public void onRoomTerminated() {
        // Cerrar ventana de sala de espera si estaba pendiente
        closeWaitingRoomDialog();
        stopCameraLocal();
        activeRoomId = 0;
        activeRoomCode = null;
        activeRoomName = null;

        // Restaurar botón por si el participante aún estaba en espera
        btnUnirseSala.setEnabled(true);
        btnUnirseSala.setText("Unirse a la Reunión");

        JOptionPane.showMessageDialog(this,
            "La reunión ha finalizado o la conexión con el servidor se ha cerrado.",
            "Reunión Finalizada", JOptionPane.INFORMATION_MESSAGE);

        cardLayout.show(mainContainer, "WELCOME");
    }

    // ==========================================
    // 6. SUB-COMPONENTE: PANEL DE VIDEO INDIVIDUAL
    // ==========================================
    private static class ParticipantVideoPanel extends JPanel {
        private final String userName;
        private final JLabel labelName;
        private final JLabel labelVideo;
        private final JPanel panelAvatar;

        public ParticipantVideoPanel(String userName) {
            this.userName = userName;
            this.setLayout(new BorderLayout());
            this.setBackground(new Color(30, 32, 40));
            this.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 2, true));

            // Nombre del participante
            labelName = new JLabel("  " + userName, JLabel.LEFT);
            labelName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            labelName.setForeground(Color.WHITE);
            labelName.setPreferredSize(new Dimension(0, 24));
            labelName.setOpaque(true);
            labelName.setBackground(new Color(0, 0, 0, 150));
            this.add(labelName, BorderLayout.SOUTH);

            // Panel de Avatar por defecto (Cámara apagada)
            panelAvatar = new JPanel(new GridBagLayout());
            panelAvatar.setBackground(new Color(25, 27, 34));
            
            // Obtener iniciales
            String initials = "";
            String[] parts = userName.split(" ");
            if (parts.length > 0 && !parts[0].isEmpty()) initials += parts[0].substring(0, 1).toUpperCase();
            if (parts.length > 1 && !parts[1].isEmpty()) initials += parts[1].substring(0, 1).toUpperCase();
            if (initials.isEmpty()) initials = "?";

            JLabel labelAvatarText = new JLabel(initials, JLabel.CENTER);
            labelAvatarText.setFont(new Font("Segoe UI", Font.BOLD, 48));
            labelAvatarText.setForeground(Color.WHITE);
            labelAvatarText.setPreferredSize(new Dimension(100, 100));
            labelAvatarText.setOpaque(true);
            labelAvatarText.setBackground(new Color(30, 144, 255)); // Azul premium
            labelAvatarText.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true));
            
            panelAvatar.add(labelAvatarText);
            this.add(panelAvatar, BorderLayout.CENTER);

            // Componente para pintar fotogramas
            labelVideo = new JLabel("", JLabel.CENTER);
            labelVideo.setVisible(false);
            this.add(labelVideo, BorderLayout.CENTER);
        }

        public void showAvatar() {
            labelVideo.setVisible(false);
            panelAvatar.setVisible(true);
            this.revalidate();
            this.repaint();
        }

        public void updateFrame(byte[] jpegBytes) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(jpegBytes));
                if (img != null) {
                    // Escalar imagen al tamaño del panel manteniendo aspecto aproximado
                    int w = this.getWidth() > 0 ? this.getWidth() : 320;
                    int h = this.getHeight() > 0 ? this.getHeight() - 24 : 216; // Restar altura de etiqueta
                    
                    Image scaled = img.getScaledInstance(w, h, Image.SCALE_FAST);
                    labelVideo.setIcon(new ImageIcon(scaled));
                    
                    panelAvatar.setVisible(false);
                    labelVideo.setVisible(true);
                    this.revalidate();
                    this.repaint();
                }
            } catch (Exception e) {
                System.err.println("Error al decodificar frame de video: " + e.getMessage());
            }
        }
    }

    // ==========================================
    // 7. INICIADOR DEL CLIENTE (MAIN ENTRY)
    // ==========================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Aplicar Look & Feel Moderno Dark (FlatLaf)
                FlatDarkLaf.setup();
                
                ClientApp app = new ClientApp();
                app.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
