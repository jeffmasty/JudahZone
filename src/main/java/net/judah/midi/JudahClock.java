package net.judah.midi;

import static net.judah.api.Notification.Property.*;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPosition;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.samples.Sampler;
import net.judah.seq.Seq;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock extends Thread implements TimeProvider, TimeListener {
	public static final byte[] MIDI_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	public static final byte[] MIDI_START    = new byte[] {(byte)ShortMessage.START}; 
	public static final byte[] MIDI_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	public static final byte[] MIDI_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	public static final float MAX_TEMPO = 300f; 
	public static final int MIDI_24 = 24;
	
	public static enum Mode { Internal, /**24 pulses per quarter note external midi clock*/ Midi24 };
	
    @Getter private Mode mode = Mode.Midi24;
    @Getter private boolean active;
    @Getter private float tempo = 89f;
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
    private long lastBeat = lastPulse;
	private int midiPulseCount;
	@Getter float interval = Constants.millisPerBeat(tempo) / (float)MIDI_24;

	@Setter private Seq seq;
	private final Sampler sampler;
    @Getter private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private final MidiClock midiClock;
    private Loop source;
	
    /** current number of bars to record/computeTempo */
	@Getter private int length = 4; 
    /** current step */
	@Getter private int step = 0;
    /** current beat */
	@Getter private int beat;
	@Getter private int bar;
    @Getter private int steps = 16;
    @Getter private int subdivision = 4;
    @Getter private int measure = steps / subdivision;
    @Getter private int unit = MIDI_24 / subdivision;
    
    /** sync to external Midi24 pulse */
	public JudahClock(Sampler sampler) throws Exception {
		midiClock = new MidiClock();
		midiClock.start();
		this.sampler = sampler;
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

	/** in real-time */
	private void announce(Property prop, Object value) {
		for (Object o : listeners.toArray())
			((TimeListener)o).update(prop, value);
	}
	
	/** send to background thread */
	private void letItBeKnown(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}
	
	private void midi24() {
		midiPulseCount++;
		if (midiPulseCount >= MIDI_24)
			midiPulseCount = 0;
		if (midiPulseCount == 0) { // new Beat
			computeTempo();
			if (active) {
				beat++;
				if (beat >= measure) { // new Bar
					bar++; beat = 0;
					announce(Property.BARS, bar);
				}
				announce(Property.BEAT, beat);
			}
		}
		if (0 == midiPulseCount % unit) {
			step = beat * subdivision + midiPulseCount / unit;// measure; 
			letItBeKnown(STEP, step);
			if (active)
				sampler.step(step);
		}
		if (active) 
			seq.process(beat + midiPulseCount / (float)MIDI_24);
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
        
        if (ShortMessage.TIMING_CLOCK == stat) {
        	midi24(); // process external time
        }
        else if (ShortMessage.START == stat) {
            begin();
        }
        else if (ShortMessage.STOP == stat) {
            end();
        }
        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            letItBeKnown(TRANSPORT, JackTransportState.JackTransportRolling); 
            JudahZone.getLooper().getLoopA().getSync().syncUp();
            JudahZone.getMidi().synchronize(bytes);
            RTLogger.log(JudahMidi.class.getSimpleName(), "CONT MidiSync");
        }
	}

	@Override
    public void begin() {
	    
	    lastPulse = lastBeat = System.currentTimeMillis();
	    midiPulseCount = 0;
	    step = 0;
		beat = 0;
		bar = 0;
		JudahZone.getMidi().synchronize(MIDI_START);
		letItBeKnown(TRANSPORT, JackTransportState.JackTransportStarting);
	    active = true;
		RTLogger.log(JudahMidi.class.getSimpleName(), "PLAY MidiSync");

	}

	@Override
    public void end() {
	    active = false;
	    JudahZone.getMidi().synchronize(MIDI_STOP);
	    letItBeKnown(TRANSPORT, JackTransportState.JackTransportStopped);
	    RTLogger.log(JudahMidi.class.getSimpleName(), "STOP MidiSync");
	}

	public void reset() {
		end();
		step = 0;
        beat = -1;
	    lastPulse = lastBeat = System.currentTimeMillis();
	    RTLogger.log(this, "reset");
	}
	
	/** convert bpm to data1 and write to tempo control port */
	@Override
	public void writeTempo(int bpm) {
		midiClock.writeTempo(bpm);
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

	@Override public void update(Property prop, Object value) {
		if (prop.equals(LOOP)) { 
			if (!isActive()) 
				begin();
			JudahZone.getLooper().removeListener(this);
		}
		else if (Status.NEW == value) { // fresh loop
			JudahZone.getLooper().removeListener(this);
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
		MainFrame.update(this);
	}
	
	public boolean isLooperSync() {
		return source != null;
	}
	
	public void syncToLoop() {
		Looper looper = JudahZone.getLooper();
		if (looper.getRecordedLength() > 0) {
			float tempo = Constants.computeTempo(looper.getRecordedLength(), length * getMeasure());
			writeTempo(Math.round(tempo));
			looper.addListener(this);
			RTLogger.log(this, "Tempo Sync: " + tempo);
		}
	}
	
	public void listen(final Loop target) {
		source = target;
		if (false == JudahZone.getLooper().getListeners().contains(this))
			JudahZone.getLooper().addListener(this);
	}
		
	public void setMode(Mode mode) {
		this.mode = mode;
		RTLogger.log(this, "Clock: " + mode);
	}

	public void toggleMode() {
		setMode(mode == Mode.Internal ? Mode.Midi24 : Mode.Internal);
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
		int old = measure;
		measure = steps/subdivision;
		if (old == measure)
			return;
		unit = MIDI_24 / subdivision;
		MainFrame.update(this);
		letItBeKnown(Property.MEASURE, measure);
	}
	
	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}

	private void computeTempo() {
		long now = System.currentTimeMillis();
		float tempo2 = Constants.bpmPerBeat(now - lastPulse);
		if ((int)tempo2 != (int)tempo) {
			tempo = tempo2;
			letItBeKnown(Property.TEMPO, tempo);
		}
		lastPulse = now;
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

    		// supports 2,3,4,6,8 subdivision
			if (0 == midiPulseCount % (MIDI_24 / measure)) {
				step++;
				if (step >= steps)
					step = 0;
				
				letItBeKnown(STEP, step);
				if (!active) 
					return;
				if (sampler.getStepSample() != null)
					sampler.getStepSample().step(step);

				interval = Constants.millisPerBeat(tempo) / MIDI_24;
			}
    	}
	}

}

// @Setter private JackPort clockOut;
// midi24():if (clockOut != null) fwd midi24 clock tick (currently nobody listening on port) 
// 		JackMidi.eventWrite(clockOut, JudahMidi.ticker(), MIDI_RT_CLOCK, CLOCK_SZ);
// internal(): if (clockOut != null)  send midi24 clock tick 
// JackMidi.eventWrite(clockOut, JudahMidi.ticker(), MIDI_RT_CLOCK, CLOCK_SZ);
// Write Tempo LV2 plugin method: 40bpm = data1 0, 106bpm = data1 50, 172bpm = data1 100  
// JudahMidi.queue(Midi.create(Midi.CONTROL_CHANGE, 0, TEMPO_CC, (int)((bpm - 40) * 0.7575f)), JudahZone.getMidi().getTempo());


