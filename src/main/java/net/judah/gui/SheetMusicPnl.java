package net.judah.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.util.Constants;

public class SheetMusicPnl extends JPanel {
        Image image;
        JLabel labelImage;
        @Getter File file;
        
        public SheetMusicPnl(File musicImage, Dimension sz) throws IOException {
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
            Dimension smaller = new Dimension(sz.width - 10, sz.height  - 20);
            
            labelImage.setPreferredSize(smaller);
            labelImage.setMaximumSize(smaller);
            add(Gui.wrap(labelImage), constraints);
            setLocation(0,0);
            setSize(smaller);
            setMaximumSize(smaller);
            setName(Constants.CUTE_NOTE + musicImage.getName());
            doLayout();
        }
        
        public void setImage(File musicImage) throws IOException {
            image = ImageIO.read(musicImage);
            file = musicImage;
            labelImage.setIcon(new ImageIcon(image));
            setName(Constants.CUTE_NOTE + musicImage.getName());
        }
        
    }
