package net.judah.seq;

import java.io.File;
import java.util.ArrayList;

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
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;
import net.judah.widgets.FileChooser;

@Getter
public class MidiTrack extends ArrayList<Bar> implements TimeListener, MidiConstants {

	protected final JudahClock clock;

    protected final String name;
	protected boolean active;
	@Setter protected boolean onDeck;
    protected int resolution = 256;
    protected long ticks;
	protected File file;
    protected boolean record;
    protected float gain = 0.8f;
	protected MidiReceiver midiOut;
	@Setter protected String instrument;
	@Setter protected int ch;
	protected Cue cue = Cue.Bar;
    protected final Scheduler scheduler;
    protected final Situation state = new Situation();
    private Accumulator accumulator = new Accumulator();
    protected MidiView view;
    
    /** 16-step Drum track */
    public MidiTrack(MidiReceiver out, JudahClock clock) {
    	this(out.getName(), out, out instanceof DrumKit ? 9 : 0, out instanceof DrumKit ? 4 : RESOLUTION, clock);
    }
    
    @Override
    public int hashCode() {
    	return name.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
    	if (false == o instanceof MidiTrack) return false;
    	MidiTrack it = (MidiTrack)o;
    	return o instanceof MidiTrack && name.equals(it.getName()) && accumulator == it.accumulator && midiOut == it.getMidiOut() && 
    			size() == it.size() && resolution == it.resolution && ch == it.ch && state == it.state;
    }
    
    /** Synth track at standard RESOLUTION */
    public MidiTrack(MidiReceiver out, int ch, JudahClock clock) {
    	this(out.getName() + ch, out, ch, RESOLUTION, clock);
    }
    
    public MidiTrack(String name, MidiReceiver out, int ch, int rez, JudahClock clock) {
		this.name = name;
		this.ch = ch;
		this.midiOut = out;
		this.clock = clock;
		this.resolution = rez;
		ticks = clock.getMeasure() * resolution;
		add(new Bar());
		scheduler = new Scheduler(this);
		clock.addListener(this);
    }

    @Override public String toString() { return name; }
    public final boolean isDrums() { return ch == 9; }
	public final boolean isSynth() { return ch != 9; }
	public File getFolder() { return Folders.getMidi();}  //TODO return new File(Folders.getMidi(), getName()); } 

    @Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			getScheduler().cycle();
		}
		else if (prop == Property.STEP && active && viewVisible() && view.getLive().isSelected()) {
			view.getSteps().setStart((int)value);
			view.getSteps().repaint();
			view.getGrid().repaint();
		}
		else if (prop == Property.MEASURE) {
			ticks = resolution * (int)value; // untested
			view.setTimeframe(2 * ticks);
		}
	}
	public void setActive(boolean active) {
		this.active = active;
		if (!active)
			new Panic(midiOut, ch).start();
		MainFrame.update(this);

	}
	
	public Bar getNext() {
		return get(state.next);
	}

	public Bar getBar() {
		return get(getCurrent());
	}
	public int getCurrent() {
		return state.current;
	}
	
	public void setCurrent(int change) {
    	state.previous = state.current;
    	if (change == state.current)
    		return;
    	state.current = change;
    	MainFrame.update(this);
    	scheduler.compute();
	}
	
    public void setBar(Bar bar) {
    	setCurrent(indexOf(bar));
    }
    
    @Override
	public Bar get(int idx) {
    	if (idx < 0)
    		return super.get(0);
		while (idx >= size())
			add(new Bar());
		return super.get(idx);
	}

    public int after(int idx) {
		int result = idx + 1;
		if (result >= size())
			return 0;
		return result;
	}
	public int before(int idx) {
		int result = idx - 1;
		if (result < 0)
			return size() - 1;
		return result;
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
		if (this.cue == cue)
			return;
		this.cue = cue;
		MainFrame.update(this);
	}

	public boolean viewVisible() {
		return view!= null && view.isVisible();
	}
	
	public MidiView getView() {
		if (view == null)
			view = new MidiView(this);
		return view;
	}

	@Override
	public void clear() {
		super.clear();
		scheduler.init();
		MainFrame.update(this);
	}
	
	public void save() {
		try {
			File f = FileChooser.choose(getFolder());
			if (f != null)
				save(f);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}
	
	public void save(File f) throws Exception {
		try {
			Sequence s = new Sequence(Sequence.PPQ, getResolution(), 0);
			Track t = s.createTrack();
			for (int bar = 0; bar < size(); bar++) {
				Bar b = get(bar);
				for (MidiEvent e : get(bar)) 
					t.add(new MidiEvent(e.getMessage(), e.getTick() + bar * ticks));
			}
			MidiSystem.write(s, MidiSystem.getMidiFileTypes(s)[0], f);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	public void open() {
		try {
			File f = FileChooser.choose(getFolder());
			if (f != null)
				open(f);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}
	

	/** read and parse track patterns from disk (blocks thread) */
	public void open(File f) throws Exception {
		Sequence midiFile = MidiSystem.getSequence(f);
		if (midiFile.getTracks().length != 1)
			throw new Exception(f.getName() + ": Track count should be 1");
		importTrack(midiFile.getTracks()[0], midiFile.getResolution()); 
		file = f;
		setCurrent(0);
		scheduler.hotSwap();
		if (viewVisible()) {
			view.getMenu().fillPatterns();
			// view.getMenu().getBars().fill();
			MainFrame.update(this);
		}
	}

	public void importTrack(Track incoming, int resolution) {
		this.resolution = resolution;
		this.ticks = clock.getMeasure() * resolution;
		super.clear();

		// notes
		for (int i = 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (e.getMessage() instanceof ShortMessage) {
				int idx = (int) (e.getTick() / ticks);
				Bar bar = get(idx);
				bar.add(new MidiEvent(new Midi(e.getMessage().getMessage()), e.getTick() - ticks * idx));
			}
		}
	}
	

	public void addEvent(MidiEvent e) {
		int bar = (int)(e.getTick() / ticks);
		get(bar).add(new MidiEvent(e.getMessage(), e.getTick() - bar * ticks));
	}
	
	public MidiEvent findNext(int cmd, int data1, int ref, long fromTick) {
		for (int i = ref; i < 4; i++) {
			for (MidiEvent e : get(state.get(i))) {
				if (e.getTick() < fromTick)
					continue;
				if (e.getMessage() instanceof ShortMessage && e.getMessage().getStatus() == cmd) {
					if ( ((ShortMessage)e.getMessage()).getData1() == data1)
						return e;
				}
			}
			fromTick = 0; // search from start of next bar
		}
		return null;
	}
	
	// copy all or part of measure A, copy all of measure B, copy part or none of measure C
	public void publishView(Snippet result) {
		result.clear();
		accumulator.clear();
		long start = result.getStartref() % ticks; 
		long end = ticks - start;

		result.one = get(state.current);
		snip(result.one, start, ticks, 0, result);
		
		result.two = get(state.next);
		snip(result.two, 0, ticks, ticks - start, result);
		if (end != ticks) {
			result.three = get(state.afterNext);
			snip(result.three, 0, end, (2 * ticks) - start, result);
		}
		else 
			result.three = null;
	
		// anything left in accumulator?
		for (MidiEvent e : accumulator) {
			ShortMessage on = (ShortMessage)e.getMessage();
			result.add(new Note(e.getTick(), 2 * ticks, on.getData1(), on.getData2()));
		}
	}

	private void snip(Bar bar, long start, long end, long translate, Snippet result) {
		float twoBar = 2 * ticks;
		for (MidiEvent e : bar) {
			if (e.getTick() < start || e.getMessage() instanceof ShortMessage == false)
				continue;
			if (e.getTick() >= end) 
				return;
			ShortMessage s = (ShortMessage)e.getMessage();
			if (isDrums()) {
				float top = (e.getTick() + translate) / twoBar;
				result.add(new Note(top, top, s.getData1(), s.getData2()));
			}
			else if (s.getCommand() == NOTE_ON) 
				accumulator.add(e);
			else if (s.getCommand() == NOTE_OFF) {
				MidiEvent on = accumulator.get(s);
				float top = on == null ? 0 : (on.getTick() + translate) / twoBar;
				float bottom = (e.getTick() + translate) / twoBar;
				int velocity = on == null ? 99 : ((ShortMessage)on.getMessage()).getData2();
				result.add(new Note(top, bottom, s.getData1(), velocity));
			}
		}
	}

	public void newBar() {
		String input = Constants.inputBox("Pattern Name");
		if (input == null || input.isEmpty())
			return;
		Bar b = new Bar();
		add(b);
		patternUpdate();
		setCurrent(indexOf(b));
		scheduler.compute();
	}

	public void copy() {
		copy(get(state.getCurrent()));
	}

	private void copy(Bar bar) {
		Bar copy = new Bar(bar);
		add(copy);
		patternUpdate();
		setCurrent(indexOf(copy));
	}

	public void delete() {
		delete(state.getCurrent());
	}

	private void delete(int idx) {
		remove(idx);
		patternUpdate();
		if (state.current > idx)
			setCurrent(state.current - 1);
		else if (state.current == idx) {
			if (state.current - 1 < 0)
				setCurrent(0);
			else setCurrent(state.current - 1);
		}
		scheduler.compute();
	}
	
	private void patternUpdate() {
		if (viewVisible())
			view.getMenu().fillPatterns();
	}

	public void toggleMute(DrumType type) {

	}
	
}

//	// incoming pattern names
//	for (int i = 0; i < incoming.size(); i++) {
//		if (incoming.get(i).getMessage() instanceof MetaMessage) {
//        	MetaMessage m = (MetaMessage)incoming.get(i).getMessage();
//        	if (m.getMessage()[1] == NAME_STATUS) { 
//        		// long tick = incoming.get(i).getTick() / 2; // why divide tick by 2??
//        		get((int) (incoming.get(i).getTick() / ticks)).setName(new String(m.getData()));}}}

//	byte[] nombre = b.getName().getBytes();
//	try { // save outgoing pattern names
//		t.add(new MidiEvent(new MetaMessage(NAME_STATUS, nombre, nombre.length), bar * ticks));
//	} catch (InvalidMidiDataException e) {



//    public abstract void next(boolean forward); 
//    /** copy bar from to tail of track */
//    public abstract void copy(int from);
//    /** copy bar from, inserting at to */
//    public abstract void copy(int from, int to);
//    protected abstract void readFromDisk() throws Exception;
//    public abstract int newBar();
//  int key = msg.getData1();
//  int octave = (key / 12)-1;
//  int note = key % 12;
//  String noteName = NOTE_NAMES[note];
//  int velocity = msg.getData2();
//  System.out.println(cmd == NOTE_ON ? "NoteOn, " : "NoteOff, " + noteName + octave + 
//  " key=" + key + " velocity: " + velocity);
