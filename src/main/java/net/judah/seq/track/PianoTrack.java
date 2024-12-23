package net.judah.seq.track;

import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.api.MidiClock;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.Mode;
import net.judah.song.Sched;
import net.judah.synth.Polyphony;

public class PianoTrack extends MidiTrack {

	public static int DEFAULT_POLYPHONY = 24;
	public static int MONOPHONIC = 1;

	@Getter private final Arp arp = new Arp(this);
	private final ArrayList<MidiEvent> chads = new ArrayList<>();

	public PianoTrack(String name, ZoneMidi out, int ch, JudahClock clock) throws InvalidMidiDataException {
		this(name, out, ch, clock, DEFAULT_POLYPHONY);
	}

	public PianoTrack(String name, Polyphony notes, JudahClock clock) throws InvalidMidiDataException {
		super(name, notes, clock);
	}

	public PianoTrack(String name, ZoneMidi out, int ch, JudahClock clock, int polyphony) throws InvalidMidiDataException {
		super(name, out, ch, MidiClock.MIDI_24, clock, polyphony);
	}

	@Override
	public void setState(Sched sched) {
		super.setState(sched);
		if (sched.getArp() != arp.getInfo())
			arp.setInfo(state.getArp());
		if (sched.active && clock.isActive() && clock.getBeat() == 0)
			playTo(0); // kludge
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

	@Override public void next(boolean fwd) {
		if (arp.isActive())
			arp.clear();
		super.next(fwd);
	}

	@Override
	protected void parse(Track incoming) {
		if (arp.isActive())
			arp.clear();
		new Panic(this);
		for (int i = 0; i < incoming.size(); i++)
			t.add(incoming.get(i));
	}

	@Override
	protected void processRecord(ShortMessage m, long ticker) {
		midiOut.send(m, ticker);
		if (Midi.isNoteOn(m))
			chads.add(new MidiEvent(Midi.copy(m), recent));
		else if (Midi.isNoteOff(m)) {
			MidiEvent used = null;
			for (MidiEvent on : chads) {
				if (((ShortMessage)on.getMessage()).getData1() != m.getData1())
					continue;
				used = on;
				MainFrame.getMidiView(this).getGrid().push(
					new Edit(Type.NEW, new MidiPair(on, new MidiEvent(Midi.copy(m), recent))));
				break;
			}
			if (used != null)
				chads.remove(used);
		}
		// TODO CC, progchange, pitchbend
	}

}
