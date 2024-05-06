package net.judah.mixer;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.RainbowFader;
import net.judah.looper.Loop;

/** listen to channel audio, paint widget height/color based on loudest sample */
public class VolLED extends JPanel implements Updateable {
	static final int WIDTH = 14;
	static final int HALF = WIDTH / 2;
	static final int HEIGHT = Size.FADER_SIZE.height;
	static final Dimension SIZE = new Dimension(WIDTH, HEIGHT);
	static final int PAD = 3;
    private static final float FACTOR = 6f;
	
	private final Channel ch;
	private float left, right;
	
	public VolLED(Channel ch) {
		this.ch = ch;
		Gui.resize(this, SIZE);
		setBorder(Gui.SUBTLE);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		if (ch.isStereo()) {
			int h = (int)(HEIGHT * left);
			g.setColor(RainbowFader.chaseTheRainbow(left));
			g.fillRect(1, HEIGHT - h, HALF, h);
			h = (int)(HEIGHT * right);
			g.setColor(RainbowFader.chaseTheRainbow(right));
			g.fillRect(HALF, HEIGHT - h, WIDTH - 1, h);
			
		}
		else {
			int h = (int)(HEIGHT * left);
			g.setColor(RainbowFader.chaseTheRainbow(left));
			g.fillRect(PAD, HEIGHT - h, WIDTH - PAD, h);
		}
	}

	public static float compute(float[] data) {
		float big = 0f;
		for (int i = 0; i < data.length; i++) {
			// TODO normalize ~0f?
			if (data[i] > big)
				big = data[i];
		}
		return FACTOR * big;
	}
	
	@Override
	public void update() {
		left = compute(ch.getLeft().array());
		if (ch.isStereo()) 
			right = compute(ch.getRight().array());
		if (ch instanceof Loop) {
			left *= 1.5f;
			right *= 1.5f;
		} else if (ch == JudahZone.getMains()) {
			left *= 0.275f;
			right *= 0.275f;
		}
		repaint();
	}

}
