package net.judah.seq.arp;

import java.util.List;
import java.util.Map;

import javax.sound.midi.ShortMessage;

import lombok.Data;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.ModeCombo;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.ChordListener;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.util.Constants;

@Data
public class Arp implements ChordListener {

	private final PianoTrack track;
	private final ChordTrack chords = JudahZone.getChords();
	private ArpInfo info = new ArpInfo();
	private Algo algo = new Echo();
	private final Deltas deltas = new Deltas();
	private final Poly workArea = new Poly();

	public Arp(PianoTrack t) {
		track = t;
		chords.addListener(this);
	}
	
	public void setRange(int range) {
		info.setRange(range);
		if (algo != null) algo.setRange(range);
		MainFrame.update(track);
	}
	
	@Override
	public void chordChange(Chord from, Chord to) {
		if (!track.isActive() || algo instanceof Ignorant)
			return;
		if (algo != null)
			algo.change();
		for (Map.Entry<ShortMessage, List<Integer>> item : deltas.list()) {
			ShortMessage midi = item.getKey();
			Midi off = Midi.create(Midi.NOTE_OFF, midi.getChannel(), midi.getData1(), midi.getData2());
			process(off, from);
			process(midi, to);
		}
	}

	public void clear() {
		if (!isActive() || deltas.isEmpty()) return;
		Midi off = Midi.create(Midi.NOTE_OFF, track.getCh(), 36, 1);
		for (Map.Entry<ShortMessage, List<Integer>> item : deltas.list()) 
			out(off, item.getValue());
		deltas.clear();
	}

	public void toggle(Mode m) {
		setMode(info.algo == m ? Mode.Off : m);
	}

	public void setInfo(ArpInfo arp) {
		boolean setMode = info.algo != arp.algo;
		info = arp;
		if (setMode) 
			setMode(info.algo);
		else  
			clear();
		if (algo != null) 
			algo.setRange(arp.getRange());
	}

	public Mode getMode() {
		return info.getAlgo();
	}
	
	public int getRange() {
		return info.getRange();
	}

	public boolean isActive() {
		return info.algo != Mode.Off;
	}
	
	public void setMode(Mode m) {
		if (algo instanceof TimeListener)
			track.getClock().removeListener((TimeListener)algo);
		clear();
		//new Panic(track);
		info.algo = m;
		switch(info.algo) {
			case Off: algo = new Echo(); break; // not used
			case CHRD: algo = new Gen(); break;
			case BASS: algo = new Bass(); break;
			case MPK: algo = new MPKTranspose(track); break;
			case ABS: algo = new ABS(); break;
			case UP: algo = new Up(); break;
			case DWN: algo = new Down(); break;
			case UPDN: algo = new UpDown(true); break;
			case DNUP: algo = new UpDown(false); break;
			case RND: algo = new RND(); break;
			case RACM: algo = new Racman(); break;
			case ETH: algo = new Ethereal(); break;
			// case REC: algo = new REC(track); break;
			// case REL: algo = new REL(); break;
			// case UP5: case DN5:
		}
		algo.setRange(info.range);
		Constants.execute(()-> {
			ModeCombo.update(track);
			MainFrame.miniSeq().update(track);
			MainFrame.getMidiView(track).getMenu().updateMode();
		});
		
		
	}

	/** externally triggered */
	public void process(ShortMessage msg) {
		if (chords.isActive() || algo instanceof Ignorant)
			process(msg, chords.getChord());
	}

	private void process(ShortMessage msg, Chord chord) {
		if (chord == null) {
			clear();
			return;
		}

		if (Midi.isNoteOn(msg)) {
				algo.process(msg, chord, workArea.empty());
			if (!workArea.isEmpty()) {
				deltas.add(msg, workArea);
				out(msg, workArea);
			}
		} else if (Midi.isNoteOff(msg)) {
			out(msg, deltas.remove(msg));
		}
	}
	
	private void out(ShortMessage input, List<Integer> work) {
		if (work == null)
			return;
		for (Integer data1 : work)
			track.getMidiOut().send(Midi.create(
				input.getCommand(), input.getChannel(), data1, input.getData2()), JudahMidi.ticker());
	}

	public boolean mpkFeed(Midi midi) {
		if (track.isRecord())
			return false;
		if (info.algo != Mode.MPK)
			return false;
		((Feed)algo).feed(midi);
		return true;
	}
	
	public class Echo extends Algo implements Ignorant {
	@Override public void process(ShortMessage m, Chord chord, Poly result) {
		result.add(m.getData1()); }}
	
	public class Gen extends Algo {
		@Override public void process(ShortMessage m, Chord chord, Poly result) {
			if (range < 13)  chord.tight(m.getData1(), result); 
			else chord.wide(m.getData1(), result); }}
	
	public class Bass extends Algo {
		@Override public void process(ShortMessage m, Chord chord, Poly result) {
			result.add(m.getData1() + Key.key(m.getData1()).interval(chord.getBass())); }}

	/** translate root of chord by off from middle C of m.getData1() */
	public class ABS extends Algo implements Ignorant {
		@Override public void process(ShortMessage m, Chord chord, Poly result) {
			result.add(m.getData1() + chord.getRoot().ordinal()); }}

}
