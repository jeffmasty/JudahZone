package net.judah.midi;

import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackPort;

public class ProgChange extends JComboBox<String> {

	private final JackPort midiOut;
	public static final int LIMIT = 100; // not doing the full 127 (knob precision)
	
	public ProgChange(JackPort port) {
		midiOut = port;
		for (String s : GMNames.GM_NAMES)
			addItem(s);
		addActionListener(e -> JudahMidi.getInstance().progChange(getSelectedIndex(), midiOut, 0));
	}
	
	
	
}
