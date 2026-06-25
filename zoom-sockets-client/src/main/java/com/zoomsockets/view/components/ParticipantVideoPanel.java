package com.zoomsockets.view.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class ParticipantVideoPanel extends JPanel {
    private final String userName;
    private final JLabel labelName;
    private final JLabel labelVideo;
    private final JPanel panelAvatar;

    public ParticipantVideoPanel(String userName) {
        this.userName = userName;
        this.setLayout(new BorderLayout());
        this.setBackground(new Color(30, 32, 40));
        this.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 2, true));

        labelName = new JLabel("  " + userName, JLabel.LEFT);
        labelName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelName.setForeground(Color.WHITE);
        labelName.setPreferredSize(new Dimension(0, 24));
        labelName.setOpaque(true);
        labelName.setBackground(new Color(0, 0, 0, 150));
        this.add(labelName, BorderLayout.SOUTH);

        panelAvatar = new JPanel(new GridBagLayout());
        panelAvatar.setBackground(new Color(25, 27, 34));
        
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
        labelAvatarText.setBackground(new Color(30, 144, 255));
        labelAvatarText.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true));
        
        panelAvatar.add(labelAvatarText);
        this.add(panelAvatar, BorderLayout.CENTER);

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
                int w = this.getWidth() > 0 ? this.getWidth() : 320;
                int h = this.getHeight() > 0 ? this.getHeight() - 24 : 216; 
                
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
