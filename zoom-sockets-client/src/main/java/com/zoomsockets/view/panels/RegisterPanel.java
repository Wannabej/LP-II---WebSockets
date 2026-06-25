package com.zoomsockets.view.panels;

import com.zoomsockets.controller.MainController;
import com.zoomsockets.model.ClientSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterPanel extends JPanel {

    private MainController controller;
    private JTextField txtIp, txtPort, txtNombres, txtCorreo;
    private JPasswordField txtPassword;
    private JComboBox<String> cbRol;
    private JButton btnRegister;
    private JButton btnBack;

    public RegisterPanel(MainController controller) {
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
        JLabel lblTitle = new JLabel("Crear Nueva Cuenta", JLabel.CENTER);
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

        // ── Separador ──
        JSeparator sep2 = new JSeparator();
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 8, 8, 8);
        formCard.add(sep2, gbc);
        
        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 8, 6, 8);

        // ── Datos del nuevo usuario ──
        gbc.gridx = 0; gbc.gridy = 5;
        formCard.add(createLabel("Nombres:"), gbc);
        txtNombres = new JTextField("", 12);
        txtNombres.putClientProperty("JTextField.placeholderText", "Tu Nombre");
        txtNombres.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtNombres, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        formCard.add(createLabel("Correo:"), gbc);
        txtCorreo = new JTextField("", 12);
        txtCorreo.putClientProperty("JTextField.placeholderText", "correo@ejemplo.com");
        txtCorreo.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtCorreo, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        formCard.add(createLabel("Contraseña:"), gbc);
        txtPassword = new JPasswordField("", 12);
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña");
        txtPassword.putClientProperty("JTextField.showClearButton", true);
        gbc.gridx = 1;
        formCard.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        formCard.add(createLabel("Rol:"), gbc);
        cbRol = new JComboBox<>(new String[]{"Estudiante", "Docente"});
        gbc.gridx = 1;
        formCard.add(cbRol, gbc);

        // ── Botones ──
        btnRegister = new JButton("Registrar");
        btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRegister.setBackground(new Color(46, 139, 87));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.putClientProperty("JButton.buttonType", "roundRect");
        btnRegister.addActionListener(e -> onRegisterClicked());
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 8, 6, 8);
        formCard.add(btnRegister, gbc);

        btnBack = new JButton("Volver al Login");
        btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnBack.putClientProperty("JButton.buttonType", "roundRect");
        btnBack.addActionListener(e -> {
            clearForm();
            // Desconectarse por si hubo algun intento de conexion
            ClientSession.getInstance().limpiarSesion();
            controller.logout(); 
        });
        gbc.gridy = 10; gbc.insets = new Insets(4, 8, 6, 8);
        formCard.add(btnBack, gbc);

        add(formCard);
    }

    private void onRegisterClicked() {
        String ip = txtIp.getText().trim();
        String portStr = txtPort.getText().trim();
        String nombres = txtNombres.getText().trim();
        String correo = txtCorreo.getText().trim();
        String password = new String(txtPassword.getPassword());
        String rol = (String) cbRol.getSelectedItem();

        if (ip.isEmpty() || portStr.isEmpty() || nombres.isEmpty() || correo.isEmpty() || password.isEmpty() || rol == null) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            btnRegister.setEnabled(false);
            btnRegister.setText("Registrando...");
            controller.performRegister(ip, port, nombres, correo, password, rol);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error", JOptionPane.ERROR_MESSAGE);
            enableRegisterButton();
        }
    }

    public void enableRegisterButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Registrar");
    }

    public void clearForm() {
        txtNombres.setText("");
        txtCorreo.setText("");
        txtPassword.setText("");
        cbRol.setSelectedIndex(0);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(200, 200, 200));
        return lbl;
    }
}
