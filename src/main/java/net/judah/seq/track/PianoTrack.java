package net.judah.seq.track;

import static org.jaudiolibs.jnajack.JackTransportState.JackTransportStopped;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Algo;
import judahzone.api.Chord;
import judahzone.api.ControlChange;
import judahzone.api.Midi;
import judahzone.api.MidiOut.MidiPiano;
import judahzone.api.Notification.Property;
import judahzone.api.TimeListener;
import judahzone.data.Key;
import judahzone.util.Notes;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.ArpInfo;
import net.judah.seq.arp.Down;
import net.judah.seq.arp.Ethereal;
import net.judah.seq.arp.Feed;
import net.judah.seq.arp.Ignorant;
import net.judah.seq.arp.MPKTranspose;
import net.judah.seq.arp.RND;
import net.judah.seq.arp.Racman;
import net.judah.seq.arp.Up;
import net.judah.seq.arp.UpDown;
import net.judah.seq.chords.ChordListener;
import net.judah.seq.chords.Chords;
import net.judah.seq.piano.Pedal;
import net.judah.seq.track.Edit.Type;
import net.judah.song.Sched;

/** A melodic MidiTrack with an Arpeggiator */
public class PianoTrack extends NoteTrack implements ChordListener {

	public static int DEFAULT_POLYPHONY = 24;
	public static int MONOPHONIC = 1;

	@Getter private ArpInfo info = new ArpInfo();
	private final Chords chords;
	private final List<Integer> workArea = new ArrayList<Integer>();
	private Algo algo = new Echo();
	private final ArrayList<MidiEvent> chads = new ArrayList<>();
	@Getter private final Pedal pedal;
	@Getter final private Notes notes; // TODO final

	public PianoTrack(String name, int ch, Channel channel, MidiPiano gated, Chords chords) {
	    super(name, ch, channel, gated);
	    this.chords = chords;
	    if (this.chords != null)
	        this.chords.addListener(this);
	    pedal = new Pedal(this, gated);
	    notes = gated.getNotes();
	    if (notes == null)
	    	throw new NullPointerException(name + ": PianoTrack requires MidiPiano.Notes " + gated);
	}

	public PianoTrack(String name, NoteTrack source) {
	    this(name, source.getCh(), source.getChannel(), (MidiPiano) source.getMidiOut(), null);
	}

	public void panic() {
	    ((MidiPiano) midiOut).panic();
	}

	@Override
	public int[] getActiveSnapshot() {
	    return notes == null ? new int[0] : notes.actives();
	}

	@Override
	public void load(TrackInfo info) {
	    super.load(info);
	    if (info.getArp() != null) // overkill? Sched has arpinfo.
	        setInfo(info.getArp());
	}

	@Override
	protected void cycle() {
	    flush();
	    super.cycle();
	}

	protected boolean filterPiano(MidiMessage m) {
	    if (m instanceof ShortMessage midi) {
	        if (ControlChange.PEDAL.matches(midi)) {
	            pedal.setPressed(midi.getData2() > Midi.CUTOFF);
	            return true;
	        }
	        if (ControlChange.PANIC.matches(midi)) {
	            pedal.setPressed(false);
	            Threads.execute(() -> panic()); // no loops on Panic(), the class
	            return true;
	        }
	    }
	    return false;
	}

	// filter PEDAL HOLD CC
	@Override
	public void send(MidiMessage m, long ticker) {
	    if (filterPiano(m))
	        return;
	    super.send(m, ticker);
	}

	@Override
	public void setState(Sched sched) {
	    super.setState(sched);
	    if (sched.getArp() != info && sched.getArp() != null)
	        setInfo(sched.getArp());
	    if (sched.active && clock.isActive() && clock.getBeat() == 0)
	        playTo(0); // kludge
	}

	@Override
	public void setActive(boolean on) {
	    if (!on) {
	    	panic();
	        // clear stored arp mappings
	        if (notes != null) notes.clearMappings();
	    }
	    super.setActive(on);
	}

	@Override
	public void update(Property prop, Object value) {
	    super.update(prop, value);
	    if (prop == Property.TRANSPORT && value == JackTransportStopped && isActive()) {
	        if (((MidiPiano) midiOut).isPedal())
	            silence(); // pedal-aware
	        else
	            panic();
	    }
	}

	@Override
	public void next(boolean fwd) {
	    silence();
	    super.next(fwd);
	}

	@Override
	protected void parse(Track incoming) {

	    panic(); // not part of parse process
	    // clear mappings when re-parsing
	    if (notes != null) notes.clearMappings();

	    for (int i = 0; i < incoming.size(); i++) {
	        MidiEvent e = incoming.get(i);
	        if (e.getMessage() instanceof ShortMessage orig)
	            t.add(new MidiEvent(Midi.create(orig.getCommand(), ch, orig.getData1(), orig.getData2()), e.getTick()));
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
	    } else if (Midi.isNoteOff(m)) {
	        midiOut.send(m, JudahMidi.ticker());
	        MidiEvent used = null;
	        for (MidiEvent on : chads) {
	            if (((ShortMessage) on.getMessage()).getData1() != m.getData1())
	                continue;
	            used = on;
	            editor.push( // TODO atomic
	                    new Edit(Type.NEW, on, new MidiEvent(m, tick)));
	            break;
	        }
	        if (used != null)
	            chads.remove(used);
	    }
	    // TODO CC, progchange, pitchbend
	    else
	        return false;
	    return true;
	}

	@Override
	public void clear() {
	    silence();
	    ((MidiPiano) midiOut).panic();
	    super.clear();
	}

	public int getRange() {
	    return info.getRange();
	}

	public void setRange(int range) {
	    info.range = range;
	    if (algo != null)
	        algo.setRange(range);
	    MainFrame.updateTrack(Update.RANGE, this);
	}

	@Override
	public void chordChange(Chord from, Chord to) {
	    if (to == null)
	        silence();
	    if (!isActive() || algo instanceof Ignorant)
	        return;
	    if (algo != null)
	        algo.change();

	    // Use Notes mappings snapshot to send offs for current chord
	    List<Notes.Mapping> snap = notes != null ? notes.mappingsSnapshot() : List.of();
	    for (Notes.Mapping item : snap) {
	        ShortMessage midi = item.key();
	        Midi off = Midi.create(Midi.NOTE_OFF, midi.getChannel(), midi.getData1(), midi.getData2());
	        process(off, from);
	    }
	    for (Notes.Mapping item : snap) {
	        process(item.key(), to);
	    }

	}

	protected void flush() { // mode check?
	    long end = (current + 1) * barTicks;
	    for (int i = 0; i < t.size(); i++) {
	        MidiEvent e = t.get(i);
	        if (e.getTick() <= recent)
	            continue;
	        if (e.getTick() > end)
	            break;
	        if (e.getMessage() instanceof ShortMessage m && Midi.isNoteOff(e.getMessage()))
	            midiOut.send(Midi.format(m, ch, 1), JudahMidi.ticker());
	    }
	    silence();
	}

	@Override
	protected void setCurrent(int change) {
	    if (current == change)
	        return;
	    flush();
	    super.setCurrent(change);
	}

	/** clear the Arpeggiator */
	private void silence() {
	    if (notes != null) {
	        List<Notes.Mapping> snap = notes.mappingsSnapshot();
	        for (Notes.Mapping mm : snap) {
	            ShortMessage midi = mm.key();
	            out(Midi.create(Midi.NOTE_OFF, midi.getChannel(), midi.getData1(), midi.getData2()), mm.values());
	        }
	        notes.clearMappings();
	    }
	}

	public void setInfo(ArpInfo arp) {
	    boolean setMode = info.algo != arp.algo;
	    info = arp;
	    if (setMode)
	        setArp(info.algo);
	    else
	        silence();
	    if (algo != null) // updates?
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
	    if (info.getAlgo() != Arp.Off && isCapture())
	        setCapture(false);
	}

	public void setArp(Arp mode) {
	    if (algo instanceof TimeListener)
	        clock.removeListener((TimeListener) algo);
	    silence();
	    // new Panic(track);
	    info.algo = mode;
	    switch (info.algo) {
	    case Off:
	        algo = new Echo();
	        break; // not used
	    case CHRD:
	        algo = new Gen();
	        break;
	    case BASS:
	        algo = new Bass();
	        break;
	    case MPK:
	        algo = new MPKTranspose(this);
	        break;
	    case ABS:
	        algo = new ABS();
	        break;
	    case UP:
	        algo = new Up();
	        break;
	    case DWN:
	        algo = new Down();
	        break;
	    case UPDN:
	        algo = new UpDown(true);
	        break;
	    case DNUP:
	        algo = new UpDown(false);
	        break;
	    case RND:
	        algo = new RND();
	        break;
	    case RACM:
	        algo = new Racman();
	        break;
	    case ETH:
	        algo = new Ethereal();
	        break;
	    // case REC: algo = new REC(track); break;
	    // case REL: algo = new REL(); break;
	    // case UP5: case DN5:
	    }
	    algo.setRange(info.range);
	    MainFrame.updateTrack(Update.ARP, this);
	}

	/** externally triggered */
	public void mpk(Midi midi) {
	    if (algo instanceof Feed mpk)
	        mpk.feed(midi);
	}

	@Override
	protected void processNote(ShortMessage msg) {
	    if (info.getAlgo() == Arp.Off)
	        midiOut.send(msg, JudahMidi.ticker());
	    else
	        arpeggiate(msg);
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
	        workArea.clear();
	        algo.process(msg, chord, workArea);
	        if (!workArea.isEmpty()) {
	            // store mapping in Notes so we can release those outputs on the corresponding input note-off
	            if (notes != null)
	                notes.addMapping(msg, new ArrayList<>(workArea));
	            out(msg, workArea);
	        }
	    } else if (Midi.isNoteOff(msg)) {
	        // release outputs associated with this input message
	        List<Integer> outs = notes != null ? notes.removeMapping(msg) : null;
	        out(msg, outs);
	    }
	}

	private void out(ShortMessage input, List<Integer> work) {
	    if (work == null)
	        return;
	    for (Integer data1 : work)
	        midiOut.send(Midi.create(input.getCommand(), input.getChannel(), data1, input.getData2()),
	                JudahMidi.ticker());
	}

	public class Echo extends Algo implements Ignorant {
	    @Override
	    public void process(ShortMessage m, Chord chord, List<Integer> result) {
	        result.add(m.getData1());
	    }
	}

	public class Gen extends Algo {
	    @Override
	    public void process(ShortMessage m, Chord chord, List<Integer> result) {
	        if (range < 13)
	            chord.tight(m.getData1(), result);
	        else
	            chord.wide(m.getData1(), result);
	    }
	}

	public class Bass extends Algo {
	    @Override
	    public void process(ShortMessage m, Chord chord, List<Integer> result) {
	        result.add(m.getData1() + Key.key(m.getData1()).interval(chord.getBass()));
	    }
	}

	/** translate root of chord by off from middle C of m.getData1() */
	public class ABS extends Algo implements Ignorant {
	    @Override
	    public void process(ShortMessage m, Chord chord, List<Integer> result) {
	        result.add(m.getData1() + chord.getRoot().ordinal());
	    }
	}

	}