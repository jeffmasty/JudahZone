package net.judah.midi;

import java.util.ArrayList;
import java.util.HashSet;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.api.Midi;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.tracker.Track;
import net.judah.util.RTLogger;
import net.judah.util.SettableCombo;

/**Handles Prog Change midi for a synth port on a channel, 
 * synchronizes updates between combo box instances */
public class ProgChange extends SettableCombo<String> {
	
	private final Track t;
	private final JackPort midiOut;
	private final int ch;
	public static final int LIMIT = 100; // not doing the full 127 (knob precision)
	private static final HashSet<ProgChange> widgets = new HashSet<>();
	
	public ProgChange(Track track) {
		super(() -> change(track));
		midiOut = null;
		ch = track.getCh();
		t = track;
		init();
	}
	
	public ProgChange(JackPort port, int channel) {
		super(() -> change(port, channel));
		t = null;
		midiOut = port;
		ch = channel;
		init();
	}

	private void init() {
		if (ch == 0)
			for (String s : GMNames.GM_NAMES)
				addItem(s);
		else 
			for (FluidInstrument d : FluidSynth.getInstruments().getDrumkits()) // todo hardcode?
				addItem(d.name);
		addActionListener(this);
		widgets.add(this);

	}
	
	public static void change(JackPort midiOut, int ch) {
		int change = ((ProgChange)SettableCombo.getFocus()).getSelectedIndex();
		if (ch == 9)
			change = FluidSynth.getInstruments().getDrumkits().get(change).index;
		progChange(change, midiOut, ch);
	}
	
	public static void change(Track t) {
		int change = ((ProgChange)SettableCombo.getFocus()).getSelectedIndex();
		if (t.isDrums())
			change = FluidSynth.getInstruments().getDrumkits().get(change).index;
//		progChange(change, t.getMidiOut(), t.getCh());
		if (t!=null)
			t.setInstrument(change + "");
	}
	
	public static void next(boolean fwd, JackPort port, int channel) {
		int change = 0;
		if (channel == 9)
		for (ProgChange combo : widgets) {
			if (port != combo.midiOut || channel != combo.ch) 
				continue;
			change = combo.getSelectedIndex() + (fwd ? 1 : -1);
			if (change < 0)
				change = combo.getItemCount() - 1;
			if (change >= combo.getItemCount())
				change = 0;
			if (channel == 9)
				change = FluidSynth.getInstruments().getDrumkits().get(change).index;
			progChange(change, port, channel);
			
		}
	}
	
	public static void progChange(int preset, JackPort out, int ch) {
		try {
			Midi midi = new Midi(ShortMessage.PROGRAM_CHANGE, ch, preset);
			JudahMidi.queue(midi, out);
		} catch (Exception  e) {
			RTLogger.warn(ProgChange.class.getSimpleName(), e);
			return;
		}
		new Thread(() -> {
			for (ProgChange combo : widgets) {
				if (preset == combo.getSelectedIndex())
					continue;
				if (ch != combo.ch )
					continue;
				if (combo.t == null) {
					if (out != combo.midiOut )
						continue;
				}
//				else if (combo.t.getMidiOut() != out)
//					continue;
				int change = preset;
				if (ch == 9) {
					ArrayList<FluidInstrument> drums = FluidSynth.getInstruments().getDrumkits();
					for (int i = 0; i < drums.size(); i++)
						if (drums.get(i).index == preset)
							change = i;
				}
				combo.setSelectedIndex(change); // reverse lookup on drums?
			}
			highlight(null);
		}).start();
	}

	public static void progChange(String instrument, JackPort out, int channel) {
		if (channel == 0) {
			for (int i = 0; i < GMNames.GM_NAMES.length; i++) 
        		if (GMNames.GM_NAMES[i].equals(instrument)) 
        			ProgChange.progChange(i, out, channel);
		}
		else {
			for (FluidInstrument i :FluidSynth.getInstruments().getDrumkits()) {
				if (i.name.equals(instrument))
					progChange(i.index, out, channel);
			}
		}
	}
}
