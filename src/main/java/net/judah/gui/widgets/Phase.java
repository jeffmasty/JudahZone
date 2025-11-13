package net.judah.gui.widgets;

import javax.swing.JSlider;

import net.judah.fx.Chorus;
import net.judah.gui.Updateable;

public class Phase extends JSlider implements Updateable {

	private final Chorus ch;
	private static final int PHASE = Chorus.Settings.Phase.ordinal();

	public Phase(Chorus c) {
		ch = c;
		setValue(ch.get(PHASE));
		addChangeListener(e->ch.set(PHASE, getValue()));
		setToolTipText("Phase");
	}


	@Override public void update() {
		if (getValue() != ch.get(PHASE))
				setValue(ch.get(PHASE));
	}

}
