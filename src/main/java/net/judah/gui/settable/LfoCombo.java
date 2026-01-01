package net.judah.gui.settable;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import net.judah.gui.Size;
import net.judah.midi.LFO;
import net.judah.midi.LFO.Target;

public class LfoCombo extends SetCombo<Target> implements Updateable {
	private final LFO lfo;

	public LfoCombo(LFO lfo) {
		super(LFO.Target.values(), lfo.getTarget());
		this.lfo = lfo;
		setSelectedItem(lfo.getTarget());
		Gui.resize(this, Size.MEDIUM);
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
	}

	@Override
	protected void action() {
		lfo.setTarget((Target)getSelectedItem());
	}

	@Override
	public void update() {
		if (set == this)
			return;
		if (lfo.getTarget() != getSelectedItem())
			override(lfo.getTarget());
	}

}
