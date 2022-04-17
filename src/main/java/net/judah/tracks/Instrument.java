package net.judah.tracks;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.beatbox.JudahKit;
import net.judah.clock.BeatBuddy;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.midi.JudahMidi;
import net.judah.util.RTLogger;

public class Instrument extends JComboBox<String> implements ActionListener{

	private static final HashMap<JackPort, Integer> voices = new HashMap<>();
	private static final HashMap<JackPort, Integer> drums = new HashMap<>();
	
	private Track track;
	
	@Getter private final ActionListener listener = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			set(track, getSelectedIndex()); }};
	
	public Instrument(Track t) {
		track = t;
		fillInstruments();
	}
	
	public void fillInstruments() {
		removeActionListener(listener);
		if (getItemCount() > 0)
			removeAllItems();
        	
        if (track.getMidiOut() == JudahMidi.getInstance().getDrumsOut())
        	for (int i = 1; i < BeatBuddy.Drumset.values().length; i++)
        		addItem(BeatBuddy.Drumset.values()[i].name());
        else if (track instanceof StepTrack)
        	for (int i = 0; i < JudahKit.values().length; i++)
        		addItem(JudahKit.values()[i].getDisplay());
        else {
        	ArrayList<FluidInstrument> list; 
        	if (track.isDrums()) 
        		list = FluidSynth.getInstruments().getDrumkits();
        	else 
        		list = FluidSynth.getInstruments().getInstruments();
        	for (int i = 0; i < list.size(); i++)
        		addItem(list.get(i).name);
        }
        
        Integer idx = lookup(track);

        if (idx == null || idx < 0)
        	idx = 0;
        setSelectedIndex(idx);
        addActionListener(listener);
	}
	

    /** get PROG CHANGE MIDI code for selected instrument 
     * @throws InvalidMidiDataException */
    public static ShortMessage reverseLookup(Track track, int selected) throws InvalidMidiDataException {
    	if (track.isDrums()) {
    		if (track.getMidiOut() == JudahMidi.getInstance().getDrumsOut()) {
    			return Midi.create(ShortMessage.CONTROL_CHANGE, BeatBuddy.DRUMSET, selected + 1);
    		}
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
	
	public static void set(Track track, int idx) {
		if (idx < 0) return;
		try {
			ShortMessage midi = reverseLookup(track, idx);
        	JudahMidi.queue(midi, track.getMidiOut());
        	if (track.isDrums()) 
        		drums.put(track.getMidiOut(), idx);
        	else
        		voices.put(track.getMidiOut(), idx);
        } catch (InvalidMidiDataException e) {
        	RTLogger.warn(track, e);
		}

	}

	
}
