package net.judah.effects.gui;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Reverb;
import net.judah.mixer.Channel;
import net.judah.util.JudahKnob;

public class Row extends JPanel {
	
	private final JPanel labels = new JPanel();
	private final JPanel controls = new JPanel();
	private final int row;
	@Getter private JudahKnob a, b, c, d;
	
	
	
	public Row(final Channel ch, final int idx) {

		row = idx;
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		labels.setLayout(new BoxLayout(labels, BoxLayout.X_AXIS));
		controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
		
		switch (idx) {
		case 0 : row0();
			a = new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Wet.ordinal(), Reverb.Settings.Wet.name());
			b = new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Room.ordinal(), Reverb.Settings.Room.name());
			c = new JudahKnob(ch, ch.getReverb(), Reverb.Settings.Damp.ordinal(), Reverb.Settings.Damp.name());
			d = new JudahKnob(ch, ch.getOverdrive(), 0, "Gain");
			break;
		case 1: row1();
			a = new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Rate.ordinal(), Chorus.Settings.Rate.name());
			b = new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Depth.ordinal(), Chorus.Settings.Depth.name());
			c = new JudahKnob(ch, ch.getChorus(), Chorus.Settings.Feedback.ordinal(), "F/B");
			d = new JudahKnob(ch, ch.getCutFilter(), CutFilter.Settings.Frequency.ordinal(), "Hz.");
			break;
		case 2: row2();
			a = new JudahKnob(ch, ch.getEq(), EQ.EqBand.Bass.ordinal(), EQ.EqBand.Bass.name());
			b = new JudahKnob(ch, ch.getEq(), EQ.EqBand.Mid.ordinal(), EQ.EqBand.Mid.name());
			c = new JudahKnob(ch, ch.getEq(), EQ.EqBand.High.ordinal(), EQ.EqBand.High.name());
			d = new JudahKnob(ch, ch.getDelay(), Delay.Settings.DelayTime.ordinal(), "Time");
			break;
		case 3: row3();
			a = new JudahKnob(ch, ch.getGain(), Gain.VOLUME, "");
			b = new JudahKnob(ch, ch.getGain(), Gain.PAN, "");
			c = new JudahKnob(ch, ch.getCompression(), Compression.Settings.Threshold.ordinal(), "Thold");
			d = new JudahKnob(ch, ch.getDelay(), Delay.Settings.Feedback.ordinal(), "F/B");
			break;
		}
		add(labels);
		if (a != null) controls.add(a);
		if (b != null) controls.add(b);
		if (c != null) controls.add(c);
		if (d != null) controls.add(d);
		add(controls);
		update();
	}

	private void row0() {

		JPanel atit = new JPanel();
		JPanel btit = new JPanel();
		JPanel ctit = new JPanel();
		JPanel dtit = new JPanel();
		atit.add(new JLabel(" "));
		btit.add(new JLabel("Reverb"));
		ctit.add(new JLabel(" "));
		dtit.add(new JLabel("O/Drive"));
		labels.add(atit);
		labels.add(btit);
		labels.add(ctit);
		labels.add(dtit);
		
	}
		
	private void row1() {
		JPanel atit = new JPanel();
		JPanel btit = new JPanel();
		JPanel ctit = new JPanel();
		JPanel dtit = new JPanel();
		atit.add(new JLabel(" "));
		btit.add(new JLabel("Chorus"));
		ctit.add(new JLabel(" "));
		dtit.add(new JLabel("pArTy"));
		labels.add(atit);
		labels.add(btit);
		labels.add(ctit);
		labels.add(dtit);
	}
	
	private void row2() {
		JPanel atit = new JPanel();
		JPanel btit = new JPanel();
		JPanel ctit = new JPanel();
		JPanel dtit = new JPanel();
		atit.add(new JLabel(" "));
		btit.add(new JLabel("EQ"));
		ctit.add(new JLabel(" "));
		dtit.add(new JLabel("Delay"));
		labels.add(atit);
		labels.add(btit);
		labels.add(ctit);
		labels.add(dtit);
	}
	
	private void row3() {
	JPanel atit = new JPanel();
		JPanel btit = new JPanel();
		JPanel ctit = new JPanel();
		JPanel dtit = new JPanel();
		atit.add(new JLabel("Vol."));
		btit.add(new JLabel("Pan"));
		ctit.add(new JLabel("Comp."));
		dtit.add(new JLabel(""));
		labels.add(atit);
		labels.add(btit);
		labels.add(ctit);
		labels.add(dtit);
	}

	public void update() {
		if (a != null) a.update();
		if (b != null) b.update();
		if (c != null) c.update();
		if (d != null) d.update();
		
		boolean on = (row == 0 || row == 1) ? 
				KnobMode.Effects1 == MPK.getMode() : 
				KnobMode.Effects2 == MPK.getMode(); 
		a.setOnMode(on);
		b.setOnMode(on);
		c.setOnMode(on);
		d.setOnMode(on);
		
	}
	
}
