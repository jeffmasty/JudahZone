package net.judah.beatbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;

public class JudahClock implements TimeProvider, Runnable, TimeListener {

    @Getter private static JudahClock instance = new JudahClock();
	@Getter final BeatBox beatBox = new BeatBox();

	private final ArrayList<TimeListener> listeners = new ArrayList<>();

	@Getter private float tempo = 80f;
	@Setter @Getter private int measure = 4;
	@Setter @Getter private int subdivision = 4;

	@Getter private int count = -1;
	@Setter @Getter private int steps = 16;

	/** current step */
	@Setter @Getter private int step = 0;
	@Setter @Getter private File file;

	private BeatBoxGui gui;
	@Getter @Setter private boolean active;

	/** jack frames since transport start or frames between pulses */
	@Getter private long lastPulse;

	private JudahClock() {
	}

	public BeatBoxGui getGui() {
	    if (gui == null) gui = new BeatBoxGui(this);
	    return gui;
	}

	@Override public void run() {

        Console.info("beatbox start");
	    while (active) {

	        // run the current beat
	        if (step % subdivision == 0) {
	            count++;
	            // Console.info("step: " + step + " beat: " + step/2f + " count: " + count);
	            listeners.forEach(listener -> {listener.update(Property.BEAT, count);});
	        }

	        listeners.forEach(listener -> {listener.update(Property.STEP, step);});
	        beatBox.process(step);

	        step++;
	        if (step == steps) {
	            step = 0;
	        }

	        long period = Constants.millisPerBeat(tempo * subdivision);
	        Constants.sleep(period);
	    }

	}

	@Override
    public void begin() {
	    active = true;
	    if (Sequencer.getCurrent() != null)
	        Sequencer.getCurrent().setClock(this);
	    new Thread(this).start();
	}

	@Override
    public void end() {
	    active = false;
		Console.info("beatbox end");
	}

	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 < tempo || tempo2 > tempo) {
			tempo = tempo2;
			listeners.forEach(l -> {l.update(Property.TEMPO, tempo);});
		}
		return true;
	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	@Override
    public void update(Property prop, Object value) {
	    if (Property.TEMPO == prop) {
	        setTempo((float)value);
	    }
	}

    public void parse(File file) {
        if (file == null || !file.isFile()) {
            Console.info("no pattern file " + file);
            return;
        }
        ArrayList<String> raw = new ArrayList<>();
        Scanner scanner = null;
        try {
            boolean first = true;
            scanner = new Scanner(file);
            beatBox.clear();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (first) {
                    String[] split = line.split("[/]");
                    beatBox.kit = GMKit.valueOf(split[0]);
                    steps = Integer.parseInt(split[1]);
                    subdivision = Integer.parseInt(split[2]);
                    first = false;
                }
                else
                    beatBox.add(new DrumTrack(line));
            }
            this.file = file;
        } catch (FileNotFoundException ex) {
            Console.warn(ex); return;
        } finally {
            if (scanner != null) scanner.close();
        }
        for (String saved: raw)
            beatBox.add(new DrumTrack(saved));
        if (gui != null) gui.initialize();
    }

    public void write(File f) {
        StringBuffer raw = new StringBuffer(beatBox.getKit().name());
        raw.append("/").append(steps);
        raw.append("/").append(subdivision).append(Constants.NL);
        for (DrumTrack t : beatBox)
            raw.append(t.forSave());
        try {
            Constants.writeToFile(f, raw.toString());
        } catch (Exception e) { Console.warn(e); }
    }


}


