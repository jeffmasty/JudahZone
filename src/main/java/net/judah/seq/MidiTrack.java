package net.judah.seq;

import java.io.File;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.MidiReceiver;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumKit;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.util.Folders;
import net.judah.util.RTLogger;
import net.judah.widgets.FileChooser;

@Getter
public class MidiTrack implements TimeListener, MidiConstants {

	protected final JudahClock clock;
	protected final Track t;
	protected final Sequence s;
	
    protected final String name;
	@Setter protected boolean onDeck;
    protected int resolution = 256;
    protected long barTicks;
	protected File file;
    protected boolean record;
    protected float gain = 0.8f;
	protected MidiReceiver midiOut;
	@Setter protected int ch;
	protected final Scheduler scheduler;
	private final File folder;
	
    protected float head;
    protected long oldTime;
    private Bar work = new Bar();
    
    /** 16-step Drum track 
     * @throws Exception */
    public MidiTrack(MidiReceiver out, JudahClock clock) throws Exception {
    	this(out.getName(), out, out instanceof DrumKit ? 9 : 0, out instanceof DrumKit ? 4 : RESOLUTION, clock);
    }
    
    /** Synth track at standard RESOLUTION 
     * @throws Exception */
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
		scheduler = new Scheduler(this);
		folder = new File(Folders.getMidi(), name);
		if (folder.isDirectory() == false)
			folder.mkdir();
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
    			t.equals(it.getT()) && ch == it.ch && scheduler == it.scheduler;
    }
    

    @Override public String toString() { return name; }
    public final boolean isDrums() { return ch == 9; }
	public final boolean isPiano() { return ch != 9; }
	public int getPrevious() { return scheduler.getPrevious(); }
	public int getCurrent() { return scheduler.getCurrent(); }
	public int getNext() { return scheduler.getNext(); }
	public int getAfter() { return scheduler.getAfterNext(); }
	public void setCurrent(int change) {
		scheduler.setCurrent(change);
	}
	public boolean isActive() { return scheduler.isActive(); }

	
    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS && scheduler.isActive()) {
			getScheduler().cycle();
		}
		else if (prop == Property.MEASURE) {
			barTicks = resolution * (int)value; // untested
		}
	}
    
	public void setActive(boolean active) {
		scheduler.setActive(active);
	}
	
	/** copy measure to end of track */
	public void copy(int measure) {
		MidiTools.append(measure, this);
		MainFrame.update(scheduler);
	}

	
	/** remove notes and update state */
	public void delete(int measure) {
		MidiTools.delete(measure, this);
		if (getCurrent() > measure)
			setCurrent(getCurrent() - 1);
		else if (getCurrent() == measure) {
			if (getCurrent() - 1 < 0)
				setCurrent(0);
			else setCurrent(getCurrent() - 1);
		}
		scheduler.compute();
		MainFrame.update(scheduler);
	}
	
	/**@return number of bars */
	public int bars() {
		int track = MidiTools.measureCount(t.ticks(), barTicks);
		return track < 2 ? 2 : track;
	}
	
    public final void setRecord(boolean rec) { // TODO
		record = rec;
		MainFrame.update(this);
	}
    
	public final void setMidiOut(MidiReceiver port) {
		if (midiOut == port) return;
		MidiReceiver old = midiOut;
		midiOut = port;
		if (old != null) 
			new Panic(old, ch).start();
		MainFrame.update(this);
	}

	public final void setGain(float vol) {
		if (gain == vol)
			return;
		gain = vol;
		MainFrame.update(this);
	}
	
	public final void setCue(Cue cue) {
		if (scheduler.cue == cue)
			return;
		scheduler.cue = cue;
		MainFrame.update(this);
	}

	public void clear() {
		for (int i = t.size() -1; i >= 0; i--)
			t.remove(t.get(i));
		scheduler.init();
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
			MainFrame.update(getFolder());
		}
	}
	
	public void save(File f) {
		try {
			MidiSystem.write(s, MidiSystem.getMidiFileTypes(s)[0], f);
			this.file = f;
			RTLogger.log(this, getName() + " saved " + file.getName());
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void playTo(float percent) {
		head = percent;
		long newTime = getCurrent() * barTicks + (long)(percent * getResolution());
		MidiTools.loadSection(oldTime, newTime + 1, t, work);
		ShortMessage s;
		for (MidiEvent e : work)
			if (e.getMessage() instanceof ShortMessage) {
				s = (ShortMessage)e.getMessage();
				getMidiOut().send(s, JudahMidi.ticker());
			}
		oldTime = newTime + 1;
	}
	
	public void load() {
		File f = FileChooser.choose(getFolder());
		if (f != null) 
			load(f);
	}
	

	/** read and parse track patterns from disk (blocks thread) */
	public void load(File f) {
		try {
			Sequence midiFile = MidiSystem.getSequence(f);
			if (midiFile.getTracks().length != 1) {
				new ImportMidi(this, midiFile);
				// throw new Exception(f.getName() + ": Track count should be 1");
				return;
			}
			importTrack(midiFile.getTracks()[0], midiFile.getResolution()); 
			file = f;
			setCurrent(0);
			flush(1f);
			MainFrame.update(this);
		} catch (Exception e) { RTLogger.warn(this, e); }
	}
	
	public void importTrack(Track incoming, int resolution) {
		this.resolution = resolution;
		this.barTicks = clock.getMeasure() * resolution;
		clear();
		// notes
		for (int i = 0; i < incoming.size(); i++) {
				t.add(incoming.get(i));
		}
	}

	public void flush(float head) {
		long start = getCurrent() * barTicks + (long)(head * barTicks);
		long end = (getCurrent() + 1) * barTicks;
		MidiTools.loadSection(start, end, getT(), work);
		for (MidiEvent e : work)
			getMidiOut().send(e.getMessage(), JudahMidi.ticker());
	}

	
	public void addEvent(MidiEvent e, int bar) {
		t.add(new MidiEvent(e.getMessage(), e.getTick() + barTicks * bar));
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

	public boolean isEven() {
		return scheduler.getCount() % 2 == 0;
	}

}

