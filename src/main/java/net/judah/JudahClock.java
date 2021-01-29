package net.judah;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.beatbox.BeatBox;
import net.judah.notebox.NoteBox;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;

public class JudahClock implements TimeProvider, Runnable, TimeListener {

    @Getter private static JudahClock instance = new JudahClock();

    private final ArrayList<TimeListener> listeners = new ArrayList<>();

    @Setter @Getter private BeatBox beatBox;
    @Setter @Getter private NoteBox noteBox;
    @Getter @Setter private boolean active;
	@Getter private float tempo = 80f;
	@Setter @Getter private int measure = 4;
	@Setter @Getter private int subdivision = 4;

	@Getter private int count = -1;
	@Setter @Getter private int steps = 16;

	/** current step */
	@Setter @Getter private int step = 0;
	/** jack frames since transport start or frames between pulses */
	@Getter private long lastPulse;

	private JudahClock() {
	    instance = this;
	    beatBox = new BeatBox();
	    noteBox = new NoteBox();
	}

	@Override public void run() {
	    String clazz = JudahClock.class.getSimpleName();
        Console.info(clazz + " start");
	    while (active) {

	        // run the current beat
	        if (step % subdivision == 0) {
	            count++;
	            // Console.info("step: " + step + " beat: " + step/2f + " count: " + count);
	            listeners.forEach(listener -> {listener.update(Property.BEAT, count);});
	        }

	        listeners.forEach(listener -> {listener.update(Property.STEP, step);});
	        beatBox.process(step);
	        noteBox.process(step);
	        step++;
	        if (step == steps) {
	            step = 0;
	        }

	        long period = Constants.millisPerBeat(tempo * subdivision);
	        Constants.sleep(period);
	    }
	    Console.info(clazz + " end");
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

}


