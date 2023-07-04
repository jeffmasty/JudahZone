package net.judah.gui.widgets;

import java.util.HashSet;

import javax.swing.JComboBox;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Cycle;
import net.judah.util.Constants;

public class CycleCombo extends JComboBox<Cycle> {
	
	private static final HashSet<CycleCombo> instances = new HashSet<>();
	private final Computer track;
	
	public CycleCombo(Computer track) {
		super(Cycle.values());
		this.track = track;
		Gui.resize(this, Size.SMALLER_COMBO);
		instances.add(this);
		update(track);
		addActionListener(e->action());
	}

	protected void action() {
		Cycle c = (Cycle)getSelectedItem();
		if (track.getCycle() != c)
			track.setCycle(c);
	}
	
	public static void update(Computer t) {
		Constants.execute(()->{
			for (CycleCombo c : instances)
				if (c.track == t && c.getSelectedItem() != t.getCycle()) {
					c.setSelectedItem(t.getCycle());
					c.setToolTipText(t.getCycle().getTooltip());
				}
		});
	}
	
}
