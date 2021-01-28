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

	private int count = -1;
	@Getter private int steps = 16;
	@Getter private int intro = 0;
	/** current step */
	@Getter private int step = 0;

	@Getter private String name = "zone";

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
			if (gui != null)
			    gui.getTempo().setText("Tempo: " + tempo);
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
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                raw.add(line);
            }
            scanner.close();
            if (raw.isEmpty())
                throw new FileNotFoundException("No patterns " + file);

        } catch (FileNotFoundException ex) {
            Console.warn(ex); return;
        }
        beatBox.clear();
        for (String saved: raw)
            beatBox.add(new DrumTrack(saved));
        if (gui != null) gui.initialize();
    }

    public void write(File f) {
        StringBuffer raw = new StringBuffer();
        for (DrumTrack t : beatBox)
            raw.append(t.forSave());
        try {
            Constants.writeToFile(f, raw.toString());
        } catch (Exception e) { Console.warn(e); }
    }


}


