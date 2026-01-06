package net.judah.gui.widgets;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.api.FX;
import judahzone.api.MidiConstants;
import judahzone.fx.Gain;
import judahzone.fx.Reverb;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.widgets.Knob;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.Bindings;
import net.judah.gui.HQ;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FXAware;

/** a knob and label, paints itself on update when effect is active */
public class FxKnob extends JPanel implements Updateable, FXAware {

	public static final Color THUMB = Pastels.BLUE;

	protected final Channel ch;
	@Getter protected FX fx;
	protected final int idx; // main knob target
	protected int shiftIdx = -1; // alternate target
	protected boolean tholdHi = false; // most effects activate on low threshold

	protected final Knob knob;
	protected final JLabel label;

	public FxKnob(Channel ch, FX fx, int idx, String lbl, int alt, boolean hi) {
		this(ch, fx, idx, lbl, THUMB, alt);
		tholdHi = hi;
	}

	public FxKnob(Channel ch, FX fx, int idx, String lbl, int alt) {
		this(ch, fx, idx, lbl, THUMB);
		shiftIdx = alt;
	}
	public FxKnob(Channel ch, FX fx, int idx, String lbl, Color highlight, int alt) {
		this(ch, fx, idx, lbl, highlight);
		shiftIdx = alt;
	}

	public FxKnob(Channel ch, int idx, String lbl, int alt) { // custom reverb
		this(ch, idx, lbl);
		shiftIdx = alt;
	}

	public FxKnob(Channel ch, FX fx, int idx, String lbl, boolean hi) {
		this(ch, fx, idx, lbl, THUMB);
		tholdHi = hi;
	}

	public FxKnob(Channel ch, FX fx, int idx, String lbl) {
		this(ch, fx, idx, lbl, THUMB);
	}

	public FxKnob(Channel ch, FX fx, int idx, String lbl, Color highlight) {
		this.fx = fx;
		this.ch = ch;
		this.idx = idx;
		knob = new Knob(value-> {
			fx.set(idx, value);
			MainFrame.update(ch);});
//		knob.setKnobColor(highlight);
		add(knob);

		label = new JLabel(lbl, JLabel.LEFT);
		label.setFont(Gui.FONT11);
		add(label);
	}

	public FxKnob(Channel ch, int idx, JLabel lbl, Knob knob) {
		fx = null;
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
		fx = null;
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
		if (fx == null) {
			if (knob.getValue() != ch.getReverb().get(idx))
				knob.setValue(ch.getReverb().get(idx));
		}
		else if (knob instanceof Updateable update)
			update.update();
		else if (knob.getValue() != fx.get(idx))
				knob.setValue(fx.get(idx));

		Color bg = fx == null ? ch.isActive(ch.getReverb()) ? Bindings.getFx(Reverb.class) : null :
				ch.isActive(fx) ? Bindings.getFx(fx.getClass()) : null;
		setBackground(bg);
		label.setBackground(bg);
		knob.setBackground(bg);
	}

	public void knob(boolean up) {
		if (fx == null)
			fx = ch.getReverb();

		if (shiftProcessing())
			fx.set(shiftIdx, offset(fx.get(shiftIdx), up));

		else {
			int criteria = offset(fx.get(idx), up);
			fx.set(idx, criteria);
			if (fx instanceof Gain)
				return;// volume/pan always on
			// threshold activation
			ch.setActive(fx, tholdHi ? criteria < MidiConstants.THOLD_HI : criteria > MidiConstants.THOLD_LO);
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
