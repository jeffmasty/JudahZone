package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.midi.Midi;

@RequiredArgsConstructor
public class Measure extends ArrayList<MidiPair> implements MidiConstants {
	
	@Getter private Accumulator stash = new Accumulator();
	
	private final MidiTrack track;
	private final Track t;
	@Getter @Setter protected long startTick;
	@Getter @Setter protected long bTick; // optional

	public Measure(MidiTrack track) {
		this.track = track;
		this.t = track.getT();
	}
	
	public void populate() {
		if (track.isEven()) {
			startTick = track.getCurrent() * track.getBarTicks();
			bTick = track.getNext() * track.getBarTicks();
		}
		else {
			startTick = track.getPrevious() * track.getBarTicks();
			bTick = track.getCurrent() * track.getBarTicks();
		}
		clear();
		if (track.isDrums()) {
			loadDrums();
		}
		else {
			loadPiano();
		}

	}
	
	/** loads two bars from measure of track into the supplied result, zero-basing the ticks*/
	public void loadDrums() {
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < startTick) continue;
			if (e.getTick() >= bTick) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOn(((ShortMessage)e.getMessage())))
				add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() - startTick), null));
		}
		long oneBar = track.getBarTicks();
		long end = bTick + oneBar;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < bTick) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOn(((ShortMessage)e.getMessage())))
				add(new MidiPair(new MidiEvent(e.getMessage(), e.getTick() - bTick + oneBar), null));
		}
	}

	
	
	/** loads two bars from measure of track into the supplied result, zero-basing the ticks*/
	public void loadPiano() {
		stash.clear();
		long oneBar = track.getBarTicks();
		long end = startTick + oneBar;
		for (int i = 0; i < t.size(); i++) { // first bar
			MidiEvent e = t.get(i);
			if (e.getTick() < startTick) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)e.getMessage();
			if (s.getCommand() == NOTE_ON) 
				stash.add(new MidiEvent(e.getMessage(), e.getTick() - startTick)); 
			else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = stash.get(s);
				long time = on == null ? oneBar : on.getTick() - startTick; 
				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
				add(new MidiPair(
						new MidiEvent(Midi.create(NOTE_ON, track.getCh(), s.getData1(), velocity), time),
						new MidiEvent(Midi.create(NOTE_OFF, track.getCh(), s.getData1()), e.getTick() - startTick)));
			}
		}
		end = bTick + oneBar;
		for (int i = 0; i < t.size(); i++) { // second bar
			MidiEvent e = t.get(i);
			if (e.getTick() < bTick) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)e.getMessage();
			if (s.getCommand() == NOTE_ON) 
				stash.add(new MidiEvent(e.getMessage(), e.getTick() - bTick + oneBar));
			else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = stash.get(s);
				long time = on == null ? oneBar : on.getTick() - bTick + oneBar; 
				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
				add(new MidiPair(
						new MidiEvent(Midi.create(NOTE_ON, track.getCh(), s.getData1(), velocity), time),
						new MidiEvent(Midi.create(NOTE_OFF, track.getCh(), s.getData1()), e.getTick() - bTick + oneBar)));
			}
		}
		// anything left in accumulator?
		for (MidiEvent e : stash) {
			ShortMessage on = (ShortMessage)e.getMessage();
			ShortMessage off = Midi.create(NOTE_OFF, track.getCh(), on.getData1(), on.getData2());
			add(new MidiPair(new MidiEvent(on, e.getTick()), new MidiEvent(off, bTick + oneBar - 1)));
		}

	}
	
	
}
