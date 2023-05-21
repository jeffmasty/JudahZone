package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.RequiredArgsConstructor;
import net.judah.midi.Midi;

@RequiredArgsConstructor
public class Measure extends ArrayList<MidiPair> implements MidiConstants {
	
	private final Accumulator stash = new Accumulator();
	private final MidiTrack track;
	private final Track t;

	public Measure(MidiTrack track) {
		this.track = track;
		this.t = track.getT();
	}
	
	public void populate() {
		clear();
		if (track.isDrums()) 
			loadDrums();
		else 
			loadPiano();
	}
	
	/** loads two bars from measure of track into the supplied result, zero-basing the ticks*/
	public void loadDrums() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		MidiEvent e;
		for (int i = 0; i < t.size(); i++) {
			e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOn((e.getMessage())))
				add(new MidiPair(e, null));
		}
//		long oneBar = track.getBarTicks();
//		long startTick = track.getLeft();
//		long bTick = track.getRight();
//		long end = bTick + oneBar;
//
//		for (int i = 0; i < t.size(); i++) {
//			MidiEvent e = t.get(i);
//			if (e.getTick() < startTick) continue;
//			if (e.getTick() >= bTick) break;
//			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOn(((ShortMessage)e.getMessage())))
//				add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() - startTick), null));
//		}
//		for (int i = 0; i < t.size(); i++) {
//			MidiEvent e = t.get(i);
//			if (e.getTick() < bTick) continue;
//			if (e.getTick() >= end) break;
//			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOn(((ShortMessage)e.getMessage())))
//				add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() - bTick + oneBar), null));
//		}
	}
	
	
	/** loads two bars from measure of track into the supplied result*/
	public void loadPiano() {
		stash.clear();
		long startTick = track.getLeft();
		long end = startTick + track.getWindow(); 
		for (int i = 0; i < t.size(); i++) { 
			MidiEvent e = t.get(i);
			if (e.getTick() < startTick) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)e.getMessage();
			if (s.getCommand() == NOTE_ON) 
				stash.add(e); 
			else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = stash.get(s);
				long time = on == null ? startTick : on.getTick(); 
				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
				add(new MidiPair(
						new MidiEvent(Midi.create(NOTE_ON, track.getCh(), s.getData1(), velocity), time),
						new MidiEvent(Midi.create(NOTE_OFF, track.getCh(), s.getData1()), e.getTick())));
			}
		}
		// anything left in accumulator?
		for (MidiEvent e : stash) {
			ShortMessage on = (ShortMessage)e.getMessage();
			ShortMessage off = Midi.create(NOTE_OFF, track.getCh(), on.getData1(), on.getData2());
			add(new MidiPair(new MidiEvent(on, e.getTick()), new MidiEvent(off, end - 1)));
		}

	}
	
	
}
