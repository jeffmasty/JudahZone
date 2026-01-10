package net.judah.seq.track;

import static judahzone.api.MidiClock.MIDI_24;
import static judahzone.util.Constants.NL;
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
import javax.swing.JOptionPane;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.api.Notification.Property;
import judahzone.api.Signature;
import judahzone.api.TimeListener;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.midi.JudahMidi;
import net.judah.seq.Meta;
import net.judah.seq.MetaMap;
import net.judah.seq.automation.Automation;
import net.judah.seq.automation.ControlChange;
import net.judah.song.Sched;

@Getter
public abstract class MidiTrack extends Computer implements TimeListener, MidiConstants {

    protected final int ch;
    @Setter protected String name;
	private File file;
    protected Cue cue = Cue.Bar;
	private boolean onDeck;
	protected boolean capture;
	@Setter protected boolean permanent;
	@Getter protected final Editor editor;
	protected Automation automation;

	public MidiTrack(String name, int ch) throws InvalidMidiDataException {
		this.ch = ch;
		this.name = name;
		clock.addListener(this);
		editor = new Editor(this);
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
		if (ControlChange.VOLUME.matches(m)) {// filter VOL CC
			setAmp(m.getData2() * Constants.TO_1);
			return;
		} else if (ControlChange.STOP.matches(m)) {
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
	public abstract Channel getChannel();

	public String getProgram() {
		return state.getProgram();
	}

	@Override public boolean equals(Object o) {
    	if (o instanceof MidiTrack other)
			return getName().equals(other.getName())
					&& getChannel() == other.getChannel()
					&& ch == other.ch;
    	return false;
    }
    @Override public int hashCode() { return ch + getName().hashCode();}
    @Override public String toString() { return getName(); }
    public final boolean isDrums() { return this instanceof DrumTrack; }
    public final boolean isSynth() { return this instanceof PianoTrack; }

    public Automation getAutomation () { // lazy load
		if (automation == null)
			automation = new Automation(this);
		return automation;
    }

	public void clear() {
		init();
		synchronized (t) {
			for (int i = t.size() - 1 ; i >= 0; i--)
				t.remove(t.get(i));
		}
		resolution = MIDI_24;
		setBarTicks(clock.getTimeSig().beats * resolution);
		compute();
		setFile(null);
	}

    void init() {
		count = clock.isEven() ? 0 : 1;
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
			MainFrame.updateTrack(Update.CYCLE, this);
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
			MainFrame.updateTrack(Update.PLAY, this);
		}
	}

	public void setActive(boolean on) {
		state.active = on;
		if (state.active && clock.isEven() != (count % 2 == 0))
			cycle();
		onDeck = false;
		MainFrame.updateTrack(Update.PLAY, this);
	}

	public void setAmp(float amp) {
    	if (amp > 1 || amp < 0) {
    		RTLogger.warn(this, "Track vol: " + amp);
    		return;
    	}
    	state.amp = amp;
    	MainFrame.updateTrack(Update.AMP, this);
    }

	public final void setCue(Cue cue) {
		this.cue = cue;
		MainFrame.updateTrack(Update.CUE, this);
	}

	private void setFile(File f) {
		this.file = f;
		MainFrame.updateTrack(Update.FILE, this);
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
    	int idx = MidiTools.find(t, from);
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
		return Folders.midi(isDrums() ? Folders.getBeats() : Folders.getSynths(), clock.getTimeSig());
		// getType() == Trax.B ? Folders.getBass() :
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
			MainFrame.updateTrack(Update.REFILL, this);
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
//		meta = new MetaMap(from.t); // TODO
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
		setCue(info.getCue());
		if (info.getProgram() != null)
			progChange(info.getProgram());
		if (this instanceof NoteTrack notes)
			notes.setGate(info.getGate());
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


	public void importTrack(MetaMap map, int rez) {
		clear();
		if (map.containsKey(Meta.TRACK_NAME))
			name = map.getString(Meta.TRACK_NAME);
		if (map.containsKey(Meta.CUE)) {
			try {
			setCue(Cue.valueOf(map.getString(Meta.CUE)));
			} catch (IllegalArgumentException e) { RTLogger.log(this, "invalid cue: " + map.getString(Meta.CUE)); }
		}

		setResolution(rez);
		parse(map.t);
	}

	/** see parse() method in subclasses*/
	public void importTrack(Track incoming, int rez) {
		clear();
		resolution = rez;
		setBarTicks(clock.getTimeSig().beats * resolution);
		parse(incoming);
		compute();
		setCycle(Cycle.ALL); // updates
	}

	public void setCapture(boolean rec) {
		capture = rec;
		MainFrame.updateTrack(Update.CAPTURE, this);
	}

	/** @return a step in the current midi resolution */
	public long timecode(int bar, int step) {
		return bar * barTicks + step * getStepTicks();
	}

	public void info() {

		StringBuffer sb = new StringBuffer(getName() + " ");
		sb.append(getClass().getSimpleName()).append(" Info").append(NL).append(NL);
		if (getFile() != null && getFile().isFile()) {
			sb.append("File:").append(NL);
			String file = getFile().getAbsolutePath().replace(System.getProperty("user.home"), "~");
			sb.append(file).append(NL);
		}
		sb.append("TrackName: ").append(name).append(" ch ").append(ch);
		if (this instanceof NoteTrack notes)
			sb.append(" on ").append(notes.midiOut);
		sb.append(NL);
		sb.append("events: ").append(t.size()).append(NL);
		sb.append("length: ").append(  t.ticks() / resolution ).append(" beats ").append(NL);
		sb.append("    ").append(t.ticks()).append(" ticks @ ").append(resolution).append(" resolution").append(NL);
		String result = JOptionPane.showInputDialog(null,
				sb.toString() + "New Resolution:", getResolution());
		if (result == null)
			return;
		try {
			setResolution(Integer.parseInt(result));
		} catch (NumberFormatException e) { RTLogger.log("Resolution", result + ": " + e.getMessage()); }
	}

}
