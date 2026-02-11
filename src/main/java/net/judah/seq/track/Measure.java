// java
package net.judah.seq.track;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;

public class Measure extends ArrayList<PianoNote> implements MidiConstants {

//	public Measure
//		public Measure(Measure copy) {
//			addAll(copy);


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

		int idx = MidiTools.find(t, start);
		if (idx < 0) idx = 0;

		for (int i = idx; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			long tick = e.getTick();
			if (tick < start) continue;
			if (tick >= end) break;
			if (Midi.isNoteOn(e.getMessage()))
				add(new PianoNote(e));
		}
	}

	/** loads two bars of notes from track's current position*/
	public void loadPiano() {
		stash.clear();
		long start = track.getLeft();
		long end = start + track.getWindow();

		int idxStart = MidiTools.find(t, start);
		if (idxStart < 0) idxStart = 0;

		// iterate only relevant portion of track
		for (int i = idxStart; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			long tick = e.getTick();
			if (tick < start) continue;
			if (tick >= end) break;
			if (!(e.getMessage() instanceof ShortMessage)) continue;

			ShortMessage s = (ShortMessage) e.getMessage();
			if (s.getCommand() == NOTE_ON) {
				stash.add(e);
			} else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = stash.get(s);
				long time = on == null ? start : on.getTick();
				int velocity = on == null ? 99 : ((ShortMessage) on.getMessage()).getData2();
				add(new PianoNote(
						new MidiEvent(Midi.create(NOTE_ON, ch, s.getData1(), velocity), time),
						new MidiEvent(Midi.create(NOTE_OFF, ch, s.getData1()), e.getTick())));
			}
		}

		// Resolve any remaining hanging note-ons in stash.
		for (MidiEvent e : stash) {
			ShortMessage on = (ShortMessage) e.getMessage();
			int data1 = on.getData1();

			int idx = MidiTools.find(t, e.getTick());
			if (idx < 0) idx = 0;

			PianoNote target = null;

			// First pass: search forward from idx
			for (int i = idx; i < t.size(); i++) {
				MidiEvent cand = t.get(i);
				if (!(cand.getMessage() instanceof ShortMessage sht)) continue;
				if (!Midi.isNoteOff(sht) || sht.getData1() != data1) continue;

				long offTick = MidiTools.wrapTickInWindow(cand.getTick(), start, track.getWindow());
				target = new PianoNote(new MidiEvent(on, e.getTick()), new MidiEvent(sht, offTick));
				break;
			}

			// Second pass: search earlier events (to catch wrapped offs)
			if (target == null) {
				for (int i = 0; i < idx; i++) {
					MidiEvent cand = t.get(i);
					if (!(cand.getMessage() instanceof ShortMessage sht)) continue;
					if (!Midi.isNoteOff(sht) || sht.getData1() != data1) continue;

					long offTick = MidiTools.wrapTickInWindow(cand.getTick(), start, track.getWindow());
					target = new PianoNote(new MidiEvent(on, e.getTick()), new MidiEvent(sht, offTick));
					break;
				}
			}

			// If still not found, synthesize a note-off at the window end
			if (target == null) {
				target = new PianoNote(new MidiEvent(on, e.getTick()),
						new MidiEvent(Midi.create(NOTE_OFF, ch, data1, on.getData2()), end - 1));
			}

			add(target);
		}
	}

	@Deprecated
	@Override public boolean contains(Object o) {
		if (o instanceof MidiEvent evt) {
			for (PianoNote note : this)
				if (note.equals(evt))
					return true;
		}
		return false;
	}

	@Deprecated
	public boolean contains(long tick, int data1) {
		for (PianoNote note : this)
			if (note.getTick() <= tick)
				if (note.getOff() == null || note.getOff().getTick() > tick) {
					if (note.getMessage() instanceof ShortMessage &&
							((ShortMessage)note.getMessage()).getData1() == data1)
					return true;
				}
		return false;
	}


}
