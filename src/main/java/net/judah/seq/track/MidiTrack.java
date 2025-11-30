package net.judah.seq.track;

import static net.judah.api.MidiClock.MIDI_24;
import static net.judah.util.Constants.NL;
import static org.jaudiolibs.jnajack.JackTransportState.JackTransportNetStarting;

import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.gui.Updateable;
import net.judah.gui.settable.CurrentCombo;
import net.judah.gui.settable.Folder;
import net.judah.gui.widgets.CueCombo;
import net.judah.gui.widgets.CycleCombo;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.RecordWidget;
import net.judah.gui.widgets.TrackAmp;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Meta;
import net.judah.seq.MetaMap;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTools;
import net.judah.seq.automation.CC;
import net.judah.song.Sched;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public abstract class MidiTrack extends Computer implements TimeListener, MidiConstants {

	/** Sequence (for save) */
    protected final Track t;
    protected final int ch;
    protected String name;

	private File file;
    private int resolution;
    private Cue cue = Cue.Bar;
	private Gate gate = Gate.SIXTEENTH;
	private boolean onDeck;
	protected boolean capture;
	protected long recent; // sequencer sweep
	protected MetaMap meta = new MetaMap();
	@Setter protected boolean permanent;

	public MidiTrack(String name, int ch) throws InvalidMidiDataException {
		this.ch = ch;
		this.name = name;
		t = new MidiFile().createTrack();
		setResolution(MIDI_24);
		clock.addListener(this);
	}

	/** publish the note/cc */
	public void send(MidiMessage midi, long ticker) {
		if (midi instanceof MetaMessage)
			return; // already filtered

		ShortMessage m = (ShortMessage)midi;
		if (Midi.isProgChange(midi)) {
			progChange(m.getData1());
			return;
		}
		if (CC.VOLUME.matches(m)) {// filter VOL CC
			setAmp(m.getData2() * Constants.TO_1);
			return;
		} else if (CC.STOP.matches(m)) {
			setActive(false);
			return;
		}

		if (m.getChannel() != ch) // conform to midi channel
			m = Midi.format(m, ch, 1);
	}

	/** if recording currently enabled, add note to track */
	public abstract boolean capture(Midi midi);
    protected abstract void processNote(ShortMessage m);
	protected abstract void parse(Track incoming);

	public abstract String[] getPatches();
	public abstract String progChange(int data1);
	public abstract boolean progChange(String name);

	@Override public boolean equals(Object o) {
    	if (o instanceof MidiTrack)
			return getName().equals(((MidiTrack) o).getName())
					/* && midiOut == ((MidiTrack)o).getMidiOut() */
					&& ch == ((MidiTrack)o).ch;
    	return false;
    }
    @Override public int hashCode() { return getName().hashCode() * ch;}
    @Override public String toString() { return getName(); }
    public final boolean isDrums() { return this instanceof DrumTrack; }
    public final boolean isSynth() { return this instanceof PianoTrack; }

    public void setResolution(int rez) { // TODO
		if (rez < 2 || rez > 2048)
			throw new NumberFormatException("out of bounds");
		float factor = rez / (float)getResolution();
		for (int i = t.size() - 1; i >= 0; i--) {
			t.get(i).setTick((long) (t.get(i).getTick() * factor));
		}
		resolution = rez;
		setBarTicks(clock.getTimeSig().beats * rez);
		compute();
		MainFrame.update((Updateable) () -> {
			if (TabZone.getMusician(MidiTrack.this) != null)
				TabZone.getMusician(MidiTrack.this).timeSig(clock.getTimeSig());
			});
    }

	public long getStepTicks() { return resolution / clock.getSubdivision(); }
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
		// setResolution(MIDI_24); // TODO
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
			MainFrame.update(this);
		}
	}

	public void setActive(boolean on) {
		state.active = on;
		if (state.active && clock.isEven() != (count % 2 == 0))
			cycle();
		onDeck = false;
		MainFrame.update(this);
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

	@Override protected void setCurrent(int change) {
		if (current == change) return;
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
		long newTime = current * barTicks + (long)(percent * resolution);
		if (percent == 0)
			recent = newTime - 1;
		if (state.active)
			processTrack(recent, newTime);
		recent = newTime + 1;
	}

    private void processTrack(long from, long to) {
    	int idx = MidiTools.fastFind(t, from);
    	if (idx < 0)
    		return;
    	for (; idx < t.size(); idx++) {
			MidiEvent e = t.get(idx);
			if (e.getTick() > to) break;
			if (e.getMessage() instanceof ShortMessage m && Midi.isNote(m))
				processNote(Midi.format(m, ch, state.amp)); // malloc
			else
				send(e.getMessage(), JudahMidi.ticker());
		}
    }

    @Override public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if ((int)value == 0)
				reset();
			else if (state.active)
				cycle();
			else if (onDeck && cue == Cue.Bar)
				setActive(true);
			return;
		}
		if (prop == Property.SIGNATURE) {
			barTicks = resolution * ((Signature)value).beats;
			init();
			return;
		}

		if (prop == Property.BOUNDARY && onDeck  && cue == Cue.Loop) {
			setActive(true);
			return;
		}
		if (prop == Property.TRANSPORT && value == JackTransportNetStarting)
			init();
	}

	public File getFolder() {
		return Folders.midi(isDrums() ? Folders.getBeats() : Folders.getSynths()); // getType() == Trax.B ? Folders.getBass() :
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
			MidiFile export = new MidiFile(resolution);
			MidiTools.copy(this, export.createTrack());
			MidiSystem.write(export, MidiFile.TYPE_1, f);
			RTLogger.log(this, getName() + " saved " + f.getName() + " @" + resolution);
		} catch (Exception e) { RTLogger.warn(this, e); }
	}

	public void load() {
		File f = Folders.choose(getFolder());
		if (f != null)
			load(f);
	}

	/** current scene, not Program, not Cue */ // NOT Meta?
	public void load(MidiTrack from) {
		clear();
		setResolution(from.getResolution()); // TODO
//		meta.clear();
		parse(from.t); // TODO will overwrite DEVICE
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
		if (info.getProgram() != null)
			progChange(info.getProgram());
	}

	/** read and parse track patterns from disk (blocks thread) */
	public void load(File f) {
		if (f == null) {
			clear();
			return;
		}
		if (f.isFile() == false) {
			RTLogger.log(getName(), "Missing midi: " + f.getAbsoluteFile());
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
		meta.clear();
		parse(incoming);
		CurrentCombo.update(this);
		setCycle(Cycle.ALL); // updates
	}

	public long quantize(long tick) {
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

	public void setCapture(boolean rec) {
		capture = rec;
		RecordWidget.update(this); // <- runs other updates
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
		sb.append("TrackName: ").append(meta.getString(Meta.TRACK_NAME)).append(NL);
		sb.append("DEVICE: ").append(meta.getString(Meta.DEVICE)).append(NL);

		sb.append("events: ").append(t.size()).append(NL);
		sb.append("length: ").append(  t.ticks() / resolution ).append(" beats ").append(NL);
		sb.append("    ").append(t.ticks()).append(" ticks @ ").append(resolution).append(" resolution").append(NL);
		return sb.toString();
	}

}
