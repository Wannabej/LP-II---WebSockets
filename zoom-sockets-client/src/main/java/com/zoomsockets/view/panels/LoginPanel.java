package com.zoomsockets.view.panels;

import com.zoomsockets.controller.MainController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginPanel extends JPanel {

    private MainController controller;
    private JTextField txtIp, txtPort, txtCorreo;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public LoginPanel(MainController controller) {
        this.controller = controller;
        setLayout(new GridBagLayout());
        setBackground(new Color(20, 21, 24));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 52, 58), 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));
        formCard.setBackground(new Color(32, 33, 37));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitle = new JLabel("Iniciar Sesión", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(240, 240, 240));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblTitle, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = 1; gbc.insets = new Insets(5, 8, 15, 8);
        formCard.add(sep, gbc);

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
        txtCorreo = new JTextField("host@zoom.com", 12);
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

        btnLogin = new JButton("Conectar e Ingresar");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(30, 144, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.putClientProperty("JButton.buttonType", "roundRect");
        btnLogin.addActionListener(e -> onLoginClicked());
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 8, 8);
        formCard.add(btnLogin, gbc);

        JLabel lblHelp = new JLabel("Semillas: host@zoom.com, juan@zoom.com, maria@zoom.com (clave: password123)", JLabel.CENTER);
        lblHelp.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblHelp.setForeground(Color.GRAY);
        gbc.gridy = 7; gbc.insets = new Insets(10, 8, 0, 8);
        formCard.add(lblHelp, gbc);

        add(formCard);
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
}
