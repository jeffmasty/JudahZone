package net.judah.seq.track;

import static net.judah.midi.JudahClock.MIDI_24;

import java.io.File;

import javax.sound.midi.*;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.drumkit.KitMode;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.CurrentCombo;
import net.judah.gui.settable.Folder;
import net.judah.gui.widgets.*;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiTools;
import net.judah.song.Sched;
import net.judah.synth.Polyphony;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter 
public abstract class MidiTrack extends Computer implements TimeListener, MidiConstants {
	public static final String SUFFIX = ".mid";
	
	private final JudahClock clock;
	/** Sequence (for save) */
	private final MidiFile s; 
	protected final Track t; 
    private final String name;
	
    /** parent midi port */
    protected final MidiReceiver midiOut; 
	protected final int ch;
    protected final Actives actives;
	
	private File file;
	protected long recent; // sequencer sweep
	private Cue cue = Cue.Bar; 
	private Gate gate = Gate.SIXTEENTH;
	private boolean onDeck; 
	private float gain = 0.9f;
	
    public MidiTrack(String name, MidiReceiver out, final int ch, int rez, JudahClock clock, int polyphony) throws InvalidMidiDataException {
		this.name = name;
		this.ch = ch;
		this.midiOut = out;
		this.clock = clock;
		s = new MidiFile(rez);
		setBarTicks(clock.getMeasure() * s.getResolution());
		t = s.createTrack();
		if (name.equals(KitMode.Fills.name()))
			cue = Cue.Hot;
		
		actives = new Actives(midiOut, ch, polyphony);
		clock.addListener(this);
    }
    
	public MidiTrack(String name, Polyphony notes, JudahClock clock) throws InvalidMidiDataException {
		this.name = name;
		this.ch = notes.getChannel();
		this.midiOut = notes.getMidiOut();
		this.clock = clock;
		s = new MidiFile(JudahClock.MIDI_24);
		setBarTicks(clock.getMeasure() * s.getResolution());
		t = s.createTrack();
		actives = notes;
		clock.addListener(this);
	}

	@Override public boolean equals(Object o) {
    	if (o instanceof MidiTrack) 
    		return name.equals(((MidiTrack)o).getName()) && midiOut == ((MidiTrack)o).getMidiOut() && ch == ((MidiTrack)o).ch;
    	return false;
    }
    @Override public int hashCode() { return name.hashCode() * ch;}
    @Override public String toString() { return name; }
    public final boolean isDrums() { return ch == DRUM_CH; }
    public final boolean isSynth() { return ch != DRUM_CH; }
    public void setResolution(int rez) { 
    	s.setResolution(rez); 
		setBarTicks(clock.getTimeSig().beats * rez);
		compute();
		MainFrame.update(this);
    }
    public int getResolution() { return s.getResolution(); }
    public long getWindow() { return 2 * barTicks; }
	public long getStepTicks() { return s.getResolution() / clock.getSubdivision(); }
	/**@return number of bars with notes recorded into them */
	@Override public int bars() { return MidiTools.measureCount(t.ticks(), barTicks); }
	
    void init() {
		count = 0;
		if (current != state.launch)
			setCurrent(state.launch);
	}

    /** Scene change */
    public void setState(Sched sched) {
    	
		count = JudahClock.isEven() ? 0 : 1;
		
		offset = 0;
    	boolean previous = state.active;
    	Cycle old = state.cycle;
		state = sched;
		setAmp(state.amp);
		if (previous != state.active)
			setActive(state.active);
		if (state.getProgram() != null && !state.getProgram().equals(midiOut.getProg(ch))) 
			midiOut.progChange(state.getProgram(), ch);
		if (old != state.cycle) 
			CycleCombo.update(this);
		if (current != state.launch) 
			setCurrent(state.launch);
		else 
			compute();
    }
	
	public void setActive(boolean on) {
		onDeck = false;
		if (JudahClock.isEven() != (count % 2 == 0))
			cycle();
		state.active = on;
		MainFrame.update(this);
	}
	
	public void setAmp(float amp) {
    	state.amp = amp;
    	if (amp > 1) {
    		RTLogger.warn(this, "Track vol: " + amp);
    		return;
    	}
    	Constants.execute(()->{
    		TrackAmp.update(this);
    		TrackVol.update(this);});
    }

	@Override
	protected void setCurrent(int change) {
		if (current == change) return;
		flush();
		if (change < 0) 
			change = JudahClock.isEven() ? 0 : 1;
		recent = change * barTicks + (recent - current * barTicks);
		current = change;
		compute();
		MainFrame.update(this);
	}

    public void playTo(float percent) {
		long newTime = current * barTicks + (long)(percent * s.getResolution());
		if (percent == 0) 
			recent = newTime - 1;
		if (state.active) 
			playNotes(recent, newTime);
		recent = newTime + 1;
	}
    
    protected abstract void playNote(ShortMessage m);
    
    private void playNotes(long from, long to) {
    	for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < from) continue;
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
		else if (prop == Property.LOOP && onDeck && cue == Cue.Loop) 
			setActive(true);
		else if (prop == Property.TRANSPORT) {
			if (value == JackTransportState.JackTransportStopped && isActive())
				new Panic(this);
			else if (value == JackTransportState.JackTransportNetStarting)
				init();
		}
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

	public void clear() {
		synchronized (t) {
			for (int i = t.size() -1; i >= 0; i--)
				t.remove(t.get(i));
		}
		setFile(null);
		init();
		setResolution(MIDI_24);
		new Panic(this);
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
		File f = FileChooser.choose(getFolder());
		if (f != null) {
			setFile(f);
			save(f);
			Folder.refill(this);
		}
	}
	
	public void save(File f) {
		try {
			MidiSystem.write(s, MidiFile.TYPE_1, f); // MidiSystem.getMidiFileTypes(s)[0]
			RTLogger.log(this, getName() + " saved " + f.getName() + " @" + s.getResolution());
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void load() {
		File f = FileChooser.choose(getFolder());
		if (f != null) 
			load(f);
	}

	/** read and parse track patterns from disk (blocks thread) */
	public void load(File f) {
		clear();
		if (f == null || f.isFile() == false) {
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

	public void importTrack(Track incoming, int rez) {
		for (int i = t.size() -1; i >= 0; i--)
			t.remove(t.get(i)); // clear
		setResolution(rez);

		
//		if (isDrums()) {
//			for (int i = 0; i < incoming.size(); i++) {
//				MidiEvent e = incoming.get(i);
//				if (Midi.isNoteOn(e.getMessage())) {
//					int data1 = ((ShortMessage)e.getMessage()).getData1();
//					if (DrumType.index(data1) >= 0) 
//						t.add(e);
//					else if (DrumType.alt(data1) < 0) {
//						int newVal = data1 % 6 + 2; // skip bass and snare
//						ShortMessage orig = (ShortMessage)e.getMessage();
//						t.add(new MidiEvent(Midi.create(
//								orig.getCommand(), orig.getChannel(), DrumType.values()[newVal].getData1(), orig.getData2()), e.getTick()));
//						RTLogger.log(this, "remap'd " + data1 + " " + GMDrum.lookup(data1) + " to " + DrumType.values()[newVal]);
//					}
//					else {
//						ShortMessage orig = (ShortMessage)e.getMessage();
//						t.add(new MidiEvent(Midi.create(
//								orig.getCommand(), orig.getChannel(), DrumType.alt(data1), orig.getData2()), e.getTick()));
//					}
//				} // else t.add(e);
////			}
//		} else {
//			for (int i = 0; i < incoming.size(); i++) 
//				t.add(incoming.get(i));
//		}
		count = 0;
		setCurrent(0);
		CurrentCombo.update(this);
		MainFrame.update(this);
		
	}

	public MidiEvent get(int cmd, int data1, long tick) {
		for (int i = 0; i < t.size(); i++) {
			if (t.get(i).getTick() > tick) return null;
			if (t.get(i).getTick() < tick) continue;
			if (t.get(i).getMessage() instanceof ShortMessage) {
				MidiEvent e = t.get(i);
				ShortMessage m = (ShortMessage)e.getMessage();
				if (m.getCommand() == cmd && m.getData1() == data1)
					return e;
			}
		}
		return null;
	}

	public void trigger() {
		if (!clock.isActive()) 
			setActive(!isActive());
		else if (isActive()) {
			setActive(false);
		} else if (cue == Cue.Hot)
			setActive(true);
		else {
			onDeck = !onDeck;
		}
		MainFrame.update(this);
	}
	
	public long quantize(long tick) {
		int resolution = s.getResolution();
		if (clock.getTimeSig().div == 3 && (gate == Gate.SIXTEENTH || gate == Gate.EIGHTH))
			return swing(tick, resolution);
		
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
		
		switch(gate) {
		case SIXTEENTH: return quantize(tick) + (swing ? resolution / 6 : resolution / 4);
		case EIGHTH:	return quantize(tick) + (swing ? resolution / 3 : resolution / 2);
		case QUARTER:	return quantize(tick) + (resolution);
		case HALF:		return quantize(tick) + (2 * resolution);
		case WHOLE: 	return quantize(tick) + (4 * resolution);
		case MICRO:		return quantize(tick) + (resolution / 8);
		case NONE: case FILE:  // :	return quantize(tick) + 1/*RATCHET*/;
		default: return tick;
		}
	}

	protected long swing(long tick, int resolution) {
		if (gate == Gate.SIXTEENTH) 
			return tick - tick % (resolution / 6);
		return tick - tick % (resolution / 3);
	}


	public void progChange(String name) {
		if (midiOut.progChange(name, ch))
			state.setProgram(name);
	}

}
