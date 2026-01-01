package net.judah.gui.widgets;

import javax.swing.JComboBox;

import judahzone.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Cycle;

public class CycleCombo extends JComboBox<Cycle> {

	private final Computer track;

	public CycleCombo(Computer track) {
		super(Cycle.values());
		this.track = track;
		setSelectedItem(track.getCycle());
		Gui.resize(this, Size.SMALLER);
		addActionListener(e->action());
	}

	protected void action() {
		Cycle c = (Cycle)getSelectedItem();
		if (track.getCycle() != c)
			track.setCycle(c);
	}

	public void update() {
		if (getSelectedItem() != track.getCycle()) {
			setSelectedItem(track.getCycle());
			setToolTipText(track.getCycle().getTooltip());
		}
	}

}
