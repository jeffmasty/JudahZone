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

import judahzone.gui.Gui;
import judahzone.util.Constants;

public class SheetMusicPnl extends JPanel {
        Image image;
        JLabel labelImage;
        File file;

        public SheetMusicPnl(File musicImage, Dimension sz) throws IOException {
        	this.file = musicImage;
            image = ImageIO.read(musicImage);
            labelImage = new SheetMusic(new ImageIcon(image));

            setLayout(new GridBagLayout());
            Dimension smaller = new Dimension(sz.width - 10, sz.height  - 20);
            Gui.resize(labelImage, smaller);

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(1, 1, 1, 1);
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridy = 1;
            c.gridx = 0;
            c.gridwidth = 3;
            add(labelImage, c);

            setLocation(0,0);
            setPreferredSize(sz);
            setMaximumSize(sz);
            setName(Constants.CUTE_NOTE + musicImage.getName());
            doLayout();
        }

        public void setImage(File musicImage) throws IOException {
            if (musicImage == null || !musicImage.isFile())
            	return;
        	image = ImageIO.read(musicImage);
            file = musicImage;
            labelImage.setIcon(new ImageIcon(image));
            setName(Constants.CUTE_NOTE + musicImage.getName());
        }

    }
