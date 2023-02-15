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
import net.judah.api.ProcessAudio.Type;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.drumkit.Sampler;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.LoopMix;
import net.judah.seq.Seq;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class JudahClock extends Thread implements TimeProvider {
	public static final byte[] MIDI_CLOCK 	 = new byte[] {(byte)ShortMessage.TIMING_CLOCK};
	public static final byte[] MIDI_START    = new byte[] {(byte)ShortMessage.START}; 
	public static final byte[] MIDI_CONTINUE = new byte[] {(byte)ShortMessage.CONTINUE};
	public static final byte[] MIDI_STOP     = new byte[] {(byte)ShortMessage.STOP};
	static final int CLOCK_SZ = 1;
	public static final float MAX_TEMPO = 300f; 
	public static final int MIDI_24 = 24;

	@Setter private Seq seq;
	private final Sampler sampler;
    @Getter private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private final MidiClock midiClock;
    //private Loop source;
    private boolean onDeck;

    @Getter private boolean active;
    @Getter private float tempo = 89f;
    /** jack frames since transport start or frames between pulses */
    @Getter private long lastPulse = System.currentTimeMillis();
	private int midiPulseCount;
	@Getter float interval = Constants.millisPerBeat(tempo) / (float)MIDI_24;
    /** current number of bars to record/computeTempo */
	@Getter private int length = 4; 
    /** current step */
	@Getter private int step = 0;
    /** current beat */
	@Getter private int beat;
	@Setter @Getter private int bar;
	@Getter private Signature timeSig = Signature.FOURFOUR;
    @Getter private int measure = timeSig.steps / timeSig.div;
    @Getter private int unit = MIDI_24 / timeSig.div;
    
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
						listener -> {if (listener != null) listener.update(n.prop, n.value);});
			}
		} catch (Exception e) {
			RTLogger.warn(this, e);		
		}
	}

	private void midi24() {
		midiPulseCount++;
		if (midiPulseCount >= MIDI_24)
			midiPulseCount = 0;
		if (midiPulseCount == 0) { // new Beat
			computeTempo(getTempo());
			if (active) {
				beat++;
				if (beat >= measure) { // new Bar
					bar++; beat = 0;
					if (bar >= length)
						bar = 0;
					announce(Property.BARS, bar);
				}
				announce(Property.BEAT, beat);
			}
		}
		if (0 == midiPulseCount % unit) {
			step = beat * timeSig.div + midiPulseCount / unit;// measure; 
			letItBeKnown(STEP, step);
			if (active)
				sampler.step(step);
		}
		if (!active)
			return;
		float percent = beat + midiPulseCount / (float)MIDI_24; //  + (isEven() ? 0 : 1);
		seq.getTracks().forEach(track->track.playTo(percent));
	}
	
	public boolean isEven() { return bar % 2 == 0; }

	
	/** receive clock message from external source in real-time */
	public void processTime(byte[] bytes) throws JackException {
		// if (mode != Mode.Midi24) return;
		int stat = 0;
        if (bytes.length == CLOCK_SZ)
            stat = bytes[0] & 0xFF;
        else if (bytes.length == 2) {
            stat = bytes[1] & 0xFF;
            stat = (stat << 8) | (bytes[0] & 0xFF);
        }
        else return;
        
        if (ShortMessage.TIMING_CLOCK == stat) 
        	midi24(); // process external time
        
        else if (ShortMessage.START == stat) 
            begin();
        
        else if (ShortMessage.STOP == stat) 
            end();
        
        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI24 CONTINUE");
            letItBeKnown(TRANSPORT, JackTransportState.JackTransportRolling); 
            JudahZone.getMidi().synchronize(bytes);
            RTLogger.log(JudahMidi.class.getSimpleName(), "CONT MidiSync");
        }
	}

	@Override public void addListener(TimeListener l) {
		if (!listeners.contains(l)) listeners.add(l);
	}

	@Override public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	public void syncUp(Loop loop, int init) {
		LoopMix sync = (LoopMix)JudahZone.getMixer().getFader(loop);
		if (listeners.contains(sync)) {
			syncDown(loop);
			MainFrame.update(loop);
			return;
		}
		int bars = loop.getType() == Type.BSYNC && !loop.hasRecording() ? LoopMix.BSYNC_UP : length;
		sync.setup(bars, init);
		addListener(sync);
	}

	public void syncFlush() {
		for (TimeListener listener : new ArrayList<TimeListener>(listeners)) 
			if (listener instanceof LoopMix)	{
				LoopMix sync = (LoopMix)listener;
				listeners.remove(listener);
				MainFrame.update(sync.getLoop());
			}
	}

	public void tail(Loop loop) {
		LoopMix sync = (LoopMix)JudahZone.getMixer().getFader(loop);
		sync.setBars(LoopMix.BSYNC_DOWN);
		MainFrame.update(sync);
	}
	
	public void syncDown(Loop loop) {
		removeListener((LoopMix)JudahZone.getMixer().getFader(loop));
	}

	public boolean isSync(Loop loop) {
		for (int i = 0; i < listeners.size(); i++) 
			if (listeners.get(i) instanceof LoopMix && ((LoopMix)listeners.get(i)).getLoop() == loop)
				return true;
		return false;
	}

	public boolean isLooperSync() {
		return onDeck;
	}

	public void syncToLoop() {
		Looper looper = JudahZone.getLooper();
		if (looper.getRecordedLength() > 0) {
			float tempo = Constants.computeTempo(looper.getRecordedLength(), length * getMeasure());
			writeTempo(Math.round(tempo));
			onDeck = true;
			RTLogger.log(this, "Tempo Sync: " + tempo);
		}
	}

	@Override
    public void begin() {
	    midiPulseCount = -1;
	    step = 0;
		beat = -1;
		bar = 0;
		active = true;
		announce(TRANSPORT, JackTransportState.JackTransportStarting);
		JudahZone.getMidi().synchronize(MIDI_START);
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
	    RTLogger.log(this, "reset");
	}
	
	/** convert bpm to data1 and write to tempo control port */
	@Override
	public void writeTempo(int bpm) {
		midiClock.writeTempo(bpm);
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
	
	
	public void listen() {
		onDeck = true;
	}
		
	public void setTimeSig(Signature time) {
		if (timeSig == time)
			return;
		timeSig = time;
		measure = timeSig.steps/timeSig.div;
		unit = MIDI_24 / timeSig.div;
		MainFrame.update(this); // both update and announce?
		letItBeKnown(Property.MEASURE, measure);
	}
	
	public int getSteps() { return timeSig.steps; }
	public int getSubdivision() { return timeSig.div; }
	
	public static String toString(JackPosition position) {
		return position.getBeat() + "/" + position.getBar() + " " + position.getTick() + "/" + position.getTicksPerBeat();
	}

	private void computeTempo(float avg) {
		long now = System.currentTimeMillis();
		float tempo2 = Constants.bpmPerBeat(now - lastPulse);
		lastPulse = now;
		if ((int)tempo2 != (int)tempo) {
			tempo = (tempo2 + avg) / 2f;
			letItBeKnown(Property.TEMPO, tempo);
		}
	}

	public void loop(Object value) {
		if (Status.NEW == value) 
			bar = 0;
		announce(Property.LOOP, value);
		if (onDeck && !active) 
			begin();
		onDeck = false;
	}

	public void cycle() {
		bar++;
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


	
}
