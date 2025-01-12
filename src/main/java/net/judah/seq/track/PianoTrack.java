package net.judah.seq.track;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.api.MidiClock;
import net.judah.api.TimeListener;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.ModeCombo;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.omni.Threads;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.Poly;
import net.judah.seq.arp.Algo;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.ArpInfo;
import net.judah.seq.arp.Deltas;
import net.judah.seq.arp.Down;
import net.judah.seq.arp.Ethereal;
import net.judah.seq.arp.Feed;
import net.judah.seq.arp.Ignorant;
import net.judah.seq.arp.MPKTranspose;
import net.judah.seq.arp.RND;
import net.judah.seq.arp.Racman;
import net.judah.seq.arp.Up;
import net.judah.seq.arp.UpDown;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.ChordListener;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.Sched;
import net.judah.synth.taco.Polyphony;

/** A melodic MidiTrack with an Arpeggiator */
public class PianoTrack extends MidiTrack implements ChordListener {

	public static int DEFAULT_POLYPHONY = 24;
	public static int MONOPHONIC = 1;

	@Getter private ArpInfo info = new ArpInfo();
	private final ChordTrack chords = JudahZone.getChords();
	private final Deltas deltas = new Deltas();
	private final Poly workArea = new Poly();
	private Algo algo = new Echo();
	private final ArrayList<MidiEvent> chads = new ArrayList<>();

	public PianoTrack(String name, ZoneMidi out, int ch, JudahClock clock) throws InvalidMidiDataException {
		this(name, out, ch, clock, DEFAULT_POLYPHONY);
		chords.addListener(this);
		info = getState().getArp();
		this.clear();
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
		if (sched.getArp() != info)
			setInfo(state.getArp());
		if (sched.active && clock.isActive() && clock.getBeat() == 0)
			playTo(0); // kludge
	}

	@Override
	public void setActive(boolean on) {
		super.setActive(on);
		if (!state.active) { // TODO actives
			if (isArpOn())
				silence();
			new Panic(this);
		}
	}

	@Override
	protected void playNote(ShortMessage msg) {
		if (info.getAlgo() == Arp.Off)
    		midiOut.send(msg, JudahMidi.ticker());
    	else
    		arpeggiate(msg);
	}

	@Override public void next(boolean fwd) {
		silence();
		super.next(fwd);
	}

	@Override
	protected void parse(Track incoming) {
		silence();
		new Panic(this);
		for (int i = 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (e.getMessage() instanceof ShortMessage orig)
				t.add(new MidiEvent(Midi.create(
					orig.getCommand(), ch, orig.getData1(), orig.getData2()), e.getTick()));
		}
	}

	@Override
	public boolean capture(Midi midi) {
		if (!capture)
			return false;
		long tick = quantize(recent);


		Midi m = Midi.create(midi.getCommand(), ch, midi.getData1(), midi.getData2());


		if (Midi.isNoteOn(m)) {
			chads.add(new MidiEvent(m, tick));
			if (tick <= recent)
				midiOut.send(m, JudahMidi.ticker());
		}
		else if (Midi.isNoteOff(m)) {
			midiOut.send(m, JudahMidi.ticker());
			MidiEvent used = null;
			for (MidiEvent on : chads) {
				if (((ShortMessage)on.getMessage()).getData1() != m.getData1())
					continue;
				used = on;
				MainFrame.getMidiView(this).getGrid().push(
					new Edit(Type.NEW, new MidiPair(on, new MidiEvent(m, tick))));
				break;
			}
			if (used != null)
				chads.remove(used);
			//if (tick <= recent)
		}
		// TODO CC, progchange, pitchbend
		else
			return false;
		return true;
	}

	public int getRange() {
		return info.getRange();
	}

	public void setRange(int range) {
		info.setRange(range);
		if (algo != null) algo.setRange(range);
		MainFrame.update(this);
	}

	@Override
	public void chordChange(Chord from, Chord to) {
		if (!isActive() || algo instanceof Ignorant)
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
		silence();
	}

	/** clear the Arpeggiator */
	private void silence() {
		if (isArpOn() == false)
			return;
		if (deltas.isEmpty())
			return;
		Midi off = Midi.create(Midi.NOTE_OFF, ch, 36, 1);
		for (Map.Entry<ShortMessage, List<Integer>> item : deltas.list())
			out(off, item.getValue());
		deltas.clear();
	}

	public void setInfo(ArpInfo arp) {
		boolean setMode = info.algo != arp.algo;
		info = arp;
		if (setMode)
			setArp(info.algo);
		else
			silence();
		if (algo != null)
			algo.setRange(arp.getRange());
	}

	public Arp getArp() {
		return info.getAlgo();
	}

	public boolean isArpOn() {
		return info.algo != Arp.Off;
	}

	public boolean isMpkOn() {
		return info.algo == Arp.MPK;
	}

	public void toggle(Arp m) {
		setArp(info.getAlgo() == m ? Arp.Off : m);
	}

	public void setArp(Arp mode) {
		if (algo instanceof TimeListener)
			clock.removeListener((TimeListener)algo);
		silence();
		//new Panic(track);
		info.algo = mode;
		switch(info.algo) {
			case Off: algo = new Echo(); break; // not used
			case CHRD: algo = new Gen(); break;
			case BASS: algo = new Bass(); break;
			case MPK: algo = new MPKTranspose(this); break;
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
		Threads.execute(()-> {
			ModeCombo.update(this);
			MainFrame.miniSeq().update(this);
			MainFrame.getMidiView(this).getMenu().updateMode();
			//JudahZone.getMidiGui().update(this);
		});
	}


	/** externally triggered */
	public void mpk(Midi midi) {
		if (algo instanceof Feed mpk)
			mpk.feed(midi);
	}

	public void arpeggiate(ShortMessage msg) {
		if (chords.isActive() || algo instanceof Ignorant)
			process(msg, chords.getChord());
	}

	private void process(ShortMessage msg, Chord chord) {
		if (chord == null) {
			silence();
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
			midiOut.send(Midi.create(
				input.getCommand(), input.getChannel(), data1, input.getData2()), JudahMidi.ticker());
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
