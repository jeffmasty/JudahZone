package net.judah.midi;

import static net.judah.api.Notification.Property.*;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification;
import net.judah.api.Notification.Property;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.drumkit.Sampler;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Folder;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
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

	/** current number of bars to record/computeTempo */
	@Getter private static int length = 4; 
	@Getter static private int bar;
	@Getter private final MidiClock midiClock;

	@Setter private Seq seq;
	private final Sampler sampler;
    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
    @Getter private boolean active;
    @Getter private float tempo = 89f;
	/** current step */
	@Getter private int step;
    /** current beat */
	@Getter private int beat;
	@Getter private Signature timeSig = Signature.FOURFOUR;
    private int unit = MIDI_24 / timeSig.div;
    private long lastPulse = System.currentTimeMillis();
	private int midiPulseCount;
    private boolean onDeck;
    private boolean runnersDialZero;
    
    /** sync to external Midi24 pulse */
	public JudahClock(Sampler sampler) throws Exception {
		midiClock = new MidiClock();
		midiClock.start();
		this.sampler = sampler;
		setPriority(8);
		start();
	}

	@Override public void addListener(TimeListener l) 	 { if (!listeners.contains(l)) listeners.add(l); }
	@Override public void removeListener(TimeListener l) { listeners.remove(l); }
	/** write to tempo control port */
	@Override public void writeTempo(int bpm)  { midiClock.writeTempo(bpm); }
	@Override public int getMeasure() 	{ return timeSig.beats; }
	public static boolean isEven() 		{ return bar % 2 == 0; }
	public int getSteps() 				{ return timeSig.steps; }
	public int getSubdivision() 		{ return timeSig.div; }

	@Override public void run() {
		try {
			while (true) {
				Notification n = notifications.take();
				new ArrayList<TimeListener>(listeners).forEach(
						listener -> {if (listener != null) listener.update(n.prop, n.value);});
			}
		} catch (Exception e) { RTLogger.warn(this, e); }
	}

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
            announce(TRANSPORT, JackTransportState.JackTransportRolling); 
            JudahZone.getMidi().synchronize(bytes);
            RTLogger.log(this, "MidiSync CONTINUE");
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
				if (beat >= timeSig.beats) { 
					bar++; // new Bar
					beat = 0;
					if (bar >= length)
						bar = 0;
					announce(Property.BARS, bar);
				}
				announce(Property.BEAT, beat);
			}
		}
		if (midiPulseCount % unit == 0) {
			step = beat * timeSig.div + midiPulseCount / unit;
			letItBeKnown(STEP, step);
			if (active) {
				JudahZone.getChords().step(step);
				sampler.step(step);
			}
		}
		if (!active)
			return;
		float percent = beat + midiPulseCount / (float)MIDI_24; //  + (isEven() ? 0 : 1);
		seq.getTracks().forEach(track->track.playTo(percent));
	}
	
	public void syncToLoop() {
		Looper looper = JudahZone.getLooper();
		if (looper.getPrimary() != null) {
			float tempo = Constants.computeTempo((long) (1000 * looper.getPrimary().seconds()), length * getMeasure());
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
	}

	@Override
    public void end() {
	    active = false;
	    JudahZone.getMidi().synchronize(MIDI_STOP);
	    letItBeKnown(TRANSPORT, JackTransportState.JackTransportStopped);
	}

	public void reset() {
		step = 0;
        beat = 0;
        bar = 0;
	}
	
	public void toggle() {
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
	
	public void setTimeSig(Signature time) {
		if (timeSig == time)
			return;
		timeSig = time;
		unit = MIDI_24 / timeSig.div;
		letItBeKnown(Property.SIGNATURE, timeSig);
		Folder.refillAll();
		MainFrame.update(this); // both update and announce?
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

	public void loopCount(Object value) {
		if (Status.NEW == value && runnersDialZero) 
			bar = 0;
		announce(Property.LOOP, value);
		if (onDeck && !active) 
			begin();
		onDeck = false;
	}

	public void skipBar() {
		bar++;
		letItBeKnown(Property.BARS, bar);
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

	public void syncTempo(Loop primary) {
		if (primary == null || !primary.isPlaying()) return;
		long millis = (long) (1000 * primary.seconds());
		int tempo = (int)Constants.computeTempo(millis, length * timeSig.beats);
		RTLogger.log(this, "loop tempo: " + length + " bars @ " + tempo + " bpm");
		writeTempo(tempo);
	}

	public void runnersDialZero() {
		runnersDialZero = !runnersDialZero;
	}

}
