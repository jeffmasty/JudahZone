package net.judah.util;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.judah.song.SheetMusic;
 
/**
 * This is a test program that draws an image provided by the user and scales
 * the image to fit its parent container (a JLabel).
 *
 * @author www.codejava.net
 *
 */
public class SheetMusicTest extends JFrame implements ActionListener {
     
    // private JLabel labelImgFilePath = new JLabel("Enter Image File Path: ");
    // private JTextField fieldImgFilePath = new JTextField(20);
    // private JButton buttonDisplay = new JButton("Display");
     
    private JLabel labelImage = new SheetMusic();
     
    public SheetMusicTest(File file) {
    	this();
    	try {
	        Image image = ImageIO.read(file);
	        labelImage.setIcon(new ImageIcon(image));
	    } catch (Exception e) {
	        Console.warn(e);
	    }      

    }
    
    public SheetMusicTest() {
        super("Image Frame Demo");
         
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(3, 3, 3, 3);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTHWEST;
         
//        constraints.gridy = 0;
//        constraints.gridx = 0;
//        add(labelImgFilePath, constraints);
//         
//        constraints.fill = GridBagConstraints.HORIZONTAL;
//        constraints.weightx = 1.0;
//         
//        constraints.gridx = 1;
//        add(fieldImgFilePath, constraints);
//         
//        constraints.gridx = 2;
//        constraints.fill = GridBagConstraints.NONE;
//        constraints.weightx = 0.0;
//        add(buttonDisplay, constraints);
         
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
         
        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        
        labelImage.setPreferredSize(new Dimension(400, 300));
        add(labelImage, constraints);
 
//         buttonDisplay.addActionListener(this);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }
     
    @Override
    public void actionPerformed(ActionEvent event) {
//        String filePath = fieldImgFilePath.getText();
//        try {
//            Image image = ImageIO.read(new File(filePath));
//            labelImage.setIcon(new ImageIcon(image));
//             
//        } catch (Exception ex) {
//            System.err.println(ex);
//        }      
    }
     
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(new Runnable() {
//             
//            @Override
//            public void run() {
//                new SheetMusicTest().setVisible(true);
//            }
//        });
//    }
}
