package net.judah.tracker;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.api.Midi;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.GMNames;
import net.judah.midi.JudahMidi;

// MidiGui, Tracks and BeatBoxs guis

public class Instrument extends JComboBox<String> {

	private static final HashMap<JackPort, Integer> voices = new HashMap<>();
	private static final HashMap<JackPort, Integer> drums = new HashMap<>();
	
	private Track track;
	
	private final ActionListener listener = new ActionListener() {
		
		@Override public void actionPerformed(ActionEvent evt) {
			String name = "" + getSelectedItem();
			ArrayList<FluidInstrument> pack = track.isDrums()
                ? FluidSynth.getInstruments().getDrumkits()
                : FluidSynth.getInstruments().getInstruments();
			for (FluidInstrument patch : pack)
				if (patch.name.equals(name)) { 
					track.setInstrument(name);
					JudahMidi.getInstance().progChange(patch.index, track.getMidiOut(), track.getCh());
            }
		}
	};
	
	public Instrument(Track t) {
		track = t;
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		fillInstruments();
	}

	public void setInstrument(String s) {
		removeActionListener(listener);
		setSelectedItem(s);
		addActionListener(listener);
	}
	

	public void fillInstruments() {
		removeActionListener(listener);
		if (getItemCount() > 0)
			removeAllItems();
        	
		//if (track.getMidiOut() == JudahMidi.getInstance().getDrumsOut())
		//	for (int i = 1; i < BeatBuddy.Drumset.values().length; i++)
		//		addItem(BeatBuddy.Drumset.values()[i].name()); else 
    	if (track.isDrums()) 
    		for (FluidInstrument i :FluidSynth.getInstruments().getDrumkits())
    			addItem(i.name);
    	else 
    		for (String s : GMNames.GM_NAMES)
    			addItem(s);
        Integer idx = lookup(track);

        if (idx == null || idx < 0)
        	idx = 0;
        setSelectedIndex(idx);
        if (track.getInstrument() != null)
        	setInstrument(track.getInstrument());
        addActionListener(listener);

	}
	

    /** get PROG CHANGE MIDI code for selected instrument 
     * @throws InvalidMidiDataException */
    public static ShortMessage reverseLookup(Track track, int selected) throws InvalidMidiDataException {
    	if (track.isDrums()) {
			//if (track.getMidiOut() == JudahMidi.getInstance().getDrumsOut()) {
			//	return Midi.create(ShortMessage.CONTROL_CHANGE, BeatBuddy.DRUMSET, selected + 1); }
    		return new Midi(ShortMessage.PROGRAM_CHANGE, 9, 
    				FluidSynth.getInstruments().getDrumkits().get(selected).index);
    	}
    	
    	return new Midi(ShortMessage.PROGRAM_CHANGE, 0, FluidSynth.getInstruments().getInstruments().get(selected).index);
	}

	public void panic() {
    	for (int i = 0; i <= 127; i++)
    		JudahMidi.queue(Midi.create(Midi.NOTE_OFF, track.getCh(), i), track.getMidiOut());
    }

	public static Integer lookup(Track t) {
		if (t.isDrums()) {
			return drums.get(t.getMidiOut());
		}
		return voices.get(t.getMidiOut());
	}

	public static int lookup(String name, Track t) {
		for (FluidInstrument patch : t.isDrums() ? 
				FluidSynth.getInstruments().getDrumkits()
				: FluidSynth.getInstruments().getInstruments())
			if (patch.name.equals(name)) 
				return patch.index;
		return 0;
	}
	
}
