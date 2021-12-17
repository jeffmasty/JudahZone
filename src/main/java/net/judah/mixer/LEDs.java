package net.judah.mixer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.security.InvalidParameterException;

import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.effects.Chorus;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EffectColor;
import net.judah.effects.LFO;
import net.judah.effects.Overdrive;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Reverb;


/**effect indicator lights: red reverb, orange delay, yellow distortion, green chorus, blue LFO
 * @author judah */
public class LEDs extends JPanel {

	private final int UNITS = 6;
	private final Channel channel;
	
	public static final int REVERB = 5;
	public static final int DELAY = 4;
	public static final int OVERDRIVE = 3;
	public static final int CHORUS = 2;
	public static final int LFO = 1;
	public static final int CUTFILTER = 0;
	
	@Getter boolean[] model = new boolean[UNITS];
	
	public LEDs(Channel channel) { 
		this.channel = channel;
		add(new JLabel());
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		final Dimension d = getSize();
		float unit = d.width / (float)UNITS;
		g.setColor(Color.GRAY);
		g.drawLine(0, 0, d.width, 0);
		g.drawLine(0, d.height - 1, d.width, d.height - 1);
		
		for (int i = 0; i < UNITS; i++) {
			if (!model[i]) continue;
			g.setColor(EffectColor.get(lookup(i)));
			int x = (int)(i * unit);
			g.fillRect(x, 1, (int)Math.ceil(unit), d.height - 2);
		}
	}

	private Class<? extends Effect> lookup(int i) {
		if (i == REVERB)
			return Reverb.class;
		if (i == DELAY)
			return Delay.class;
		if (i == OVERDRIVE)
			return Overdrive.class;
		if (i == CHORUS)
			return Chorus.class;
		if (i == LFO)
			return LFO.class;
		if (i == CUTFILTER)
			return CutFilter.class;
		throw new InvalidParameterException("class idx: " + i);
	}

	public void sync() {
		boolean repaint = false;
		if (channel.getReverb().isActive() != model[REVERB]) {
			model[REVERB] = channel.getReverb().isActive();
			repaint = true;
		}
		if (channel.getDelay().isActive() != model[DELAY]) {
			model[DELAY] = channel.getDelay().isActive();
			repaint = true;
		}
		if (channel.getOverdrive().isActive() != model[OVERDRIVE]) {
			model[OVERDRIVE] = channel.getOverdrive().isActive();
			repaint = true;
		}
		if (channel.getChorus().isActive() != model[CHORUS]) {
			model[CHORUS] = channel.getChorus().isActive();
			repaint = true;
		}
		if (channel.getLfo().isActive() != model[LFO]) {
			model[LFO] = channel.getLfo().isActive();
			repaint = true;
		}
		if (channel.getCutFilter().isActive() != model[CUTFILTER]) {
			model[CUTFILTER] = channel.getCutFilter().isActive();
			repaint = true;
		}
		if (repaint)
			repaint();
	}
	
}
