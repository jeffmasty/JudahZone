package net.judah.mixer;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.gui.Pastels;
import net.judah.widgets.RainbowFader;

public class Decibels extends JPanel {

	private final int height;
	private final int width = 6;
	private float db;
	
	public Decibels(int height) {
		this.height = height;
		Dimension size = new Dimension(width, height);
        setPreferredSize(size); 
        setMinimumSize(size);
        setMaximumSize(size);
		setBorder(new LineBorder(Pastels.MY_GRAY, 1));
	}

	
	public void setDb(float vol) {
		db = vol;
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int h = (int)(height * db);
		g.setColor(RainbowFader.chaseTheRainbow(db));
		g.fillRect(0, height - h, 6, h);
	}
	
}
