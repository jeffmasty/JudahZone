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
	private static final int WIDTH = 14;
	private static final int HALF = WIDTH / 2;
	private static final int HEIGHT = Size.FADER_SIZE.height;
    private static final float FACTOR = 6f;

	private final Channel ch;
	private float left, right;

	public VolLED(Channel ch) {
		this.ch = ch;
		Gui.resize(this, new Dimension(WIDTH, HEIGHT));
		setBorder(Gui.SUBTLE);
	}

	public static float compute(float[] data) {
        float big = 0f;
        for (float value : data) {
            if (value > big)
                big = value;
        }
		return FACTOR * big;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int h = (int)(HEIGHT * left);
		g.setColor(RainbowFader.chaseTheRainbow(left));
		g.fillRect(1, HEIGHT - h, HALF, h);
		h = (int)(HEIGHT * right);
		g.setColor(RainbowFader.chaseTheRainbow(right));
		g.fillRect(HALF, HEIGHT - h, WIDTH - 1, h);
		//	else { // MONO
		//		int h = (int)(HEIGHT * left);
		//		g.setColor(RainbowFader.chaseTheRainbow(left));
		//		g.fillRect(PAD, HEIGHT - h, WIDTH - PAD, h);
	}

	@Override
	public void update() {
		left = compute(ch.getLeft().array());
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
