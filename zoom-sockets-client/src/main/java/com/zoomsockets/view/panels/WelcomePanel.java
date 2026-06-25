package com.zoomsockets.view.panels;

import com.zoomsockets.controller.MainController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class WelcomePanel extends JPanel {

    private JLabel lblWelcomeMsg;
    private JTextField txtCrearNombreSala, txtUnirseCodigoSala;
    private JButton btnCrearSala, btnUnirseSala;

    private JDialog waitingRoomDialog;
    private Timer spinnerTimer;

    public WelcomePanel(MainController controller) {
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
            controller.createRoom(nombre);
        });
        panelCrear.add(btnCrearSala, BorderLayout.SOUTH);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        formCard.add(panelCrear, gbc);

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

            controller.joinRoom(codigo);
        });
        panelUnirse.add(btnUnirseSala, BorderLayout.SOUTH);

        gbc.gridx = 1;
        formCard.add(panelUnirse, gbc);

        JButton btnLogout = new JButton("Cerrar Sesión");
        btnLogout.putClientProperty("JButton.buttonType", "roundRect");
        btnLogout.addActionListener(e -> controller.logout());
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(20, 10, 10, 10);
        formCard.add(btnLogout, gbc);

        add(formCard);
    }

    public void setWelcomeMessage(String name, String role) {
        lblWelcomeMsg.setText("Bienvenido, " + name + " (" + role + ")");
    }

    public void enableJoinButton() {
        btnUnirseSala.setEnabled(true);
        btnUnirseSala.setText("Unirse a la Reunión");
    }

    public void showWaitingRoomDialog(String roomName, MainController controller) {
        if (waitingRoomDialog != null && waitingRoomDialog.isVisible()) {
            waitingRoomDialog.dispose();
        }

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        waitingRoomDialog = new JDialog(parentWindow, "Sala de Espera", Dialog.ModalityType.MODELESS);
        waitingRoomDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitingRoomDialog.setSize(440, 360);
        waitingRoomDialog.setLocationRelativeTo(parentWindow);
        waitingRoomDialog.setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(new Color(20, 21, 24));
        content.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        JLabel lblIcon = new JLabel("\u23F3", JLabel.CENTER);
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        gbc.gridy = 0;
        content.add(lblIcon, gbc);

        JLabel lblTitle = new JLabel("Sala de Espera", JLabel.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(30, 144, 255));
        gbc.gridy = 1;
        content.add(lblTitle, gbc);

        JLabel lblSalaName = new JLabel(roomName, JLabel.CENTER);
        lblSalaName.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblSalaName.setForeground(new Color(150, 150, 150));
        gbc.gridy = 2;
        content.add(lblSalaName, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 0, 15, 0);
        content.add(sep, gbc);
        gbc.insets = new Insets(8, 0, 8, 0);

        JLabel lblMsg = new JLabel(
            "<html><div style='text-align:center;'>Aguarda mientras el anfitrión<br>" +
            "revisa tu solicitud de ingreso.</div></html>", JLabel.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMsg.setForeground(new Color(200, 200, 200));
        gbc.gridy = 4;
        content.add(lblMsg, gbc);

        String[] spinnerFrames = { "|", "/", "\u2014", "\\" };
        JLabel lblSpinner = new JLabel(spinnerFrames[0] + "  Esperando respuesta del host...", JLabel.CENTER);
        lblSpinner.setFont(new Font("Monospaced", Font.BOLD, 13));
        lblSpinner.setForeground(new Color(30, 144, 255));
        gbc.gridy = 5;
        content.add(lblSpinner, gbc);

        int[] frameIdx = { 0 };
        if (spinnerTimer != null) spinnerTimer.stop();
        spinnerTimer = new Timer(150, e -> {
            frameIdx[0] = (frameIdx[0] + 1) % spinnerFrames.length;
            lblSpinner.setText(spinnerFrames[frameIdx[0]] + "  Esperando respuesta del host...");
        });
        spinnerTimer.start();

        JButton btnCancel = new JButton("Cancelar solicitud");
        btnCancel.setBackground(new Color(178, 34, 34));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.putClientProperty("JButton.buttonType", "roundRect");
        btnCancel.addActionListener(e -> {
            controller.cancelWaitingRoom();
            closeWaitingRoomDialog();
        });
        gbc.gridy = 6;
        gbc.insets = new Insets(20, 0, 0, 0);
        content.add(btnCancel, gbc);

        waitingRoomDialog.setContentPane(content);
        waitingRoomDialog.setVisible(true);
    }

    public void closeWaitingRoomDialog() {
        if (spinnerTimer != null) {
            spinnerTimer.stop();
            spinnerTimer = null;
        }
        if (waitingRoomDialog != null) {
            waitingRoomDialog.dispose();
            waitingRoomDialog = null;
        }
    }
}
