package net.judah.scope;

import java.awt.Dimension;
import java.awt.Graphics;
import java.nio.FloatBuffer;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.gui.Pastels;
import net.judah.gui.widgets.RainbowFader;
import net.judah.util.Constants;

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
	
	public static float abs2(FloatBuffer buf) {
		float result = Float.MIN_VALUE;
		buf.rewind();
		for(int i = 0; i < Constants.bufSize(); i++) {
			float f = Math.abs(buf.get());
			if (f > result)
				result = f;
		}
		return result;
	}

	
}
