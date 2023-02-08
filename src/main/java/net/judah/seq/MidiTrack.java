package net.judah.seq;

import java.io.File;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.MidiReceiver;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Bar;
import net.judah.gui.settable.Cue;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Launch;
import net.judah.gui.widgets.FileChooser;
import net.judah.gui.widgets.TrackVol;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.song.Sched;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class MidiTrack implements TimeListener, MidiConstants {
	@Getter private final Track t;
	private final Sequence s;
    @Getter private final String name;
	@Getter private final JudahClock clock;
	@Getter private final MidiReceiver midiOut;
	@Getter private final int ch;
	@Getter private final File folder;
	@Getter private File file;
    @Getter private int resolution = 256;
    @Getter private long barTicks;
 	@Getter private boolean onDeck; 
	@Getter private boolean recording; // TODO
 	@Getter private CUE cue = CUE.Bar; 
 	@Getter @Setter private boolean live;

 	@Getter private Sched state;
	private int current; // current measure/bar (not frame)
	@Getter private long left; // left bar's computed start tick 
	@Getter private long right; // right bar's computed start tick
	private int count; // increment bar cycle
	private long oldTime; // sequencer sweep
	
    /** 16-step Drum track */
    public MidiTrack(MidiReceiver out, JudahClock clock) throws Exception {
    	this(out.getName(), out, out instanceof DrumKit ? 9 : 0, out instanceof DrumKit ? 4 : RESOLUTION, clock);
    }
    
    /** Synth track at standard Resolution */
    public MidiTrack(MidiReceiver out, int ch, JudahClock clock) throws Exception {
    	this(out.getName() + ch, out, ch, RESOLUTION, clock);
    }
    
    public MidiTrack(String name, MidiReceiver out, int ch, int rez, JudahClock clock) throws Exception {
		this.name = name;
		this.ch = ch;
		this.midiOut = out;
		this.clock = clock;
		this.resolution = rez;
		state = new Sched(isDrums());
		barTicks = clock.getMeasure() * resolution;
		s = new Sequence(Sequence.PPQ, resolution, 0);
		t = s.createTrack();
		folder = new File(Folders.getMidi(), name);
		if (folder.isDirectory() == false)
			folder.mkdir(); // inelegant?
		clock.addListener(this);
    }
    
    @Override public boolean equals(Object o) {
    	if (false == o instanceof MidiTrack) return false;
    	return name.equals(((MidiTrack)o).getName()) && midiOut == ((MidiTrack)o).getMidiOut() && ch == ((MidiTrack)o).ch; 
    }
    @Override public int hashCode() { return name.hashCode();}
    public boolean isActive() { return state.active; }
    public CYCLE getCycle() { return state.cycle; }
    public int getLaunch() { return state.launch; }
    public float getAmp() { return state.amp; }
    public final boolean isDrums() { return ch == 9; }
    public final boolean isSynth() { return ch != 9; }
	public boolean isEven() { return count % 2 == 0; }
    @Override public String toString() { return name; }
    public long getWindow() { return 2 * barTicks; }
	public int getFrame() { return current / 2; }
	/**@return number of bars with notes recorded into them */
	public int bars() { return MidiTools.measureCount(t.ticks(), barTicks); }
	/**@return number of frames with notes recorded into them */
	public int frames() { return MidiTools.measureCount(t.ticks(), 2 * barTicks); }

    void init() {
		count = 0;
		if (getFrame() != state.launch)
			setFrame(state.launch);
	}

    public void setState(Sched sched) {
    	boolean previous = state.active;
    	CYCLE old = state.cycle;
    	count = 0;
		while (sched.amp > 1) // legacy convert int to float
			sched.amp *= 0.01f;
		setAmp(sched.amp);
		state = sched;
		if (old != state.cycle) {
			count = 0;
			Cycle.update(this);
		}
		if (previous != state.active) {
			if (state.active) {
				setFrame(state.launch);
				if (clock.isActive() && isSynth())
					playTo(0f);
			}
			else {
				new Panic(midiOut, ch).start();
			}
		}
		if (getFrame() != state.launch)
			setFrame(state.launch);
		Launch.update(this);
    }
	
	public void setLaunch(int frame) {
		if (state.launch == frame)
			return;
		state.launch = frame;
		Launch.update(this);
		if (!state.active)
			setFrame(frame);
	}

	public void setActive(boolean on) {
		onDeck = false;
		state.active = on;
		if (on)
			init();
		else 
			new Panic(midiOut, ch).start();
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

	public void setCycle(CYCLE x) {
		state.cycle = x;
		count = isEven() ? 0 : 1;
		if (!clock.isActive())
			setFrame(state.launch);
		Cycle.update(this);
	}

	public void setFrame(int window) {
		setCurrent(window * 2 + (isEven() ? 0 : 1));
	}
    
	private void setCurrent(int change) {
		if (isSynth()) 
			flush();
		if (change < 0) 
			change = 0;
		current = change;
		compute();
		MainFrame.update(this);
		Bar.update(this);
	}

    public void playTo(float percent) {
    	if (!state.active && !live)
			return;

		long newTime = current * barTicks + (long)(percent * resolution);
		if (percent == 0) {
			oldTime = newTime;
			if (isSynth()) 
				oldTime--;
		}
		if (!state.active) {
			oldTime = newTime + 1;
			return;
		}

		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < oldTime) continue;
			if (e.getTick() > newTime) break;
			if (e.getMessage() instanceof ShortMessage) 
				getMidiOut().send(
						Midi.format((ShortMessage)e.getMessage(), ch, state.amp), 
						JudahMidi.ticker());
		}
		oldTime = newTime + 1;
	}
	
	private void flush() {
		long end = (current + 1) * barTicks;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() <= oldTime) continue;
			if (e.getTick() > end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOff((ShortMessage)e.getMessage())) {
				midiOut.send(Midi.format((ShortMessage)e.getMessage(), ch, 1), JudahMidi.ticker());
			}
		}
	}

    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			if (state.active) 
				cycle();
			else if (onDeck && cue == CUE.Bar) {
				setActive(true);
			}
		}
		else if (prop == Property.MEASURE) {
			barTicks = resolution * clock.getMeasure(); // untested
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
    
	/** copy measure to end of track */
	public void copyFrame(int frame) {
		long start = frame * getWindow();
		Pattern work = new Pattern();
		MidiTools.loadSection(start, start + getWindow(), t, work);
		long transfer = frames() * getWindow();
		for (MidiEvent e : work)
			if (e.getMessage() instanceof ShortMessage)
				t.add(new MidiEvent(Midi.copy((ShortMessage)e.getMessage()), e.getTick() + transfer));
		setFrame(frames());
	}

	/** remove notes and update state */
	public void deleteFrame(int frame) {
		long start = frame * getWindow();
		long end = start + getWindow();
		long subtract = getWindow();
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end)
				e.setTick(e.getTick() - subtract);
			else {
				t.remove(e);
				i--;
			}
		}
		
		if (getFrame() > frame)
			setFrame(getFrame() - 1);
		else if (getFrame() == frame) {
			if (getFrame() - 1 < 0)
				setFrame(0);
			else setFrame(getFrame() - 1);
		}
		compute();
		MainFrame.update(this);
	}
	
    public final void setRecording(boolean rec) { // TODO
		recording = rec;
		MainFrame.update(this);
	}
    
	public final void setCue(CUE cue) {
		this.cue = cue;
		Constants.execute(()->Cue.update(this));
	}
	
	private void setFile(File f) {
		this.file = f;
		Folder.update(this);
	}

	public void clear() {
		new Panic(midiOut, ch).start();
		for (int i = t.size() -1; i >= 0; i--)
			t.remove(t.get(i));
		init();
		setFile(null);
		Folder.update(this);
		MainFrame.update(this);
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
			MidiSystem.write(s, MidiSystem.getMidiFileTypes(s)[0], f);
			RTLogger.log(this, getName() + " saved " + f.getName());
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
			if (midiFile.getTracks().length == 1) 
				importTrack(midiFile.getTracks()[0], midiFile.getResolution(), f); 
			else 
				new ImportMidi(this, midiFile);
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void importTrack(Track incoming, int resolution, File f) {
		this.resolution = resolution;
		this.barTicks = clock.getMeasure() * resolution;
		// notes
		for (int i = 0; i < incoming.size(); i++) 
			t.add(incoming.get(i));
		count = 0;
		setFile(f);
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
		if (!clock.isActive()) 
			setActive(!isActive());
		else if (isActive()) {
			setActive(false);
		} else if (cue == CUE.Hot)
			setActive(true);
		else {
			onDeck = !onDeck;
			MainFrame.update(this);
		}
	}
	
	private int after(int idx) {
		int result = idx + 1;
		if (result >= bars()) 
			result = 0;
		return result;
	}
	private int before(int idx) {
		int result = idx - 1;
		if (result < 0)
			return bars() - 1;
		return result;
	}

	/** compute left and right frame ticks based on cycle, current bar and count */
	private void compute() {
		switch(state.cycle) {
		case A:
			left = right = current * barTicks;
			break;
		case AB:
			if (isEven()) {
				left = current * barTicks;
				right = (current + 1) * barTicks;
			}
			else {
				left = before(current) * barTicks;
				right = current * barTicks;
			}
			break;
		case A3B:
			switch (count % 4) {
				case 0: left = right = current * barTicks; break;
				case 1: left = right = current * barTicks; break;
				case 2: left = current * barTicks; 
					right = (current + 1) * barTicks; break; 
				case 3: left = before(current) * barTicks;
					right = current * barTicks; break;
			}
			break;
		case ABCD:
			switch (count % 4) {
				case 0: 
				case 2: left = current * barTicks; 
					right = (current + 1) * barTicks; break;
				case 1: 
				case 3:
					left = before(current) * barTicks;
					right = current * barTicks; break;
			}
		case ALL:
			left = isEven() ? current * barTicks : before(current) * barTicks;
			right = isEven() ? after(current) * barTicks : current * barTicks;
			break;
		}
	}
	
	void cycle() {
		
		int change = current;
		count++;

		switch(state.cycle) {
			case AB:
				change += count % 2 == 0 ? -1 : 1;
				break;
			case ABCD:
				switch (count % 4) {
					case 0: 
						if (count != 0)
							change = before(before(before(current))); 
						break;
					case 1: 
					case 2: 
					case 3: 
						change++; 
						break;
				} 
				break;
			case A3B:
				switch (count % 4) {
					case 0: change = before(current); break;
					case 3: change++; break;
				}
				break;
			case ALL:
				change = after(current);
				break;
			case A:
				break;
		}
		
		setCurrent(change);
		
	}
	
}
