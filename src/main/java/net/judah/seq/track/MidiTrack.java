package net.judah.seq.track;

import static net.judah.api.MidiClock.MIDI_24;
import static net.judah.util.Constants.NL;
import static org.jaudiolibs.jnajack.JackTransportState.JackTransportNetStarting;
import static org.jaudiolibs.jnajack.JackTransportState.JackTransportStopped;

import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;
import net.judah.gui.RecordWidget;
import net.judah.gui.TabZone;
import net.judah.gui.settable.CurrentCombo;
import net.judah.gui.settable.Folder;
import net.judah.gui.widgets.CueCombo;
import net.judah.gui.widgets.CycleCombo;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackAmp;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTools;
import net.judah.seq.Trax;
import net.judah.song.Sched;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public abstract class MidiTrack extends Computer implements TimeListener, MidiConstants {

	/** Sequence (for save) */
	private final MidiFile s;
	private final Trax type;
	private final String name;
    protected final Track t;
    /** parent midi port */
    protected final ZoneMidi midiOut;
	protected final int ch;
    protected final Actives actives;

	private File file;
    protected boolean capture;

	protected long recent; // sequencer sweep
	private Cue cue = Cue.Bar;
	private Gate gate = Gate.SIXTEENTH;
	private boolean onDeck;
	private float gain = 0.9f;

	// PianoTrack
    public MidiTrack(Trax type, ZoneMidi out, int rez, JudahClock clock, int polyphony)  throws InvalidMidiDataException {
    	this(type.name(), type, out, rez, clock, polyphony);
    }

	public MidiTrack(Trax type, ZoneMidi out, JudahClock clock, int polyphony) throws InvalidMidiDataException {
		this(type, out, MIDI_24, clock, polyphony);
	}

    public MidiTrack(String name, Trax type, ZoneMidi out, int rez, JudahClock clock, int polyphony) throws InvalidMidiDataException {
    	super(clock);
		this.type = type;
		this.name = name;
		this.ch = type.getCh();
		this.midiOut = out;
		s = new MidiFile(rez);
		setBarTicks(clock.getMeasure() * s.getResolution());
		t = s.createTrack();
		actives = new Actives(midiOut, ch, polyphony);
		if (type.name().equals(Trax.H2.name()))
			cue = Cue.Hot;
		clock.addListener(this);
    }

    // DrumTrack
	public MidiTrack(Trax type, Actives actives, JudahClock clock) throws InvalidMidiDataException {
		super(clock);
		this.type = type;
		this.name = type == null ? "Import" : type.name();
		this.actives = actives;
		this.midiOut = actives.getMidiOut();
		this.ch = actives.getChannel();
		s = new MidiFile();
		setBarTicks(clock.getMeasure() * s.getResolution());
		t = s.createTrack();
		clock.addListener(this);
	}

	public void send(ShortMessage midi, long ticker) {
		if (midi.getChannel() != ch) // conform to midi channel
			midi = Midi.format(midi, ch, 1);

		if (Midi.isNoteOn(midi)) {
			try { // apply local gain
				midi.setMessage(Midi.NOTE_ON, midi.getChannel(), midi.getData1(), (int)(midi.getData2() * gain));
			} catch (InvalidMidiDataException e) { RTLogger.warn(midi, e); }
		}
		midiOut.send(midi, ticker);
	}

	/** if recording currently enabled, add note to track */
	public abstract boolean capture(Midi midi);
    protected abstract void playNote(ShortMessage m);
	protected abstract void parse(Track incoming);

	@Override public boolean equals(Object o) {
    	if (o instanceof MidiTrack)
    		return name.equals(((MidiTrack)o).getName()) && midiOut == ((MidiTrack)o).getMidiOut() && ch == ((MidiTrack)o).ch;
    	return false;
    }
    @Override public int hashCode() { return name.hashCode() * ch;}
    @Override public String toString() { return name; }
    public final boolean isDrums() { return this instanceof DrumTrack; }
    public final boolean isSynth() { return this instanceof PianoTrack; }
    public void setResolution(int rez) {
		if (rez < 2 || rez > 2048)
			throw new NumberFormatException("out of bounds");
		float factor = rez / (float)getResolution();
		for (int i = t.size() - 1; i >= 0; i--) {
			t.get(i).setTick((long) (t.get(i).getTick() * factor));
		}
    	s.setResolution(rez);
		setBarTicks(clock.getTimeSig().beats * rez);
		compute();
		MainFrame.update(this);
    }
    public int getResolution() { return s.getResolution(); }
	public long getStepTicks() { return s.getResolution() / clock.getSubdivision(); }
    public long getWindow() { return 2 * barTicks; }
	/**@return number of bars with notes recorded into them */
	@Override public int bars() { return MidiTools.measureCount(t.ticks(), barTicks); }

	public void clear() {
		synchronized (t) {
			for (int i = t.size() - 1 ; i >= 0; i--)
				t.remove(t.get(i));
		}
		setFile(null);
		init();
		setResolution(MIDI_24);
		new Panic(this);
	}

    void init() {
		count = 0;
		if (current != state.launch)
			setCurrent(state.launch);
	}

    /** Scene change */
    public void setState(Sched sched) {

		count = clock.isEven() ? 0 : 1;

		offset = 0;
    	boolean previous = state.active;
    	Cycle old = state.cycle;
		state = sched;
		setAmp(state.amp);
		if (state.getProgram() != null && !state.getProgram().equals(midiOut.getProg(ch)))
			midiOut.progChange(state.getProgram(), ch);
		if (old != state.cycle)
			CycleCombo.update(this);
		if (current != state.launch)
			setCurrent(state.launch);
		else
			compute();
		if (previous != state.active)
			setActive(state.active);

    }

	public void toggle() {
		setActive(!isActive());
	}

	public void trigger() {
		if (!clock.isActive())
			setActive(!isActive());
		else if (isActive())
			setActive(false);
		else if (cue == Cue.Hot)
			setActive(true);
		else {
			onDeck = !onDeck;
			MainFrame.update(name);
		}
	}

	public void setActive(boolean on) {
		state.active = on;
		if (state.active && clock.isEven() != (count % 2 == 0))
			cycle();
		onDeck = false;
		MainFrame.update(name);
	}

	public void setAmp(float amp) {
    	if (amp > 1) {
    		RTLogger.warn(this, "Track vol: " + amp);
    		return;
    	}
    	state.amp = amp;
		TrackAmp.update(this);
		TrackVol.update(this);
    }

	@Override
	protected void setCurrent(int change) {
		if (current == change) return;
		flush();
		if (change < 0)
			change = clock.isEven() ? 0 : 1;
		recent = change * barTicks + (recent - current * barTicks);
		current = change;
		compute();
		MainFrame.update(this);
	}

	public void setGate(Gate gate2) {
		gate = gate2;
		GateCombo.refresh(this);
	}

	public final void setCue(Cue cue) {
		this.cue = cue;
		CueCombo.refresh(this);
	}

	private void setFile(File f) {
		this.file = f;
		Folder.update(this);
	}

    public final void playTo(float percent) {
		long newTime = current * barTicks + (long)(percent * s.getResolution());
		if (percent == 0)
			recent = newTime - 1;
		if (state.active)
			playNotes(recent, newTime);
		recent = newTime + 1;
	}

    private void playNotes(long from, long to) {
    	int idx = MidiTools.fastFind(t, from);
    	if (idx < 0)
    		return;
    	for (; idx < t.size(); idx++) {
			MidiEvent e = t.get(idx);
			if (e.getTick() > to) break;
			if (Midi.isNote(e.getMessage()))
		    	playNote(Midi.format((ShortMessage)e.getMessage(), ch, state.amp)); // malloc
		}
    }

    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if ((int)value == 0)
				reset();
			else if (state.active)
				cycle();
			else if (onDeck && cue == Cue.Bar)
				setActive(true);
		}
		else if (prop == Property.SIGNATURE) {
			barTicks = s.getResolution() * ((Signature)value).beats;
			init();
		}
		else if (prop == Property.BOUNDARY && onDeck && cue == Cue.Loop)
			setActive(true);
		else if (prop == Property.TRANSPORT) {
			if (value == JackTransportStopped && isActive())
				new Panic(this);
			else if (value == JackTransportNetStarting)
				init();
		}
	}

	public File getFolder() {
		return Folders.midi(isDrums() ? Folders.getBeats() :
			actives.getPolyphony() == 1 ? Folders.getBass() : Folders.getSynths());
	}

	public void save() {
		if (file == null)
			saveAs();
		else
			save(file);
	}

	public void saveAs() {
		File f = Folders.choose(getFolder());
		if (f != null) {
			setFile(f);
			save(f);
			Folder.refill(this);
		}
	}

	void save(File f) {
		try {
			MidiSystem.write(s, MidiFile.TYPE_1, f);
			RTLogger.log(this, getName() + " saved " + f.getName() + " @" + s.getResolution());
		} catch (Exception e) { RTLogger.warn(this, e); }
	}

	public void load() {
		File f = Folders.choose(getFolder());
		if (f != null)
			load(f);
	}

	/** current scene, not Program, not Cue */
	public void load(MidiTrack from) {
		clear();
		setResolution(from.getResolution());
		parse(from.t);
		Sched other = from.getState();
		setCycle(other.getCycle());
		setCurrent(other.getLaunch());
		setActive(from.isActive());
		from.setActive(false);

		TabZone.edit(this);
	}

	public void load(TrackInfo info) {
		if (info.getFile() != null && !info.getFile().isBlank())
			load(info.getFile());
		setGate(info.getGate());
		setCue(info.getCue());
	}

	/** read and parse track patterns from disk (blocks thread) */
	public void load(File f) {
		if (f == null) {
			clear();
			return;
		}
		if (f.isFile() == false) {
			RTLogger.log(name, "Missing midi: " + f.getAbsoluteFile());
			return;
		}
		try {
			Sequence midiFile = MidiSystem.getSequence(f);
			if (midiFile.getTracks().length == 1) {
				importTrack(midiFile.getTracks()[0], midiFile.getResolution());
				setFile(f);
			}
			else
				new ImportMidi(this, midiFile);
		} catch (Exception e) { RTLogger.warn(this, e); }
	}

	public void load(String name) { load(new File(getFolder(), name)); }

	/** see parse() method in subclasses*/
	public void importTrack(Track incoming, int rez) {

		for (int i = t.size() - 1; i >= 0; i--)
			t.remove(t.get(i)); // clear
		count = clock.isEven() ? 0 : 1;
		setCurrent(0);
		setResolution(rez);
		parse(incoming);

		CurrentCombo.update(this);
		MainFrame.update(this);
	}

	public long quantize(long tick) {
		int resolution = s.getResolution();
		if (clock.getTimeSig().div == 3 && (gate == Gate.SIXTEENTH || gate == Gate.EIGHTH))
			return triplets(tick, resolution);

		switch(gate) {
		case SIXTEENTH: return tick - tick % (resolution / 4);
		case EIGHTH: return tick - tick % (resolution / 2);
		case QUARTER: return tick - tick % resolution;
		case HALF: return tick - tick % (2 * resolution);
		case WHOLE: return tick - tick % (4 * resolution);
		case MICRO: return resolution > 16 ?
				tick - tick % (resolution / 8) : tick - tick % (resolution / clock.getTimeSig().div);
		case FILE : // approx MIDI_24
		case NONE :
		default:
			return tick;
		}
	}

	public long quantizePlus(long tick) {
		int resolution = s.getResolution();
		boolean swing = clock.getTimeSig().div == 3;
		long result = switch (gate) {
			case SIXTEENTH -> quantize(tick) + (swing ? resolution / 6 : resolution / 4);
			case EIGHTH -> 	quantize(tick) + (swing ? resolution / 3 : resolution / 2);
			case QUARTER -> quantize(tick) + (resolution);
			case HALF -> 	quantize(tick) + (2 * resolution);
			case WHOLE -> 	quantize(tick) + (4 * resolution);
			case MICRO -> 	quantize(tick) + (resolution / 8);
			//case NONE: case FILE:  // :	return quantize(tick) + 1/*RATCHET*/;
			default -> 		tick;
		};
		return result == tick ? result : result - 1; // hanging chads
	}

	protected long triplets(long tick, int resolution) {
		if (gate == Gate.SIXTEENTH)
			return tick - tick % (resolution / 6);
		return tick - tick % (resolution / 3);
	}

	public void progChange(String name) {
		if (midiOut.progChange(name, ch))
			state.setProgram(name);
	}

	public void setCapture(boolean rec) {
		capture = rec;
		RecordWidget.update(this);
	}

	/** @return a step in the current midi resolution */
	public long timecode(int bar, int step) {
		return bar * barTicks + step * getStepTicks();
	}

	public String info() {
		StringBuffer sb = new StringBuffer(getName() + " ");
		sb.append(getClass().getSimpleName()).append(" Info").append(NL).append(NL);
		if (getFile() != null && getFile().isFile()) {
			sb.append("File:").append(NL);
			String file = getFile().getAbsolutePath().replace(System.getProperty("user.home"), "~");
			sb.append(file).append(NL);
		}
		sb.append("events: ").append(t.size()).append(NL);
		sb.append("length: ").append(  t.ticks() / s.getResolution() ).append(" beats ").append(NL);
		sb.append("    ").append(t.ticks()).append(" ticks @ ").append(s.getResolution()).append(" resolution").append(NL);
		return sb.toString();
	}

}
