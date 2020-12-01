package net.judah.sequencer;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import net.judah.util.Constants;

public class SeqWidget extends JPanel {

	public static final int INSET = 15;
	public static final int WIDTH = 800 + 2 * INSET;
	public static final int HEIGHT = 200 + 2 * INSET;
	
	// JButton start, previous, next, end, play, pause, settings;
//	JTextField current, tempo;
	// JSlider loop;
	// ~/git/Swing-range-slider ---> https://ernienotes.wordpress.com/2010/12/27/creating-a-java-swing-range-slider/
	// https://www.infoworld.com/article/2071315/jslider-appearance-improvements.html
	
	public SeqWidget() {
		setSize(600, 220);
		setBorder(Constants.Gui.GRAY1);
	}
	
	
	private Graphics2D g2d; 
    @Override public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g2d = (Graphics2D) g;

        g2d.drawLine(30, 30, 270, 170);
        g2d.drawLine(270, 30, 30, 170);
        g2d.drawLine(30, 30, 270, 30);
        g2d.drawLine(30, 170, 270, 170);
        
        g2d.drawLine(330, 30, 570, 170);
        g2d.drawLine(570, 30, 330, 170);
        g2d.drawLine(330, 30, 570, 30);
        g2d.drawLine(330, 170, 570, 170);
    }


	
	
}
