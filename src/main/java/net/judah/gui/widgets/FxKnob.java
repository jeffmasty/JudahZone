package net.judah.gui.widgets;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.api.Effect;
import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.fx.Reverb;
import net.judah.gui.Bindings;
import net.judah.gui.HQ;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.seq.MidiConstants;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Pastels;
import net.judahzone.gui.Updateable;

/** a knob and label, paints itself on update when effect is active */
public class FxKnob extends JPanel implements Updateable {

	public static final Color THUMB = Pastels.BLUE;

	protected final Channel ch;
	@Getter protected Effect effect;
	protected final int idx; // main knob target
	protected int shiftIdx = -1; // alternate target
	protected boolean tholdHi = false; // most effects activate on low threshold

	protected final Knob knob;
	protected final JLabel label;

	public FxKnob(Channel ch, Effect fx, int idx, String lbl, int alt, boolean hi) {
		this(ch, fx, idx, lbl, THUMB, alt);
		tholdHi = hi;
	}

	public FxKnob(Channel ch, Effect fx, int idx, String lbl, int alt) {
		this(ch, fx, idx, lbl, THUMB);
		shiftIdx = alt;
	}
	public FxKnob(Channel ch, Effect fx, int idx, String lbl, Color highlight, int alt) {
		this(ch, fx, idx, lbl, highlight);
		shiftIdx = alt;
	}

	public FxKnob(Channel ch, int idx, String lbl, int alt) { // custom reverb
		this(ch, idx, lbl);
		shiftIdx = alt;
	}

	public FxKnob(Channel ch, Effect fx, int idx, String lbl, boolean hi) {
		this(ch, fx, idx, lbl, THUMB);
		tholdHi = hi;
	}

	public FxKnob(Channel ch, Effect fx, int idx, String lbl) {
		this(ch, fx, idx, lbl, THUMB);
	}

	public FxKnob(Channel ch, Effect fx, int idx, String lbl, Color highlight) {
		this.effect = fx;
		this.ch = ch;
		this.idx = idx;
		knob = new Knob(value-> {
			effect.set(idx, value);
			MainFrame.update(ch);});
		knob.setKnobColor(highlight);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}

	public FxKnob(Channel ch, int idx, JLabel lbl, Knob knob) {
		effect = null;
		this.ch = ch;
		this.idx = idx;
		this.knob = knob;
		this.label = lbl;
		add(knob);

		label.setFont(Gui.FONT11);
		add(label);
	}

	// inelegant external reverb
	public FxKnob(Channel ch, int idx, String lbl) {
		effect = null;
		this.ch = ch;
		this.idx = idx;
		knob = new Knob(value -> {
				ch.getReverb().set(idx, value);
				MainFrame.update(ch);
			});
		knob.setKnobColor(Pastels.BLUE);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}

	@Override public void update() {
		if (effect == null) {
			if (knob.getValue() != ch.getReverb().get(idx))
				knob.setValue(ch.getReverb().get(idx));
		}
		else if (knob instanceof Updateable update)
			update.update();
		else if (knob.getValue() != effect.get(idx))
				knob.setValue(effect.get(idx));

		Color bg = effect == null ? ch.isActive(ch.getReverb()) ? Bindings.getFx(Reverb.class) : null :
				ch.isActive(effect) ? Bindings.getFx(effect.getClass()) : null;
		setBackground(bg);
		label.setBackground(bg);
		knob.setBackground(bg);
	}

	public void knob(boolean up) {
		if (effect == null)
			effect = ch.getReverb();

		if (shiftProcessing())
			effect.set(shiftIdx, offset(effect.get(shiftIdx), up));

		else {
			int criteria = offset(effect.get(idx), up);
			effect.set(idx, criteria);
			if (effect instanceof Gain)
				return;// volume/pan always on
			// threshold activation
			ch.setActive(effect, tholdHi ? criteria < MidiConstants.THOLD_HI : criteria > MidiConstants.THOLD_LO);
		}
	}

	public static final int OFFSET = 2;
    public static int offset(int val, boolean up) {
    	val += up ? OFFSET : -OFFSET;
    	if (val > 100) val = 100;
    	if (val < 0) val = 0;
    	return val;
    }

    boolean shiftProcessing() {
    	return HQ.isShift() && shiftIdx >= 0;
    }

}
