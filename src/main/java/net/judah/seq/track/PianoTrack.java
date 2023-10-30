package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.Mode;
import net.judah.song.Sched;
import net.judah.synth.Polyphony;

public class PianoTrack extends MidiTrack {
	
	public static int DEFAULT_POLYPHONY = 24;
	public static int MONOPHONIC = 1;

	@Getter private final Arp arp = new Arp(this);
	
	public PianoTrack(String name, MidiReceiver out, int ch, JudahClock clock) throws InvalidMidiDataException {
		this(name, out, ch, clock, DEFAULT_POLYPHONY);
	}
	
	public PianoTrack(String name, Polyphony notes, JudahClock clock) throws InvalidMidiDataException {
		super(name, notes, clock);
	}


	public PianoTrack(String name, MidiReceiver out, int ch, JudahClock clock, int polyphony) throws InvalidMidiDataException {
		super(name, out, ch, JudahClock.MIDI_24, clock, polyphony);
	}

	@Override
	public void setState(Sched sched) {
		super.setState(sched);
		if (arp.getInfo() != state.getArp())
			arp.setInfo(state.getArp());
	}
	
	@Override
	public void setActive(boolean on) {
		super.setActive(on);
		if (!on) { // TODO actives
			if (arp.isActive())
				arp.clear();
			new Panic(this);
		}

	}


	@Override
	protected void playNote(ShortMessage msg) {
		if (arp.getMode() == Mode.Off)
    		midiOut.send(msg, JudahMidi.ticker());
    	else  
    		arp.process(msg);
		
	}
	
	@Override
	protected void flush() { // mode check?
		long end = (current + 1) * barTicks;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() <= recent) continue;
			if (e.getTick() > end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOff(e.getMessage())) 
				midiOut.send(Midi.format((ShortMessage)e.getMessage(), ch, 1), JudahMidi.ticker());
		}
		if (arp.isActive())
			arp.clear();
		
	}

	@Override
	public void importTrack(Track incoming, int rez) {
		super.importTrack(incoming, rez);
		
		if (arp.isActive())
			arp.clear();
		new Panic(this);
		for (int i = 0; i < incoming.size(); i++) 
			t.add(incoming.get(i));

	}

	@Override public void next(boolean fwd) {
		if (arp.isActive())
			arp.clear();
		super.next(fwd);
	}

}
