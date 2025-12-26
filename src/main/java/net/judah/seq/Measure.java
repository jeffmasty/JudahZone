package net.judah.seq;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.midi.Midi;
import net.judah.seq.track.MidiTrack;

public class Measure extends Notes implements MidiConstants {

	private final Accumulator stash = new Accumulator();
	private final MidiTrack track;
	private final Track t;
	private final int ch;

	public Measure(MidiTrack track) {
		this.track = track;
		this.t = track.getT();
		this.ch = track.getCh();
	}

	public Measure populate() {
		clear();
		if (track.isDrums())
			loadDrums();
		else
			loadPiano();
		return this;
	}

	/** loads two bars from track's current position, not aware of NoteOFFs */
	public void loadDrums() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		MidiEvent e;
		for (int i = 0; i < t.size(); i++) {
			e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			if (Midi.isNoteOn((e.getMessage())))
				add(new MidiNote(e));
		}
	}


	/** loads two bars of notes from track's current position*/
	public void loadPiano() {
		stash.clear();
		long start = track.getLeft();
		long end = start + track.getWindow();

		for (int i = MidiTools.find(t, start); i < t.size() && i >= 0; i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() > end) break;
			if (e.getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)e.getMessage();
			if (s.getCommand() == NOTE_ON)
				stash.add(e);
			else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = stash.get(s);
				long time = on == null ? start : on.getTick();
				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
				add(new MidiNote(
						new MidiEvent(Midi.create(NOTE_ON, ch, s.getData1(), velocity), time),
						new MidiEvent(Midi.create(NOTE_OFF, ch, s.getData1()), e.getTick())));
			}
		}
		// anything left in accumulator? // TODO hanging chads
		for (MidiEvent e : stash) {
			ShortMessage on = (ShortMessage)e.getMessage();
			int data1 = on.getData1();

			// go grab a noteOff for this noteOn...
			int idx = MidiTools.find(t, e.getTick());
			if (idx < 0) { // hmm
				ShortMessage off = Midi.create(NOTE_OFF, ch, on.getData1(), on.getData2());
				add(new MidiNote(new MidiEvent(on, e.getTick()), new MidiEvent(off, end - 1)));
				continue;
			}
			MidiNote target = null;
			for (int i = idx; i < t.size(); i++) {
				if (t.get(i).getMessage() instanceof ShortMessage sht && Midi.isNoteOff(sht)
						&& sht.getData1() == data1) {
					target = new MidiNote(new MidiEvent(on, e.getTick()), new MidiEvent(sht, t.get(i).getTick()));
					break;
				}
			}
			if (target == null) // hmm
				target = new MidiNote(new MidiEvent(on, e.getTick()), new MidiEvent(
						Midi.create(NOTE_OFF, ch, data1, on.getData2()), end - 1));
			add(target);
		}
	}



}
