package com.zoomsockets.view;

import com.zoomsockets.controller.MainController;
import com.zoomsockets.view.panels.LoginPanel;
import com.zoomsockets.view.panels.RoomPanel;
import com.zoomsockets.view.panels.WelcomePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ClientAppFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);

    private LoginPanel loginPanel;
    private WelcomePanel welcomePanel;
    private RoomPanel roomPanel;

    private MainController controller;

    public ClientAppFrame(MainController controller) {
        super("Zoom-Sockets - Prototipo Académico MVC");
        this.controller = controller;
        this.controller.setFrame(this);
        setupUI();
    }

    private void setupUI() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.exitApp();
            }
        });

        loginPanel = new LoginPanel(controller);
        welcomePanel = new WelcomePanel(controller);
        roomPanel = new RoomPanel(controller);

        mainContainer.add(loginPanel, "LOGIN");
        mainContainer.add(welcomePanel, "WELCOME");
        mainContainer.add(roomPanel, "ROOM");

        add(mainContainer);
        cardLayout.show(mainContainer, "LOGIN");
    }

    public void showCard(String cardName) {
        cardLayout.show(mainContainer, cardName);
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    public WelcomePanel getWelcomePanel() {
        return welcomePanel;
    }

    public RoomPanel getRoomPanel() {
        return roomPanel;
    }
}
