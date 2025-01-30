package net.judah.mixer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Delay;
import net.judah.fx.Effect;
import net.judah.fx.EffectColor;
import net.judah.fx.Filter;
import net.judah.fx.LFO;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;


/**effect indicator lights: red reverb, orange delay, yellow distortion, green chorus, blue LFO
 * @author judah */
public class FxLEDs extends JPanel {

	private final int UNITS = 7;
	private final Channel channel;

	public static final int REVERB = 0;
	public static final int DELAY = 1;
	public static final int OVERDRIVE = 2;
	public static final int CHORUS = 3;
	public static final int CUTFILTER = 4;
	public static final int COMPRESSION = 5;
	public static final int LFO = 6;
	public static final int LFO2 = 7;
	@SuppressWarnings("unchecked")
	private Class<Effect>[] lookup = new Class[] {Reverb.class, Delay.class, Overdrive.class,
			Chorus.class, Filter.class, Compressor.class, LFO.class};


	@Getter boolean[] model = new boolean[UNITS];

	public FxLEDs(Channel channel) {
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
			g.setColor(EffectColor.get(lookup[i]));
			int x = (int)(i * unit);
			g.fillRect(x, 1, (int)Math.ceil(unit), d.height - 2);
		}
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
		if (channel.getLfo().isActive() || channel.getLfo2().isActive() != model[LFO]) {
			model[LFO] = channel.getLfo().isActive() || channel.getLfo2().isActive();
			repaint = true;
		}
		if (channel.getFilter1().isActive() != model[CUTFILTER]) {
			model[CUTFILTER] = channel.getFilter1().isActive();
			repaint = true;
		}
		if (channel.getCompression().isActive() != model[COMPRESSION]) {
			model[COMPRESSION] = channel.getCompression().isActive();
			repaint = true;
		}
		if (repaint)
			repaint();
	}

}
