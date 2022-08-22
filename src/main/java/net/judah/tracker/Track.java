package net.judah.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.GMNames;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Panic;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** a step sequencer. 
 * Ch#,  file, midiOut/(AudioIn), pattern cycle  */
@Getter 
public abstract class Track extends ArrayList<Pattern> implements Runnable, TimeListener {

	public static final File DRUM_FOLDER = new File(Constants.ROOT, "patterns");
	public static final File MELODIC_FOLDER = new File(Constants.ROOT, "sequences");

	private static int counter;
	protected final String name;
	protected final JudahClock clock;
	protected final int num;
	
	protected final TrackView view;
	protected TrackEdit edit;

	@Setter protected String instrument;
	protected File file;
	@Setter protected JackPort midiOut;
	protected final int ch;
	protected final Cycle cycle = new Cycle(this);
	@Getter protected Pattern current;
	
    @Getter protected int steps = JudahClock.getSteps();
    @Getter protected int div = JudahClock.getSubdivision();
    @Getter @Setter protected boolean latch;

    @Setter protected int step = -1;
	protected boolean active;
	@Setter protected float gain = 0.8f;

	public Track(JudahClock clock, String name, int ch, final JackPort port) {
		this.ch = ch;
		this.name = name;
		this.clock = clock;
		clock.addListener(this);
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
	
	@Override public void update(Property prop, Object value) {
		if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStarting)
			step = -1;
		else if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStopped)
			if (active)
				new Panic(midiOut, ch).start();
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
                    if (split.length >= 3)
                    	steps = Integer.parseInt(split[2]);
                    if (split.length >= 4)
                    	div = Integer.parseInt(split[3]);
                    if (split.length >= 5) 
                    	cycle.setSelected(Integer.parseInt(split[4]));
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
            Console.warn(t); return;
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
		
		if (result == size())
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
		if (active)
			step = JudahClock.getStep() - 1;
		else if (isSynth())
			new Panic(midiOut, ch).start(); 
		this.active = active;
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
		if (step == steps) {
			step = 0;
			cycle.cycle();
		}
		step(step);
	}
	
	public void step(int step) {
		if (edit.isVisible()) edit.step(step);
		if (!active)
			return;
		
		Notes now = current.get(step);
		if (now == null)
			return;
		for (ShortMessage msg : now) {
			if (Midi.isNoteOn(msg)) {
				if (isDrums()) {
					Midi note = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
							((int)(msg.getData2() * ((DrumTrack)this).velocity(msg.getData1()) * gain))); 
					JudahMidi.queue(note, midiOut);
				}
				else {
					msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(),
								((int)(msg.getData2() * gain))); 
					if (latch) {
						JudahMidi.queue(Transpose.apply(msg), midiOut);
					}
					else 
						JudahMidi.queue(msg, midiOut);
				}
			}
			else 
				JudahMidi.queue(msg, midiOut);
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
        } catch (Exception e) { Console.warn(e); }
        for (Track t : clock.getTracks()) {
        	t.getView().getFilename().refresh();
        	t.getEdit().fillFile1();
        }
    }

    
}


