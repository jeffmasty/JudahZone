package net.judah.seq;

import static net.judah.midi.JudahClock.MIDI_24;

import java.io.File;
import java.util.ArrayList;

import javax.sound.midi.*;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Bar;
import net.judah.gui.settable.CueCombo;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Launch;
import net.judah.gui.widgets.FileChooser;
import net.judah.gui.widgets.GateCombo;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.midi.Signature;
import net.judah.seq.arp.Arp;
import net.judah.seq.arp.Mode;
import net.judah.song.Sched;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter 
public class MidiTrack extends Computer implements TimeListener, MidiConstants {
	public static final String SUFFIX = ".mid";
	
	private final JudahClock clock;
	/** Sequence (for save) */
	private final MidiFile s; 
	private final Track t; 
    private final String name;
	private final MidiReceiver midiOut;
	private final int ch;
	private File file;
	Gate gate = Gate.SIXTEENTH;
 	private Cue cue = Cue.Bar; 

	private boolean onDeck; 
	private final ArrayList<ShortMessage> actives = new ArrayList<>();
	private final Arp arp;
	
    /** ch = 0 or 9 (drumkit midiout) 
     * @throws InvalidMidiDataException */
    public MidiTrack(MidiReceiver out, JudahClock clock) throws InvalidMidiDataException {
    	this(out.getName(), out, out instanceof DrumKit ? 9 : 0, MIDI_24, clock);
    }
    
    /** Fluid track at standard Resolution 
     * @throws InvalidMidiDataException */
    public MidiTrack(MidiReceiver out, int ch, JudahClock clock) throws InvalidMidiDataException {
    	this(out.getName() + (ch + 1), out, ch, MIDI_24, clock);
    }
    
    public MidiTrack(String name, MidiReceiver out, int ch, int rez, JudahClock clock) throws InvalidMidiDataException {
		this.name = name;
		this.ch = ch;
		this.midiOut = out;
		this.clock = clock;
		s = new MidiFile(rez);
		t = s.createTrack();
		state = new Sched(isDrums());
		barTicks = clock.getMeasure() * s.getResolution();
		arp = isSynth() ? new Arp(this) :  null;
		clock.addListener(this);
    }
    
	@Override public boolean equals(Object o) {
    	if (false == o instanceof MidiTrack) return false;
    	return name.equals(((MidiTrack)o).getName()) && midiOut == ((MidiTrack)o).getMidiOut() && ch == ((MidiTrack)o).ch; 
    }
    @Override public int hashCode() { return name.hashCode() * ch;}
    @Override public String toString() { return name; }
    public final boolean isDrums() { return ch == 9; }
    public final boolean isSynth() { return ch != 9; }
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

    public void setState(Sched sched) {
		count = 0;
    	boolean previous = state.active;
    	CYCLE old = state.cycle;
		state = sched;
		setAmp(sched.amp);
		if (current != state.launch) {
			setCurrent(state.launch);
			if (isSynth())
				playTo(0f);
		}
		else 
			compute();
		if (old != state.cycle) 
			Cycle.update(this);
		if (previous != state.active && isSynth() && !state.active) 
			Constants.execute(new Panic(midiOut, ch));
		Launch.update(this);
    }
	
	public void setLaunch(int frame) {
		if (state.launch == frame) return;
		state.launch = frame;
		Launch.update(this);
		if (!state.active)
			setCurrent(frame);
	}

	public void setActive(boolean on) {
		onDeck = false;
		state.active = on;
		if (!on && isSynth()) {
			arp.clear();
			Constants.execute(new Panic(midiOut, ch));
		}
		MainFrame.update(this);
	}
	
	public void setAmp(float amp) {
    	state.amp = amp;
    	if (amp > 1) {
    		RTLogger.warn(this, "Track vol: " + amp);
    		return;
    	}
    	TrackVol.update(this);
    }

	@Override
	public void setCurrent(int change) {
		if (current == change) return;
		if (change < 0) 
			change = JudahClock.isEven() ? 0 : 1;
		if (isSynth())
			flush();
		recent = change * barTicks + (recent - current * barTicks);
		current = change;
		compute();
		MainFrame.update(this);
		Bar.update(this);
	}

    public void playTo(float percent) {
		long newTime = current * barTicks + (long)(percent * s.getResolution());
		if (percent == 0) 
			recent = newTime - 1;
		if (state.active) 
			playNotes(recent, newTime);
		recent = newTime + 1;
	}
    
    private void playNotes(long from, long to) {
    	for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < from) continue;
			if (e.getTick() > to) break;
			if (Midi.isNote(e.getMessage())) {
				ShortMessage formatted = Midi.format((ShortMessage)e.getMessage(), ch, state.amp); // malloc
		    	if (isDrums())
		    		midiOut.send(formatted, JudahMidi.ticker());
		    	else if (arp.getMode() == Mode.Off)
		    		midiOut.send(formatted, JudahMidi.ticker());
		    	else  
		    		arp.process(formatted);
			}
		}
    }
    
	private void flush() { // mode check?
		long end = (current + 1) * barTicks;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() <= recent) continue;
			if (e.getTick() > end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOff((ShortMessage)e.getMessage())) 
				midiOut.send(Midi.format((ShortMessage)e.getMessage(), ch, 1), JudahMidi.ticker());
		}
	}

    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if (state.active) 
				cycle();
			else if (onDeck && cue == Cue.Bar) {
				setActive(true);
			}
		}
		else if (prop == Property.SIGNATURE) {
			barTicks = s.getResolution() * ((Signature)value).beats; 
			init();
		}
		else if (onDeck && prop == Property.LOOP) {
			setActive(true);
		}
		else if (prop == Property.TRANSPORT) {
			if (value == JackTransportState.JackTransportStopped && isActive())
				new Panic(midiOut, ch).run();
			else if (value == JackTransportState.JackTransportNetStarting)
				init();
		}
	}
    
//	/** copy measure to end of track */
//	public void copyFrame(int frame) {
//		long start = frame * getWindow();
//		ArrayList<MidiEvent> work = new ArrayList<>();
//		MidiTools.loadSection(start, start + getWindow(), t, work);
//		long transfer = frames() * getWindow();
//		for (MidiEvent e : work)
//			if (e.getMessage() instanceof ShortMessage)
//				t.add(new MidiEvent(Midi.copy((ShortMessage)e.getMessage()), e.getTick() + transfer));
//		setFrame(frames());
//		MainFrame.update(this);
//	}
//
//	/** remove notes and update state */
//	public void deleteFrame(int frame) {
//		long start = frame * getWindow();
//		long end = start + getWindow();
//		long subtract = getWindow();
//		for (int i = 0; i < t.size(); i++) {
//			MidiEvent e = t.get(i);
//			if (e.getTick() < start) continue;
//			if (e.getTick() >= end)
//				e.setTick(e.getTick() - subtract);
//			else {
//				t.remove(e);
//				i--;
//			}
//		}
//		
//		if (getFrame() > frame)
//			setFrame(getFrame() - 1);
//		else if (getFrame() == frame) {
//			if (getFrame() - 1 < 0)
//				setFrame(0);
//			else setFrame(getFrame() - 1);
//		}
//		compute();
//		MainFrame.update(this);
//	}
	
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
		Constants.execute(new Panic(midiOut, ch));
		synchronized (t) {
			for (int i = t.size() -1; i >= 0; i--)
				t.remove(t.get(i));
		}
		init();
		s.setResolution(MIDI_24);
		barTicks = clock.getTimeSig().beats * s.getResolution();
		setFile(null);
		MainFrame.update(this);
	}
	
	public File getFolder() {
		return Folders.midi(isSynth() ? midiOut.isMono() ? Folders.getBass() : Folders.getSynths() : Folders.getBeats());
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
		Constants.execute(new Panic(midiOut, ch));
		if (isSynth())
			arp.clear();
		
		s.setResolution(rez);
		this.barTicks = clock.getTimeSig().beats * rez;
		// notes
		for (int i = 0; i < incoming.size(); i++) 
			t.add(incoming.get(i));
		
		count = 0;
		setCurrent(0);
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
		
		setCurrent(state.launch);
		if (!clock.isActive()) 
			setActive(!isActive());
		else if (isActive()) {
			setActive(false);
		} else if (cue == Cue.Hot)
			setActive(true);
		else {
			onDeck = !onDeck;
			MainFrame.update(this);
		}
	}
	
	// TODO odd/swing subdivision
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
		switch(gate) {
		case SIXTEENTH: return quantize(tick) + (resolution / 4);
		case EIGHTH:	return quantize(tick) + (resolution / 2);
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

	public void next(boolean fwd) {
		setCurrent(fwd ? after(after(current)) : before(before(current)));
		if (isSynth())
			arp.clear();
	}

}
