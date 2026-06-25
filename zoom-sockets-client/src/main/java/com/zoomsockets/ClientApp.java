package com.zoomsockets;

import com.formdev.flatlaf.FlatDarkLaf;
import com.zoomsockets.controller.MainController;
import com.zoomsockets.view.ClientAppFrame;

import javax.swing.*;

public class ClientApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Aplicar Look & Feel Moderno Dark (FlatLaf)
                FlatDarkLaf.setup();
                
                // Inicializar Controlador principal (implementa lógica y listener de red)
                MainController controller = new MainController();
                
                // Inicializar Vista principal (Frame + Paneles)
                ClientAppFrame appFrame = new ClientAppFrame(controller);
                appFrame.setVisible(true);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
