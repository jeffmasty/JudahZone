package net.judah.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.midi.GMNames;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

/** a step sequencer. 
 * Ch#,  file, midiOut/(AudioIn), pattern cycle  */
@Getter 
public abstract class Track extends ArrayList<Pattern> implements Runnable {

	public static enum Cue { Cue, Loop, Bar};
	public static final File DRUM_FOLDER = new File(Constants.ROOT, "patterns");
	public static final File MELODIC_FOLDER = new File(Constants.ROOT, "sequences");
	private static int counter;

	protected final String name;
	protected final int num;
	protected final int ch;
	protected File file;

	protected final JudahClock clock;
	protected final TrackView view;
	protected TrackEdit edit;

	protected boolean active;
	protected boolean record;
	protected Pattern current;
	protected JackPort midiOut;
	@Setter protected String instrument;
    protected Cue cue = Cue.Cue;
    @Setter protected boolean onDeck;
	protected final Cycle cycle = new Cycle(this);
    @Setter protected int steps = JudahClock.getSteps();
    @Setter protected int div = JudahClock.getSubdivision();
    @Setter protected boolean latch;
    @Setter protected int step = -1;
	protected float gain = 0.8f;
	
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

	public Track(JudahClock clock, String name, int ch, final JackPort port) {
		this.ch = ch;
		this.name = name;
		this.clock = clock;
		clock.addListener(transportListener);
		this.midiOut = port;
		num = counter++;
		add(new Pattern("A", this));
		current = get(0);
		view = new TrackView(this);
	}
	
	@Override
	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((Track)o).name);
	}
	
	
	@Override
	public void run() {
		
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
                    midiOut = JudahMidi.getByName(out);
                    if (!split[1].equals("none")) {
                        instrument = split[1];
                        if (isDrums())
                        	JudahMidi.getInstance().progChange(Instrument.lookup(instrument, isDrums()), getMidiOut(), getCh());
                        else {
                        	for (int i = 0; i < GMNames.GM_NAMES.length; i++) {
                        		if (GMNames.GM_NAMES[i].equals(instrument)) {
                        			JudahMidi.getInstance().progChange(i, midiOut, ch);
                        		}
                        	}
                        }
                    }
                    if (split.length >= 3) {
                    	steps = Integer.parseInt(split[2]);
                    	if (JudahClock.getTracker().getDrum1() == this) {
                    		clock.setSteps(steps); // drum1 sets clock time signature
                    	}
                    }
                    if (split.length >= 4) {
                    	div = Integer.parseInt(split[3]);
                    	if (JudahClock.getTracker().getDrum1() == this) {
                    		clock.setSubdivision(div); // drum1 sets clock time signature
                    	}
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
        	RTLogger.log(this, file.getName() + ": " + t.getMessage());
        	return;
        } finally {
            if (scanner != null) scanner.close();
        }
        if (isEmpty()) 
        	add(new Pattern("A", this));
        edit.fillPatterns();
        view.fillPatterns();
        setCurrent(get(0));

        RTLogger.log(this, name + " loaded " + file.getName() + " on " + midiOut.getShortName());
    }	

	// ------ Pattern CRUD ------ //
	public void deletePattern() {
		remove(current);
		if (size() == 0)
			newPattern();
		else {
			edit.fillPatterns();
			view.fillPatterns();
			setCurrent(get(0));
		}
			
	}
	
	public void copyPattern() {
		Pattern copy = new Pattern("" + (char)('A' + size()), current, this);
		add(copy);
		edit.fillPatterns();
		view.fillPatterns();
		setCurrent(copy);
	}

	public void newPattern() {
		String name = "" + (char)('A' + size());
		Pattern pat = new Pattern(name, this);
		add(pat);
    	edit.fillPatterns();
    	view.fillPatterns();
		setCurrent(pat);
	}
	
	public void setCurrent(Pattern p) {
		if (current == p) return;
		current = p;
		int idx = indexOf(p);
		if (this.contains(p) == false) {
			throw new RuntimeException(name + " -- " + p.name + " file " + file);
		}
		
		new Thread(() -> {
			if (view.getPattern().getSelectedIndex() != idx) 
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

	public File getFolder() {
		return isDrums() ? DRUM_FOLDER	: MELODIC_FOLDER;}
	
	public final boolean isDrums() {
		return ch == 9;
	}
	public final boolean isSynth() {
		return ch != 9;
	}
	
	
	
	
	public void setActive(boolean active) {
		if (active) {
			if (onDeck) {
				step = -1;
				this.active = true;
				onDeck = false;
			}
			else switch (cue) {
				case Bar:
					onDeck = true;
					JudahClock.getInstance().addListener(cueListener);
					break;
				case Cue:
					step = JudahClock.getStep() - 1;
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
	
	public void selectFile(int data2) {
		File[] folder = getFolder().listFiles();
		int idx = Constants.ratio(data2, folder.length + 1);
		if (idx == 0) { // blank line in combo box
			if (getFile() != null)
				setFile(null);
		}
		else {
			File target = folder[idx - 1];
			if (!target.equals(getFile()))
				setFile(target);
		}
	}

	public void step() {
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
			if (isDrums()) {
				msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
						((int)(msg.getData2() * ((DrumTrack)this).velocity(msg.getData1()) * gain))); 
				JudahMidi.queue(msg, midiOut);
			}
			else {
				msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
						((int)(msg.getData2() * gain))); 
				if (latch) 
					JudahMidi.queue(Transpose.apply(msg), midiOut);
				else 
					JudahMidi.queue(msg, midiOut);
			}
		}
	}
	
	public final void setFile(File file) {
		clear();
		this.file = file;
        if (file != null && file.isFile())
        	new Thread(this).start();
        else
        	newPattern();
	}

    public void write(File f) {
        StringBuffer raw = new StringBuffer();
        raw.append(getMidiOut().getShortName()).append("/");
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
        } catch (Exception e) { RTLogger.warn(this, name + " " + f.getName() + " : " + e.getMessage()); }
        for (Track t : JudahClock.getTracks()) {
        	t.getView().getFilename().refresh();
        	t.getEdit().fillFile1();
        }
    }

	public void toggleRecord() {
		record = !record;
		JudahMidi.getInstance().getGui().record(JudahClock.getTracker().isRecord());
		edit.getRecord().setBackground(record ? Pastels.RED : Pastels.BUTTONS);
	}

	public void setMidiOut(JackPort port) {
		if (midiOut == port) return;
		JackPort old = midiOut;
		midiOut = port;
		if (old != null) 
			new Panic(old).start();
		MidiOut out = edit.getMidiOut();
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
		if (cue != view.getCue().getSelectedItem())
			view.getCue().setSelectedItem(cue);
	}
    
}


