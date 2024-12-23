package net.judah.gui.widgets;

import java.util.Vector;

import javax.swing.JComboBox;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.omni.Threads;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Cycle;

public class CycleCombo extends JComboBox<Cycle> {

	private static final Vector<CycleCombo> instances = new Vector<>();
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
		if (JudahZone.isInitialized())
		Threads.execute(()->{
			for (CycleCombo c : instances)
				if (c.track == t && c.getSelectedItem() != t.getCycle()) {
					c.setSelectedItem(t.getCycle());
					c.setToolTipText(t.getCycle().getTooltip());
				}
		});
	}

}
