package net.judah.gui.settable;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.judah.fx.LFO;
import net.judah.fx.LFO.Target;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;

public class LfoCombo extends SetCombo<Target> implements Updateable {

	private final LFO lfo;

	public LfoCombo(LFO lfo) {
		super(LFO.Target.values(), lfo.getTarget());
		this.lfo = lfo;
		setSelectedItem(lfo.getTarget());
		Gui.resize(this, Size.SMALLER_COMBO);
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
