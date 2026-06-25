package com.zoomsockets.view.panels;

import com.zoomsockets.controller.MainController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

public class LoginPanel extends JPanel {

    private MainController controller;
    private JTextField txtIp, txtPort, txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    // Datos de los usuarios semilla
    private static final String[][] SEED_USERS = {
        {"Host Demo",       "host@zoom.com",  "Docente",     "password123"},
        {"Invitado Juan",   "juan@zoom.com",  "Estudiante",  "password123"},
        {"Invitada Maria",  "maria@zoom.com", "Estudiante",  "password123"}
    };

    // Colores distintos para cada avatar
    private static final Color[] AVATAR_COLORS = {
        new Color(30, 144, 255),   // Azul
        new Color(46, 139, 87),    // Verde
        new Color(178, 102, 238)   // Púrpura
    };

    public LoginPanel(MainController controller) {
        this.controller = controller;
        setLayout(new GridBagLayout());
        setBackground(new Color(20, 21, 24));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 52, 58), 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));
        formCard.setBackground(new Color(32, 33, 37));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ── Título ──
        JLabel lblTitle = new JLabel("Iniciar Sesión", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(240, 240, 240));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblTitle, gbc);

        JSeparator sep1 = new JSeparator();
        gbc.gridy = 1; gbc.insets = new Insets(4, 8, 12, 8);
        formCard.add(sep1, gbc);

        // ── Servidor y Puerto ──
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0; gbc.gridy = 2;
        formCard.add(createLabel("Servidor IP:"), gbc);
        txtIp = new JTextField("localhost", 12);
        txtIp.putClientProperty("JTextField.placeholderText", "IP del Servidor");
        txtIp.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtIp, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formCard.add(createLabel("Puerto:"), gbc);
        txtPort = new JTextField("8080", 12);
        txtPort.putClientProperty("JTextField.placeholderText", "Puerto");
        txtPort.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtPort, gbc);

        // ── Sección de selección rápida de usuario ──
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 8, 4, 8);
        JLabel lblQuickSelect = new JLabel("Selecciona tu usuario:", JLabel.CENTER);
        lblQuickSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblQuickSelect.setForeground(new Color(200, 200, 200));
        formCard.add(lblQuickSelect, gbc);

        // Panel con las tarjetas de usuario
        JPanel cardsPanel = new JPanel(new GridLayout(1, SEED_USERS.length, 12, 0));
        cardsPanel.setOpaque(false);
        for (int i = 0; i < SEED_USERS.length; i++) {
            cardsPanel.add(createUserCard(SEED_USERS[i][0], SEED_USERS[i][1], SEED_USERS[i][2], SEED_USERS[i][3], AVATAR_COLORS[i]));
        }
        gbc.gridy = 5;
        gbc.insets = new Insets(4, 8, 8, 8);
        formCard.add(cardsPanel, gbc);

        // ── Separador visual "o ingresa manualmente" ──
        JPanel separatorPanel = new JPanel(new GridBagLayout());
        separatorPanel.setOpaque(false);
        GridBagConstraints sgbc = new GridBagConstraints();

        sgbc.fill = GridBagConstraints.HORIZONTAL;
        sgbc.weightx = 1.0;
        sgbc.gridy = 0;

        JSeparator sepLeft = new JSeparator();
        sgbc.gridx = 0;
        separatorPanel.add(sepLeft, sgbc);

        JLabel lblOr = new JLabel("  o ingresa manualmente  ", JLabel.CENTER);
        lblOr.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblOr.setForeground(new Color(140, 140, 140));
        sgbc.gridx = 1; sgbc.weightx = 0;
        separatorPanel.add(lblOr, sgbc);

        JSeparator sepRight = new JSeparator();
        sgbc.gridx = 2; sgbc.weightx = 1.0;
        separatorPanel.add(sepRight, sgbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(10, 8, 10, 8);
        formCard.add(separatorPanel, gbc);

        // ── Campos de correo y contraseña (vacíos por defecto) ──
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 8, 6, 8);

        gbc.gridx = 0; gbc.gridy = 7;
        formCard.add(createLabel("Correo:"), gbc);
        txtCorreo = new JTextField("", 12);
        txtCorreo.putClientProperty("JTextField.placeholderText", "correo@ejemplo.com");
        txtCorreo.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtCorreo, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        formCard.add(createLabel("Contraseña:"), gbc);
        txtPassword = new JPasswordField("", 12);
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtPassword.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtPassword, gbc);

        // ── Botón de login manual ──
        btnLogin = new JButton("Conectar e Ingresar");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(30, 144, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.addActionListener(e -> onLoginClicked());
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 8, 6, 8);
        formCard.add(btnLogin, gbc);

        // ── Nota de ayuda ──
        JLabel lblHelp = new JLabel("Clave por defecto para todos: password123", JLabel.CENTER);
        lblHelp.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblHelp.setForeground(new Color(120, 120, 120));
        gbc.gridy = 10; gbc.insets = new Insets(4, 8, 0, 8);
        formCard.add(lblHelp, gbc);

        // ── Botón para ir al registro ──
        JButton btnGoRegister = new JButton("Crear una cuenta nueva");
        btnGoRegister.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnGoRegister.setForeground(new Color(30, 144, 255));
        btnGoRegister.setContentAreaFilled(false);
        btnGoRegister.setBorderPainted(false);
        btnGoRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGoRegister.addActionListener(e -> controller.getFrame().showCard("REGISTER"));
        gbc.gridy = 11; gbc.insets = new Insets(10, 8, 4, 8);
        formCard.add(btnGoRegister, gbc);

        add(formCard);
    }

    /**
     * Crea una tarjeta visual para un usuario semilla con avatar, nombre y rol.
     */
    private JPanel createUserCard(String name, String email, String role, String password, Color avatarColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(42, 43, 48));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 62, 68), 1, true),
                new EmptyBorder(14, 10, 14, 10)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Avatar con iniciales
        String initials = getInitials(name);
        AvatarLabel avatar = new AvatarLabel(initials, avatarColor, 50);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(avatar);

        card.add(Box.createRigidArea(new Dimension(0, 8)));

        // Nombre
        JLabel lblName = new JLabel(name.split(" ")[0], JLabel.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblName);

        card.add(Box.createRigidArea(new Dimension(0, 3)));

        // Rol
        JLabel lblRole = new JLabel(role, JLabel.CENTER);
        lblRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRole.setForeground(new Color(160, 160, 160));
        lblRole.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblRole);

        // ── Efectos de hover y clic ──
        final Color normalBg = card.getBackground();
        final Color hoverBg = new Color(52, 54, 62);
        final Color hoverBorder = avatarColor;
        final Color normalBorder = new Color(60, 62, 68);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBorder, 2, true),
                        new EmptyBorder(13, 9, 13, 9) // Compensar el borde más grueso
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(normalBg);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        new EmptyBorder(14, 10, 14, 10)
                ));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                performQuickLogin(email, password);
            }
        });

        return card;
    }

    /**
     * Ejecuta login rápido al hacer clic en una tarjeta de usuario.
     */
    private void performQuickLogin(String email, String password) {
        String ip = txtIp.getText().trim();
        String portStr = txtPort.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debes completar la IP del servidor y el puerto antes de seleccionar un usuario.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            btnLogin.setEnabled(false);
            btnLogin.setText("Conectando...");
            // Rellenar campos visualmente para feedback
            txtCorreo.setText(email);
            txtPassword.setText(password);
            controller.performLogin(ip, port, email, password);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            enableLoginButton();
        }
    }

    private void onLoginClicked() {
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
            controller.performLogin(ip, port, correo, password);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            enableLoginButton();
        }
    }

    public void enableLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Conectar e Ingresar");
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(200, 200, 200));
        return lbl;
    }

    private String getInitials(String name) {
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        if (parts.length > 0 && !parts[0].isEmpty()) sb.append(parts[0].charAt(0));
        if (parts.length > 1 && !parts[1].isEmpty()) sb.append(parts[1].charAt(0));
        return sb.length() > 0 ? sb.toString().toUpperCase() : "?";
    }

    // ──────────────────────────────────────────────
    // Componente interno: Avatar circular con iniciales
    // ──────────────────────────────────────────────
    private static class AvatarLabel extends JComponent {
        private final String initials;
        private final Color bgColor;
        private final int size;

        public AvatarLabel(String initials, Color bgColor, int size) {
            this.initials = initials;
            this.bgColor = bgColor;
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Círculo de fondo
            g2.setColor(bgColor);
            g2.fill(new Ellipse2D.Double(0, 0, size, size));

            // Borde sutil
            g2.setColor(new Color(255, 255, 255, 40));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(1, 1, size - 2, size - 2));

            // Iniciales centradas
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, size / 3));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(initials);
            int textHeight = fm.getAscent();
            g2.drawString(initials, (size - textWidth) / 2, (size + textHeight) / 2 - 2);

            g2.dispose();
        }
    }
}
