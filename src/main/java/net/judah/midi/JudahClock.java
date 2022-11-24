package net.judah.midi;

import static net.judah.api.Notification.Property.*;

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
import net.judah.api.Notification;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.api.TimeProvider;
import net.judah.drumz.DrumMachine;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.SyncWidget;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.samples.Sampler;
import net.judah.tracker.DrumTracks;
import net.judah.tracker.SynthTracks;
import net.judah.tracker.Track;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock extends Thread implements TimeProvider, TimeListener {
	public static int TEMPO_CC = 15; // midi mapped at lv2 clock plugin
	public static final byte[] MIDI_RT_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	public static final byte[] MIDI_RT_START    = new byte[] {(byte)ShortMessage.START}; 
	public static final byte[] MIDI_RT_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	public static final byte[] MIDI_RT_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	private static final float MAX_TEMPO = 300f; 
	public static final float MIDI_24 = 24;
	
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	
    @Setter private JackPort clockOut;
    @Getter private Mode mode = Mode.Midi24;
    @Getter private boolean active;
    @Getter private float tempo = 89f;
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
    @Getter private double interval = Constants.millisPerBeat(tempo) / MIDI_24;
    private long lastBeat = lastPulse;
	private int midiPulseCount;
	
	@Getter private DrumTracks beats;
	@Getter private SynthTracks notes;
	@Setter private Sampler sampler;

    /** current beat */
	@Getter private static int beat = -1;
    /** current step */
	@Getter private int step = 0;
    /** current number of bars to record/computeTempo */
	@Getter private static int length = 4; 
    @Getter private int steps = 16;
    @Getter private int subdivision = 4;
    private int _measure = steps / subdivision;

	@Getter private static boolean loopSync = true;
	private static SelectType onDeck;
    private static final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private TimeNotifier source;
    
    private final MidiClock midiClock;
    
    /** sync to external Midi24 pulse */
	public JudahClock(DrumMachine drums) {
		beats = new DrumTracks(this, drums);
		notes = new SynthTracks(this);
		
		MidiClock stub = null;
		try {
			stub = new MidiClock();
			stub.start();
		} catch (Exception e) {
			RTLogger.warn(this, e);
			mode = Mode.Internal;
		}
		midiClock = stub;
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
	
	/** Run internal Clock in real-time */
	public void process() throws JackException {
        if (mode != Mode.Internal) return;
        
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
				JackMidi.eventWrite(clockOut, JudahMidi.ticker(), MIDI_RT_CLOCK, CLOCK_SZ);

    		// supports 2,3,4,6,8 subdivision
			if (0 == midiPulseCount % (MIDI_24 / _measure)) {
				step();
				interval = Constants.millisPerBeat(tempo) / MIDI_24;
			}
    	}
    	
	}

	/** convert bpm to data1 and write to tempo control port */
	public void writeTempo(int bpm) {
		midiClock.writeTempo(bpm);
		// LV2 plugin method: 40bpm = data1 0, 106bpm = data1 50, 172bpm = data1 100  
		// JudahMidi.queue(Midi.create(Midi.CONTROL_CHANGE, 0, TEMPO_CC, (int)((bpm - 40) * 0.7575f)), JudahZone.getMidi().getTempo());
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
				JudahMidi.queue(new CC(0, 15, (int) (tempo - 40)), JudahZone.getMidi().getTempo());
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
			beat++;
    		if (beat % getMeasure() == 0) 
    			shout(BARS, Math.round(beat / getMeasure()));
			shout(BEAT, beat);
        }

		shout(STEP, step);
		
		new Thread( () -> {
			if (sampler.getStepSample() != null)
				sampler.getStepSample().step(step);
			for (Track t : beats)
				t.step();
			for (Track t : notes)
				t.step();
		}).run();

        step++;
        if (step == steps)
            step = 0;	 	
    }

	
	/** receive clock message from external source in real-time */
	public void processTime(byte[] bytes) throws JackException {
		if (mode != Mode.Midi24) return;
		int stat = 0;
        if (bytes.length == CLOCK_SZ)
            stat = bytes[0] & 0xFF;
        else if (bytes.length == 2) {
            stat = bytes[1] & 0xFF;
            stat = (stat << 8) | (bytes[0] & 0xFF);
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
            JudahZone.getMidi().synchronize(bytes);
            RTLogger.log(JudahMidi.class.getSimpleName(), "CONT MidiSync");
        }
        
        // process external time
        else if (ShortMessage.TIMING_CLOCK == stat) {
    		if (clockOut != null) //fwd midi24 clock tick 
    			JackMidi.eventWrite(clockOut, JudahMidi.ticker(), MIDI_RT_CLOCK, CLOCK_SZ);
    
    		midiPulseCount++;
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

	@Override
    public void begin() {
	    active = true;
	    
	    lastPulse = lastBeat = System.currentTimeMillis();
	    midiPulseCount = 0;
	    step = 0;
		beat = -1;
		for (Track t : JudahZone.getBeats())
			t.setStep(-1);
		for (Track t : JudahZone.getNotes())
			t.setStep(-1);
		JudahZone.getMidi().synchronize(MIDI_RT_START);
		shout(TRANSPORT, JackTransportState.JackTransportStarting);
		step();
		RTLogger.log(JudahMidi.class.getSimpleName(), "PLAY MidiSync");
	}

	@Override
    public void end() {
	    active = false;
	    JudahZone.getMidi().synchronize(MIDI_RT_STOP);
	    shout(TRANSPORT, JackTransportState.JackTransportStopped);
	    RTLogger.log(JudahMidi.class.getSimpleName(), "STOP MidiSync");
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	    lastPulse = lastBeat = System.currentTimeMillis();
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
		JudahZone.getMidiGui().length(bars);
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
		RTLogger.log(JudahClock.class, "Waiting... (" + syncType.name() + " " + length + " bars)");
	}
	
	public void setMode(Mode mode) {
		this.mode = mode;
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
		MainFrame.update(this);
	}
	
	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}

	public void setLoopSync(boolean active) {
		Looper looper = JudahZone.getLooper();
		if (!active && source != null)
				source.removeListener(this);
		else if (active && looper.getRecordedLength() > 0) {
			float tempo = Constants.computeTempo(looper.getRecordedLength(), length * getMeasure());
			writeTempo(Math.round(tempo));
			listen(looper.getPrimary());
			RTLogger.log(this, "Tempo Sync: " + tempo);
		}
		loopSync = active;
	}

}


