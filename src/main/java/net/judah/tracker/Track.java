package net.judah.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.tracker.todo.PianoEdit;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** a step sequencer. 
 * Ch#,  file, midiOut/(AudioIn), pattern cycle  */
@Getter 
public abstract class Track extends ArrayList<Pattern> implements Runnable {

	public static final File DRUM_FOLDER = new File(Constants.ROOT, "patterns");
	public static final File MELODIC_FOLDER = new File(Constants.ROOT, "sequences");

	private static int counter;
	protected final String name;
	protected final JudahClock clock;
	protected final int num;
	
	@Getter protected final DrumKit kit;
	protected final TrackView view;
	protected final TrackEdit grid;

	@Setter protected String instrument;
	protected File file;
	@Setter protected JackPort midiOut;
	protected final int ch;
	protected final Cycle cycle = new Cycle(this);
	@Getter protected Pattern current;
	
	// TODO
    @Getter protected int steps = JudahClock.getSteps();
    @Getter protected int div = JudahClock.getSubdivision();

	protected boolean active;
	@Setter protected float gain = 0.8f;

	public Track(JudahClock clock, String name, int ch, final JackPort port) {
		this.ch = ch;
		this.name = name;
		this.clock = clock;
		this.midiOut = port;
		num = counter++;
		kit = new DrumKit(isDrums());
		add(new Pattern("A"));
		current = get(0);
		view = new TrackView(this);
		grid = isDrums() ? new DrumEdit(this) : new PianoEdit(this);
	}
	
	@Override
	public void run() {
		
        Scanner scanner = null;
        try {
            Pattern onDeck = null;
            boolean first = true;
            scanner = new Scanner(file);
            clear();
            ArrayList<String> contract = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                
                if (first) {
                    String[] split = line.split("[/]");
                    if (!split[1].equals("none")) {
                        instrument = split[1];
                        JudahMidi.getInstance().progChange(Instrument.lookup(instrument, this), getMidiOut(), getCh());
                    }
                    first = false;
                }
                // Contract
                else if (line.contains(DrumKit.HEADER)) {
                	if (contract == null) 
                		contract = new ArrayList<>();
                	else {
                		kit.fromFile(contract);
                		contract = null;
                	}
                }
                else if (contract != null)
                	contract.add(line);
                
                else if (line.startsWith(Pattern.PATTERN_TOKEN)) {
                	if (onDeck != null)
                        add(onDeck);
                    onDeck = new Pattern(line.substring(Pattern.PATTERN_TOKEN.length()));
                }
                else {
                    onDeck.raw(line);
                }
            }
            add(onDeck);
        } catch (Throwable t) {
            Console.warn(t); return;
        } finally {
            if (scanner != null) scanner.close();
        }
        if (isEmpty()) 
        	add(new Pattern("A"));
        current = get(0);
        view.fillPatterns();
        MainFrame.get().getBeatBox().changeTrack(this);
        RTLogger.log(this, name + " loaded " + file.getName() + " (" + kit.size() + ") on " + midiOut.getShortName());
    }	
	
	public void setCurrent(Pattern p) {
		if (current == p)
			return;
		current = p;
		int idx = indexOf(p);
		if (view.getPattern().getSelectedIndex() != idx) 
			view.getPattern().setSelectedIndex(idx);
		if (grid.getPattern().getSelectedIndex() != idx) {
			grid.getPattern().setSelectedIndex(idx);
			grid.repaint();
		}
	}
	
	public void next(boolean forward) {
		int result = indexOf(current);
        if (forward) {
        	result ++;
        	if (result >= size() - 1)
        		setCurrent(get(0));
        	else setCurrent(get(result));
        	return;
        }
        
    	if (result == 0) 
    		result = size() - 1;
    	else 
    		result--;
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
		this.active = active;
		if (!active)
			current.noteOff(midiOut);
		MainFrame.update(this);
	}
	
	public void selectFile(int data2) {
		// TODO!	
	}

	public void step(int step) {
		if (grid.isVisible()) grid.step(step);
		if (!active)
			return;
		
		Notes now = current.get(step);
		if (now == null)
			return;
		for (ShortMessage msg : now) {
			if (Midi.isNoteOn(msg) && isDrums()) {
				Midi note = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
						((int)(msg.getData2() * getKit().velocity(msg.getData1()) * gain))); 
				JudahMidi.queue(note, midiOut);
			}
			else 
				JudahMidi.queue(msg, midiOut);
		}
	}

	public final void setFile(File file) {
		clear();
		kit.init();
		this.file = file;
        if (file != null && file.isFile())
        	new Thread(this).start();
	}

    public void write(File f) {
        StringBuffer raw = new StringBuffer();
        raw.append(getMidiOut().getShortName()).append("/");
        raw.append(getInstrument() == null ? "none" : getInstrument());
        raw.append("/").append(steps);
        raw.append("/").append(div).append(Constants.NL);
        if (isDrums())
        	raw.append(kit.toFile());
        for (Pattern pattern : this) {
            raw.append(pattern.forSave(isDrums()));
        }
        try {
            Constants.writeToFile(f, raw.toString());
        } catch (Exception e) { Console.warn(e); }
    }

    
}


