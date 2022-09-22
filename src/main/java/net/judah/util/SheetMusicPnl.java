package net.judah.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;

public class SheetMusicPnl extends JPanel {
        Image image;
        JLabel labelImage;
        @Getter File file;
        
        public SheetMusicPnl(File musicImage, Rectangle sz) throws IOException {
        	this.file = musicImage;
            image = ImageIO.read(musicImage);
            labelImage = new SheetMusic(new ImageIcon(image));

            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;

            constraints.gridy = 1;
            constraints.gridx = 0;
            constraints.gridwidth = 3;
            Dimension smaller = new Dimension(sz.width - 10, sz.height  - 10);
            
            labelImage.setPreferredSize(smaller);
            add(labelImage, constraints);
            setSize(smaller);
            setName(musicImage.getName());
            doLayout();
        }
        
        public void setImage(File musicImage) throws IOException {
            image = ImageIO.read(musicImage);
            labelImage.setIcon(new ImageIcon(image));
            setName(musicImage.getName());
        }
        
    }
