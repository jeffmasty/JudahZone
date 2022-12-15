package net.judah.midi;

import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.judah.api.MidiReceiver;
import net.judah.util.RTLogger;
import net.judah.widgets.SettableCombo;

/**Handles Prog Change midi for a synth port on a channel, 
 * synchronizes updates between combo box instances */
public class ProgChange extends SettableCombo<String> {
	
	private static final HashMap<MidiReceiver, String> lookup = new HashMap<>();
	
	public static final int LIMIT = 100; // not doing the full 127 GM instruments (knob precision)
	private static final HashSet<ProgChange> widgets = new HashSet<>();
	
	private MidiReceiver out;
	private int ch;
	
	ActionListener listener = e -> lookup.put(out, "" + getSelectedItem());
			
	public ProgChange(MidiReceiver r, int channel) {
		ch = channel;
		out = r;
		reset(r.getProg(channel));
		setAction(()->progChange(lookup.get(out), out, ch));
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
	}

	public static void change(MidiReceiver r, int ch) {
		r.progChange(lookup.get(r), ch);
	}
	
	public static void change(MidiReceiver r) {
		r.progChange(lookup.get(r));
	}
	
	public static void next(boolean fwd, MidiReceiver r, int ch) {
		int change = r.getProg(ch) + (fwd ? 1 : -1);
			if (change < 0)
				change = r.getPatches().length - 1;
			if (change >= r.getPatches().length - 1)
				change = 0;
			progChange(change, r, ch);
	}
	
	public static void progChange(int preset, MidiReceiver r, int ch) {
		progChange(r.getPatches()[preset], r, ch);
	}

	public static void progChange(String instrument, MidiReceiver r, int ch) {
		try {
			r.progChange(instrument, ch);
		new Thread(() -> {
			for (ProgChange combo : widgets) {
				if (instrument.equals(combo.getSelectedItem()))
					continue;
				if (r != combo.out)
					continue;
				combo.setSelectedItem(instrument); 
			}
			highlight(null);
		}).start();

		} catch (Exception  e) {
			RTLogger.warn(ProgChange.class.getSimpleName(), e);
			return;
		}
	}

	public void reset(int selected) {
		removeActionListener(listener);
		removeAllItems();
		String[] items = out.getPatches();
		for (int i = 0; i < items.length; i++) {
			addItem(items[i]);
			if (i == selected)
				setSelectedIndex(i);
		}
		addActionListener(listener);
	}

	public void setOut(MidiReceiver r, int channel) {
		ch = channel;
		out = r;
		reset(r.getProg(channel));
	}
	
}
