package net.judah.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiCable;
import net.judah.midi.MidiPort;
import net.judah.midi.Panic;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** a step sequencer. 
 * Ch#,  file, midiOut/(AudioIn), pattern cycle  */
@Getter 
public abstract class Track extends ArrayList<Pattern> {

	public static enum Cue { Cue, Loop, Bar, Trig};
	public static final File DRUM_FOLDER = new File(Constants.ROOT, "patterns");
	public static final File MELODIC_FOLDER = new File(Constants.ROOT, "sequences");
	private static int counter;

	protected final JudahBeatz tracker;
	protected final String name;
	protected final int num;
	protected final int ch;
	protected File file;

	protected final JudahClock clock;
	protected final Cycle cycle = new Cycle(this);
	protected TrackView view;
	protected TrackEdit edit;

	protected boolean active;
    @Setter protected int step = -1;
    protected int substep = -1;
    protected float gain = 0.8f;
    protected boolean record;
	protected Pattern current;
	protected MidiPort midiOut;
	@Setter protected String instrument;
    protected Cue cue = Cue.Cue;
	protected int steps;
    protected int div;
    @Setter protected boolean latch;
    @Setter protected boolean onDeck;
    /** 1-to-ratio, 0 = 1:1 speed up of step sequencer */
    @Setter protected int ratio;
	
	private TimeListener transportListener = new TimeListener() {
		@Override public void update(Property prop, Object value) {
		if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStarting)
			step = -1;
		else if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStopped)
			if (active)
				new Panic(midiOut, ch).start();
		}
	};
	
	private TimeListener cueListener = new TimeListener() {
		@Override public void update(Property prop, Object value) {
			if (prop == Property.BARS) {
				clock.removeListener(this);
				if (onDeck)
					setActive(true);
			}
			if (prop == Property.LOOP || prop == Property.STATUS && value == Status.TERMINATED) {
				JudahZone.getLooper().getLoopA().removeListener(this);
				if (onDeck) {
					setActive(true);
					step();
				}
			}
		}
	};

	public Track(JudahClock clock, String name, int ch, final MidiPort port, JudahBeatz tracker) {
		num = counter++;
		
		if (port == null)
			throw new NullPointerException("" + counter);
		this.ch = ch;
		this.name = name;
		this.clock = clock;
		this.tracker = tracker;
		steps = clock.getSteps();
		div = clock.getSubdivision();
		clock.addListener(transportListener);
		this.midiOut = port;
		add(new Pattern("A", this));
		current = get(0);
	}
	
	public TrackView getView() {
		if (view == null)
			view = new TrackView(this);
		return view;
	}
	@Override
	public boolean equals(Object o) { return super.equals(o) && name.equals(((Track)o).name); }
	
	public File getFolder() { return isDrums() ? DRUM_FOLDER	: MELODIC_FOLDER; }
	
	public final boolean isDrums() { return ch == 9; }
	
	public final boolean isSynth() { return ch != 9; }

	/** play midi notes */
	public void step() {
		if (ratio != 0) {
			substep++;
			if (substep >= ratio) {
				substep = 0;
			}
			if (substep != 0)
				return;
		}

		++step;
		
		if (step >= steps) {
			step = 0;
			if (active)
				cycle.cycle();
		}
		if (edit.isVisible()) 
			edit.step(step);
		if (!active)
			return;
		
		Notes now = current.get(step);
		if (now == null)
			return;
		for (ShortMessage msg : now) {
			int ticker = JudahMidi.ticker();
			if (isDrums()) {
				msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
						((int)(msg.getData2() * ((DrumTrack)this).velocity(msg.getData1()) * gain))); 
				midiOut.send(msg, ticker);
			}
			else {
				msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
						((int)(msg.getData2() * gain))); 
				if (latch) 
					midiOut.send(Transpose.apply(msg), ticker);
				else 
					midiOut.send(msg, ticker);
			}
		}
	}
	
	public void setActive(boolean active) {
		if (active) {
			substep = -1;
			if (onDeck) {
				step = -1;
				this.active = true;
				onDeck = false;
			}
			else switch (cue) {
				case Bar:
					onDeck = true;
					clock.addListener(cueListener);
					break;
				case Cue:
					step = clock.getStep() - 1;
					onDeck = false;
					this.active = true;
					break;
				case Loop:
					onDeck = true;
					JudahZone.getLooper().getLoopA().addListener(cueListener);
					break;
				default:
					break;
				}
		}
		else {
			this.active = false;
			this.onDeck = false;
			if (isSynth())
				new Panic(midiOut, ch).start(); 
		}
		MainFrame.update(this);
	}


	// ------ Pattern CRUD ------ //
	public void deletePattern() {
		remove(current);
		if (size() == 0)
			newPattern();
		else {
			edit.fillPatterns();
			getView().fillPatterns();
			setCurrent(get(0));
		}
	}
	
	public void copyPattern() {
		Pattern copy = new Pattern("" + (char)('A' + size()), current, this);
		add(copy);
		edit.fillPatterns();
		getView().fillPatterns();
		setCurrent(copy);
	}

	public void newPattern() {
		String name = "" + (char)('A' + size());
		Pattern pat = new Pattern(name, this);
		add(pat);
    	edit.fillPatterns();
    	getView().fillPatterns();
		setCurrent(pat);
	}
	
	public void setPattern(String name) {
		for (int i = 0; i < size(); i++)
			if (name.equals(get(i).getName()))
				setCurrent(get(i));
	}
	
	public void setCurrent(Pattern p) {
		if (current == p) return;
		current = p;
		int idx = indexOf(p);
		if (this.contains(p) == false) {
			throw new RuntimeException(name + " -- " + p.name + " file " + file);
		}
		
		new Thread(() -> {
			tracker.label();
			if (getView().getPattern().getSelectedIndex() != idx) 
				view.getPattern().setSelectedIndex(idx);
			edit.setPattern(idx);
		}).start();
	}
	
	public void next(boolean forward) {
		int result = indexOf(current) + (forward ? 1 : -1);
		
		if (result >= size())
			result = 0;
		if (result < 0) 
			result = size() - 1;
		
        setCurrent(get(result));
	}

	public final void setFile(File file) {
		clear();
		this.file = file;
		if (file != null && file.isFile())
			readFromDisk();
        else
        	newPattern();
	}
	
	public void setFile(String name) {
		setFile(new File(getFolder(), name));
	}

	public final void clearFile() {
		setFile((File)null);
	}

	/** read and parse track patterns from disk (blocks thread) */
	private void readFromDisk() {
	
        Scanner scanner = null;
        try {
            Pattern onDeck = null;
            boolean first = true;
            scanner = new Scanner(file);
            clear();
            cycle.setSelected(0);
            ArrayList<String> contract = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                
                if (first) {
                    String[] split = line.split("[/]");
                    String out = split[0];
                    if (isDrums())
                    	midiOut = JudahZone.getDrumPorts().get(out);
                    else 
                    	midiOut = JudahZone.getSynthPorts().get(out);
                    if (!split[1].equals("none")) {
                        instrument = split[1];
                        midiOut.progChange(instrument, ch);
                    }
                    if (split.length >= 3) {
                    	setSteps(Integer.parseInt(split[2]));
                    	if (JudahZone.getTracker().getDrum1() == this) {
                    		clock.setSteps(steps); // drum1 sets clock time signature
                    	}
                    }
                    if (split.length >= 4) {
                    	setDiv(Integer.parseInt(split[3]));
                    }
                    if (split.length >= 5) 
                    	cycle.setSelected(Integer.parseInt(split[4]));
                    if (split.length >= 6)
                    	clock.writeTempo(Integer.parseInt(split[5]));
                    first = false;
                }
                // Contract
                else if (line.contains(DrumTrack.HEADER)) {
                	if (contract == null) 
                		contract = new ArrayList<>();
                	else {
                		((DrumTrack)this).kitFromFile(contract);
                		contract = null;
                	}
                }
                else if (contract != null)
                	contract.add(line);
                
                else if (line.startsWith(Pattern.PATTERN_TOKEN)) {
                	if (onDeck != null)
                        add(onDeck);
                    onDeck = new Pattern(line.substring(Pattern.PATTERN_TOKEN.length()), this);
                }
                else {
                    onDeck.raw(line);
                }
            }
            add(onDeck);
        } catch (Throwable t) {
        	RTLogger.warn(this, t); // file.getName() + ": " + t.getMessage()
        	return;
        } finally {
            if (scanner != null) scanner.close();
        }
        if (isEmpty()) 
        	add(new Pattern("A", this));
        edit.fillPatterns();
        if (view != null)
        	view.fillPatterns();
        setCurrent(get(0));
        RTLogger.log(this, name + " loaded " + file.getName() + " on " + midiOut);
    }	

	public void write(File f) {
        StringBuffer raw = new StringBuffer();
        raw.append(getMidiOut()).append("/");
        raw.append(getInstrument() == null ? "none" : getInstrument());
        raw.append("/").append(steps);
        raw.append("/").append(div);
        raw.append("/").append(cycle.getSelected());
        raw.append(Constants.NL);
        
        if (isDrums())
        	raw.append(((DrumTrack)this).kitToFile());
        for (Pattern pattern : this) {
            raw.append(pattern.forSave(isDrums()));
        }
        try {
            Constants.writeToFile(f, raw.toString());
            RTLogger.log(this, "saved " + f.getName());
        } catch (Exception e) { 
        	RTLogger.warn(this, name + " " + f.getName() + " : " + e.getMessage()); 
        }
        tracker.fileRefresh();
    }

	public void toggleRecord() {
		record = !record;
		JudahZone.getMidiGui().record(JudahZone.getTracker().isRecord());
		edit.getRecord().setBackground(record ? Pastels.RED : Pastels.BUTTONS);
	}

	public void setMidiOut(MidiPort port) {
		if (midiOut == port) return;
		MidiPort old = midiOut;
		midiOut = port;
		if (old != null) 
			new Panic(old).start();
		MidiCable out = edit.getMidiOut();
		if (out.getSelectedItem() != midiOut) 
			out.setSelectedItem(midiOut);
		out = view.getMidiOut();
		if (out.getSelectedItem() != midiOut)
			out.setSelectedItem(midiOut);
		
	}

	public void setGain(float vol) {
		if (gain == vol)
			return;
		gain = vol;
		int intVol = (int)(gain * 100);
		if (edit != null && edit.getTrackVol().getValue() != intVol)
			edit.getTrackVol().setValue(intVol);
		if (view != null && view.getVolume().getValue() != intVol)
			view.getVolume().setValue(intVol);
	}
	
	public void setCue(Cue cue) {
		if (this.cue == cue)
			return;
		this.cue = cue;
		if (cue != edit.getCue().getSelectedItem())
			edit.getCue().setSelectedItem(cue);
		if (cue != getView().getCue().getSelectedItem())
			view.getCue().setSelectedItem(cue);
	}

	@Override
	public String toString() {
		return name;
	}

	public void setDiv(int subdivision) {
		if (div == subdivision) 
			return;
		this.div = subdivision;
		if (num == 0)
			clock.setSubdivision(div);
	}
	
    public void setSteps(int change) {
    	if (steps == change)
    		return;
    	this.steps = change;
    	if (num == 0)
    		clock.setSteps(steps);
    }

	
}


