package net.judah.midi;

import static net.judah.api.Notification.Property.*;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.*;
import net.judah.api.Notification.Property;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.Sample;
import net.judah.looper.SyncWidget;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock extends Thread implements TimeProvider, TimeListener {
	public static int TEMPO_CC = 15; // midi mapped at lv2 clock plugin
	static final byte[] MIDI_RT_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	static final byte[] MIDI_RT_START    = new byte[] {(byte)ShortMessage.START}; 
	static final byte[] MIDI_RT_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	static final byte[] MIDI_RT_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	private static final float MAX_TEMPO = 300f; 
	public static final float MIDI_24 = 24;
	@Getter public static final String[] timeSignatures = new String[] {
			"4/4", "Swing", "6/8", "Waltz", "Polka", "5/4"
	};
	static final float TEMPO_LOW = 40f; // lv2 clock plugin
	static final float TEMPO_HIGH = 172f;
	
	public static final Integer[] LENGTHS= {1, 1, 2, 2, 2, 3, 4, 4, 4, 4, 5, 6, 6, 7, 8, 8, 8, 8, 9, 10, 10, 11, 12, 12, 12, 13, 14, 15, 
			16, 16, 16, 16, 17, 18, 19, 20, 20, 21, 22, 23, 24, 24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 32, 33, 34, 35, 36, 36, 40, 48, 64};
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	
	@Getter private static JudahClock instance;
	private final JudahMidi midi;
    private final JackPort clockOut;

    @Getter private Mode mode = Mode.Midi24;
    @Getter private boolean active;
    @Getter private float tempo = 89f;
    
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
    @Getter private double interval = Constants.millisPerBeat(tempo) / MIDI_24;
    private long lastBeat = lastPulse;
	private int midiPulseCount;

    /** current beat */
	@Getter @Setter private static int beat = -1;
    /** current step */
	@Getter private int step = 0;
    /** current number of bars to record/computeTempo */
	@Getter private static int length = 4; 

    @Getter private int steps = 16;
    @Getter private int subdivision = 4;
    private int _measure = steps / subdivision;
    @Getter private final TimeSigGui gui;

	/** Loop Synchronization */
	@Setter @Getter private static boolean loopSync = true;
	private static SelectType onDeck;
    private static final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private TimeNotifier source;
    private final ArrayList<TimeListener> listeners = new ArrayList<>();

    @Getter private static Tracker tracker;
    private Sample crickets = null;
    // @Getter private static ArrayList<Sample> samples = new ArrayList<>();
    // @Getter private BeatBuddy drummachine = new BeatBuddy();
    
	public JudahClock(JudahMidi midiSystem) {
		
		midi = midiSystem;
		clockOut = midi.getClockOut();
		instance = this;
	    tracker = new Tracker(this, midiSystem);
	    gui = new TimeSigGui(this, midiSystem);
	    setPriority(8);
	    start();
	}

	@Override
	public void run() {
		try {
			while (true) {
				Notification n = notifications.take();
				new ArrayList<TimeListener>(listeners).forEach(
						listener -> {listener.update(n.prop, n.value);});
			}
		} catch (Exception e) {
			RTLogger.warn(this, e);		
		}
	}

	private void shout(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}
	
	/** Run the Clock in real-time */
	public void process() throws JackException {
        if (mode != Mode.Internal) return;
        
        int increment = 0;
        long current = System.currentTimeMillis();
        
        if (current - lastBeat >= Math.floor(midiPulseCount * interval)) {
		
        	lastPulse = current;
        	midiPulseCount++;
    		if (midiPulseCount > MIDI_24) {
    			midiPulseCount = 1;
    			lastBeat = current;
    		}

        	if (clockOut != null) 
    			//send midi24 clock tick 
				JackMidi.eventWrite(clockOut, increment++, MIDI_RT_CLOCK, CLOCK_SZ);

    		// supports 2,3,4,6,8 subdivision
			if (0 == midiPulseCount % (MIDI_24 / _measure)) {
				step();
				interval = Constants.millisPerBeat(tempo) / MIDI_24;
			}
    	}
    	
	}

	/** convert bpm to data1 and write to tempo control port */
	public void writeTempo(int bpm) {
		// 40bpm = data1 0, 106bpm = data1 50, 172bpm = data1 100  
		int val = (int)((bpm - 40) * 0.7575f);
		JudahMidi.queue(Midi.create(Midi.CONTROL_CHANGE, 0, TEMPO_CC, val), midi.getTempo());
	}
	
	/** see also writeTempo() */
	@Override
	public boolean setTempo(float tempo2) {
		if (tempo2 > MAX_TEMPO) {
			RTLogger.warn(this, tempo2 + " ignored.");
			return true;
		}
		if (tempo2 == tempo)
			return true;

		tempo = tempo2;
		if (mode == Mode.Internal) {
			try {
				JudahMidi.queue(new CC(0, 15, (int) (tempo - 40)), midi.getTempo());
			} catch (InvalidMidiDataException e) {
				RTLogger.warn(this, e);
			}
		}
		shout(TEMPO, tempo);
		RTLogger.log(this, "tempo " + tempo);
		return true;
	}
	
	private void step() {
		
		if (!active) {
	        step++;
	        if (step >= steps) {
	            step = 0;
	        }
			shout(STEP, step);
			return;
		}
		
		if (step % getSubdivision() == 0) {
        	shout(BEAT, ++beat);
        	
    		if (beat % getMeasure() == 0) 
    			shout(BARS, Math.round(beat / getMeasure()));
        }

		shout(STEP, step);

		new Thread( () -> {
			if (crickets != null)
				crickets.step(step);
			for (Track track : Tracker.getTracks())
				track.step();
		}).run();

        
        step++;
        if (step == steps)
            step = 0;	 	
    }

	
	/** receive clock message from external source in real-time */
	public void processTime(byte[] midi) throws JackException {
		int stat = 0;
        if (midi.length == CLOCK_SZ)
            stat = midi[0] & 0xFF;
        else if (midi.length == 2) {
            stat = midi[1] & 0xFF;
            stat = (stat << 8) | (midi[0] & 0xFF);
        }
        else return;

        if (ShortMessage.START == stat) {
            begin();
        }

        else if (ShortMessage.STOP == stat) {
            end();
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            shout(TRANSPORT, JackTransportState.JackTransportRolling); 
            waiting(JudahZone.getLooper().getLoopA());
            JudahMidi.synchronize(midi);
            RTLogger.log(JudahMidi.class.getSimpleName(), "CONT MidiSync");

        }
        
        // process external time
        if (ShortMessage.TIMING_CLOCK == stat) {
        	if (clockOut != null) {
    			//fwd midi24 clock tick 
        		midiPulseCount++;
				JackMidi.eventWrite(clockOut, JudahMidi.getInstance().getTicker(), MIDI_RT_CLOCK, CLOCK_SZ);
				// supports 2,3,4,6,8 subdivision
				if (0 == midiPulseCount % (MIDI_24 / (steps / subdivision))) {
					step();
					if (step == 0) {
						long now = System.currentTimeMillis();
						int tempo2 = Math.round(subdivision * Constants.bpmPerBeat(now - lastPulse));
						if (tempo != tempo2) {
							tempo = tempo2;
							shout(Property.TEMPO, tempo);
						}
						lastPulse = now;
					}
				}
        	}
        }
        // else RTLogger.log(this, "unknown clock midi signal " + new Midi(midi));

	}
	

	@Override
    public void begin() {
	    active = true;
	    
	    shout(TRANSPORT, JackTransportState.JackTransportStarting);
	    lastPulse = lastBeat = System.currentTimeMillis();
	    midiPulseCount = 0;
	    step = 0;
		beat = -1;
		JudahMidi.synchronize(MIDI_RT_START);
		RTLogger.log(JudahMidi.class.getSimpleName(), "PLAY MidiSync");

		step();
	}

	@Override
    public void end() {
		// if (mode == Mode.Midi24) 
			// BeatBuddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);
	    active = false;
	    JudahMidi.synchronize(MIDI_RT_STOP);
	    RTLogger.log(JudahMidi.class.getSimpleName(), "STOP MidiSync");

	    shout(TRANSPORT, JackTransportState.JackTransportStopped);
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	    lastPulse = lastBeat = System.currentTimeMillis();
	    //shout(BEAT, beat);
	    RTLogger.log(this, "reset");
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


	public void listen(final Loop target) {
		if (source != null)
			source.removeListener(this);
		source = target;
		source.addListener(this);
	}
		
	@Override public void update(Property prop, Object value) {
		if (prop.equals(LOOP)) { 
			if (!isActive()) 
				begin();
			source.removeListener(this);
		}
		else if (Status.TERMINATED == value) {
			source.removeListener(this);
		}
	}

	
	public void togglePlay() {
		if (isActive()) 
			end();
		else begin();
	}

	public void setLength(int bars) {
		if (length == bars) 
			return;
		length = bars;
		midi.getGui().length(bars);
		MainFrame.update(JudahZone.getLooper().getLoopA());
	}
	
	public SyncWidget getSynchronized() {
		for (TimeListener l : listeners)
			if (l instanceof SyncWidget)
				return (SyncWidget)l;
		return null;
	}
	
	public static boolean waiting(Loop loop) {
		if (onDeck == null) return false;
		switch(onDeck) {
			case ERASE: 
				loop.erase();
				break;
			case SYNC:
				loop.getSync().syncUp();
				break;
		}
		onDeck = null;
		return true;
	}

	public static void setOnDeck(SelectType syncType) {
		onDeck = syncType;
		RTLogger.log(getInstance(), "Waiting... (" + syncType.name() + " " + length + " bars)");
	}

	
	
	public void setMode(Mode mode) {
		this.mode = mode;
		//	if (mode == Mode.Internal)
		//		lastPulse = System.currentTimeMillis();
		RTLogger.log(this, "Clock: " + mode);
	}

	public void toggleMode() {
		setMode(mode == Mode.Internal ? Mode.Midi24 : Mode.Internal);
	}
	
	@Override
	public int getMeasure() {
		return _measure;
	}
	public void setSubdivision(int sub) {
		subdivision = sub;
		setMeasure();
	}
	public void setSteps(int s) {
		steps = s;
		setMeasure();
	}

	private void setMeasure() {
		_measure = steps/subdivision;
		gui.update();
	}
	
	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}
	
	public void crickets(boolean on) {
		Looper looper = JudahZone.getLooper();
		if (on) {
			if (crickets != null)
				crickets(false);
			new Thread(()->{
				try {
					crickets = new Sample("Crik", new File("/home/judah/cricket3.wav"), looper);
					crickets.getGain().setVol(100);
					looper.add(crickets);
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
			}).start();
		} else {
			looper.remove(crickets);
			crickets = null;
		}
	}

}


