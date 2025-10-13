package net.judah.mixer;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.RainbowFader;
import net.judah.looper.Loop;
import net.judah.omni.AudioTools;

/** listen to channel's audio, paint widget height/color based on RMS of the audio frame */
public class RMSIndicator extends JPanel implements Updateable {

    // Boost RMS into color/pixels range, todo see AudioTools.linearToDecibal
	private static final float FACTOR = 23;
    private static final float MAINS = 3.5f;
    private static final float LOOP = 14;

    private static final int WIDTH = 12;
	private static final int HALF = WIDTH / 2;
	private static final int HEIGHT = Size.FADER_SIZE.height;

	private final Channel ch;
	private float left, right; // computed gain

	public RMSIndicator(Channel ch) {
		this.ch = ch;
		Gui.resize(this, new Dimension(WIDTH, HEIGHT));
		setBorder(Gui.SUBTLE);
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
	}

	@Override
	public void update() {
		float factor = ch instanceof Loop ? LOOP : ch instanceof Mains ? MAINS : FACTOR;
		left = AudioTools.rms(ch.getLeft().array()) * factor;
		right = AudioTools.rms(ch.getRight().array()) * factor;
		repaint();
	}

}
