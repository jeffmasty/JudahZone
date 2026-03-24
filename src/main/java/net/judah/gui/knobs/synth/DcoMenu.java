package net.judah.gui.knobs.synth;

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import judahzone.data.Shape;
import judahzone.synth.Dco.DcoJson;
import judahzone.synth.WavSynth;

/** Shared context menu logic for DCO add/remove operations. */
public class DcoMenu {

	private final WavSynth synth;
	private final Component parent;
	private final int barIndex;

	/**
	 * @param synth the synth to modify
	 * @param parent the component to show the menu relative to
	 * @param barIndex the index of the DCO bar being right-clicked (-1 for empty space / no specific bar)
	 */
	public DcoMenu(WavSynth synth, Component parent, int barIndex) {
		this.synth = synth;
		this.parent = parent;
		this.barIndex = barIndex;
	}

	public void show(int x, int y) {
		JPopupMenu menu = new JPopupMenu();
		DcoJson[] algo = synth.getAlgo();
		int len = algo.length;

		// Remove option only if we have a valid bar index and more than one DCO
		if (barIndex >= 0 && barIndex < len && len > 1) {
			JMenuItem remove = new JMenuItem("Remove DCO");
			remove.addActionListener(a -> removeDco());
			menu.add(remove);
		}

		// Add option if we haven't hit the max
		JMenuItem add = new JMenuItem("Add DCO");
		add.setEnabled(len < WavSynth.MAX_DCO);
		add.addActionListener(a -> addDco());
		menu.add(add);

		menu.show(parent, x, y);
	}

	private void removeDco() {
		DcoJson[] algo = synth.getAlgo();
		int len = algo.length;
		if (len <= 1) {
			JOptionPane.showMessageDialog(parent, "Cannot remove the last oscillator.",
				"Remove DCO", JOptionPane.WARNING_MESSAGE);
			return;
		}
		DcoJson[] next = new DcoJson[len - 1];
		int write = 0;
		for (int i = 0; i < len; i++) {
			if (i == barIndex) continue;
			next[write++] = algo[i];
		}
		synth.setAlgo(next);
	}

	private void addDco() {
	    DcoJson[] algo = synth.getAlgo();
	    int len = algo.length;
	    if (len >= WavSynth.MAX_DCO) {
	        JOptionPane.showMessageDialog(parent, "Maximum number of oscillators reached.",
	            "Add DCO", JOptionPane.WARNING_MESSAGE);
	        return;
	    }
	    int insertPos = (barIndex >= 0) ? barIndex + 1 : len;
	    DcoJson[] next = new DcoJson[len + 1];
	    for (int src = 0, dst = 0; dst < next.length; dst++) {
	        if (dst == insertPos)
	            next[dst] = new DcoJson(Shape.SIN);
	        else
	            next[dst] = algo[src++];
	    }
	    synth.setAlgo(next);
	}
}