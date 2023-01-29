package net.judah.seq;

import java.io.File;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.api.MidiReceiver;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.widgets.FileChooser;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.song.Sched;
import net.judah.song.Trigger;
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
	@Getter private int amplification = 80;
    @Getter private Sched state = new Sched();
	@Getter private long left, right;
	private int current; // current pattern/bar
	private int count; // increment pattern cycle
	private long oldTime; // sequencer sweep
	
	@Getter private boolean record; // TODO
 	@Getter private boolean onDeck; // TODO
	@Getter private Trigger cue = Trigger.HOT; // @JsonIgnore/sysex msg?
	
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
		barTicks = clock.getMeasure() * resolution;
		s = new Sequence(Sequence.PPQ, resolution, 0);
		t = s.createTrack();
		folder = new File(Folders.getMidi(), name);
		if (folder.isDirectory() == false)
			folder.mkdir(); // inelegant?
		clock.addListener(this);
    }
    
    @Override
    public int hashCode() {
    	return name.hashCode() + t.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
    	if (false == o instanceof MidiTrack) return false;
    	MidiTrack it = (MidiTrack)o;
    	return name.equals(it.getName()) && midiOut == it.getMidiOut() && 
    			t.equals(it.getT()) && ch == it.ch;
    }
    
    public final boolean isDrums() { return ch == 9; }
    public final boolean isSynth() { return ch != 9; }
	public boolean isEven() { return count % 2 == 0; }
    public CYCLE getCycle() { return state.cycle; }
    @Override public String toString() { return name; }

    public boolean isActive() { return state.active; }
	public void setActive(boolean active) {
		state.active = active;
		if (active)
			count = isEven() ? 0 : 1;
		else 
			new Panic(midiOut, ch).start();
		MainFrame.update(this);
	}
	
	public void playTo(float percent) {
		long newTime = current * barTicks + (long)(percent * resolution);
		if (percent == 0)
			oldTime = newTime - 1;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < oldTime) continue;
			if (e.getTick() > newTime) break;
			if (e.getMessage() instanceof ShortMessage) 
				getMidiOut().send(
						Midi.format((ShortMessage)e.getMessage(), ch, Constants.midi2float(amplification)), 
						JudahMidi.ticker());
		}
		oldTime = newTime + 1;
	}
	
	private void flush() {
		long end = (current + 1) * barTicks;
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < oldTime) continue;
			if (e.getTick() > end) break;
			if (e.getMessage() instanceof ShortMessage && Midi.isNoteOff((ShortMessage)e.getMessage())) {
				midiOut.send(Midi.format((ShortMessage)e.getMessage(), ch, 1), JudahMidi.ticker());
			}
		}
	}

	private void setCurrent(int change) {
		int old = current;
		if (isSynth()) 
			flush();
		if (change < 0) 
			change = 0;
		current = change;
		compute();
		if (old != current)
			MainFrame.setFocus(this);
	}

	public void setFrame(int window) {
		setCurrent(window * 2 + (isEven() ? 0 : 1));
	}
	public int getFrame() {
		return current % 2 == 0 ? current / 2 : (current - 1) / 2;
	}

	/**@return number of bars */
	public int bars() {
		return MidiTools.measureCount(t.ticks(), barTicks);
	}
	public int frames() {
		return MidiTools.measureCount(t.ticks(), 2 * barTicks);
	}
	public long getWindow() {
		return 2 * barTicks;
	}

	public void setCycle(CYCLE x) {
		state.setCycle(x);
		count = isEven() ? 0 : 1;
		compute();
		Cycle.update(this);
	}

    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS && state.isActive()) 
			cycle();
		else if (prop == Property.MEASURE) {
			barTicks = resolution * (int)value; // untested
		}
		else if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStopped) {
			if (isActive())
				new Panic(midiOut, ch).run();
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
	
	
    public final void setRecord(boolean rec) { // TODO
		record = rec;
		MainFrame.update(this);
	}
    
	public final void setCue(Trigger cue) {
		this.cue = cue;
		MainFrame.update(this); // TODO update menu?
	}

	public void clear() {
		for (int i = t.size() -1; i >= 0; i--)
			t.remove(t.get(i));
		init();
		file = null;
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
			save(f);
			Folder.refill(this);
		}
	}
	
	public void save(File f) {
		try {
			MidiSystem.write(s, MidiSystem.getMidiFileTypes(s)[0], f);
			this.file = f;
			RTLogger.log(this, getName() + " saved " + file.getName());
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void load() {
		File f = FileChooser.choose(getFolder());
		if (f != null) 
			load(f);
	}

	/** read and parse track patterns from disk (blocks thread) */
	public void load(File f) {
		if (f == null || f.isFile() == false) {
			clear();
			return;
		}
		try {
			Sequence midiFile = MidiSystem.getSequence(f);
			if (midiFile.getTracks().length != 1) {
				new ImportMidi(this, midiFile);
				// throw new Exception(f.getName() + ": Track count should be 1");
				return;
			}
			importTrack(midiFile.getTracks()[0], midiFile.getResolution()); 
			file = f;
			new Panic(midiOut, ch).start();
			current = count = 0;
			setCurrent(0);
			MainFrame.update(this);
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void importTrack(Track incoming, int resolution) {
		this.resolution = resolution;
		this.barTicks = clock.getMeasure() * resolution;
		clear();
		// notes
		for (int i = 0; i < incoming.size(); i++) 
			t.add(incoming.get(i));
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

	public void setState(Sched sched) {
		if (state == sched)
			return;
		Sched old = state;
		if (old.active && !sched.active)
			new Panic(midiOut, ch).start();

		state = sched;
		setFrame(sched.launch);
		if (old.cycle != sched.cycle)
			setCycle(sched.cycle);
		setAmplification(sched.amp);
	}
	
	public void setAmplification(int amp) {
		if (amplification != amp) {
			amplification = amp;
			MainFrame.update(this);
		}
	}
	
	void init() {
		current = 0;
		count = 0;
		compute();
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
		MainFrame.update(this);
		
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
	
}

//	public void setCount(int count) {
//		this.count = count;
//		compute();
//		MainFrame.update(this); }
