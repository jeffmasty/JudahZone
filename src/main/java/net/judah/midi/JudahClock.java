package net.judah.midi;

import static judahzone.api.Notification.Property.STEP;
import static judahzone.api.Notification.Property.TRANSPORT;
import static judahzone.util.Constants.bpmPerBeat;
import static judahzone.util.Constants.millisPerBeat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;
import javax.swing.SwingUtilities;

import org.jaudiolibs.jnajack.JackTransportState;

import judahzone.api.MidiClock;
import judahzone.api.Notification;
import judahzone.api.Notification.Property;
import judahzone.gui.Gui;
import judahzone.api.Signature;
import judahzone.api.TimeListener;
import judahzone.api.TimeProvider;
import judahzone.util.Constants;
import judahzone.util.Debounce;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import judahzone.util.WavConstants;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.seq.chords.Chords;

/** tempo/beat/step computation and notification.
 *
 * To integrate OSC:
 *      // private OSCHandler oscHandler;
 *      // oscHandler = new OSCHandler(InetAddress.getLoopbackAddress(), OSC_PORT);
 *  - example send:
 *      // if (oscHandler != null && !internal)
 *      //     oscHandler.send("/tempo", Math.round(tempo));
 *  - cleanup
 *      // if (oscHandler != null) oscHandler.close();
 */
public class JudahClock implements MidiClock, TimeProvider {

	public static final int MAX_TEMPO = (int)Math.floor( WavConstants.FPS / MIDI_24 * 60f );
	public static final int MIN_TEMPO = 30;

	/** current number of bars to record/computeTempo */
	@Getter private static int length = 4;
	@Getter private volatile boolean active;
	@Getter private volatile float tempo = 90;
	@Getter private volatile Signature timeSig = Signature.FOURFOUR;
	@Getter private volatile int bar;
    /** current beat */
	@Getter private volatile int beat;
	/** current step of current beat */
	@Getter private volatile int step;
	/** clock ticks: send(T) vs. receive(F) */
	@Getter private volatile boolean internal = true;
    @Getter private volatile boolean onDeck;

    private float jackFramesPerTick;
    private float offFrames, onFrames;
    private float callCounter = 0;
    private long lastPulse = 0;
	private int midiPulseCount;
    private int unit = MIDI_24 / timeSig.div;
    private boolean offBeat;
    @Setter @Getter private volatile boolean eighths;
    @Getter private volatile float swing;

    private final JudahMidi midi;
    private final CopyOnWriteArrayList<TimeListener> listeners = new CopyOnWriteArrayList<>();
    private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private final Debounce debounce = new Debounce();
	private final JudahZone zone;

	// announcer thread control
	private volatile boolean announceRunning = true;
	private Thread announceThread;

    /** sync to external Midi24 pulse */
	public JudahClock(JudahMidi parent, JudahZone judahZone) {
		midi = parent;
		zone = judahZone;

		announceThread = new Thread(() -> {
			try {
				while (announceRunning) {
					Notification n = notifications.take();
					for (TimeListener listener : new ArrayList<>(listeners)) {
						try {
							if (listener != null) listener.update(n.prop(), n.value());
						} catch (Throwable t) {
							RTLogger.warn(this, t);
						}
					}
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}, "JudahClock-announcer");
		announceThread.setDaemon(true);
		try { announceThread.setPriority(9); } catch (Throwable ignored) {}
		announceThread.start();

		Services.add(this);
	}

	@Override public synchronized void addListener(TimeListener l) 	 { if (!listeners.contains(l)) listeners.add(l); }
	@Override public synchronized boolean removeListener(TimeListener l) { return listeners.remove(l); }
	/** beats in the current TimeSignature */
	@Override public int getMeasure() 	{ return timeSig.beats; }
	public int getSteps() 				{ return timeSig.steps; }
	public int getSubdivision() 		{ return timeSig.div; }
	public boolean isEven() 		{ return bar % 2 == 0; }

	@Override
	public void pulse() {
		if (!internal)
			return;
        callCounter += 1;
        if (callCounter >= (offBeat ? onFrames : offFrames)) { // swingless = jackFramesPerTick;
            callCounter -= (offBeat ? onFrames : offFrames); // keep remainder
			midi24();
        }
	}

	/** receive clock message from external source in real-time */
	public void processTime(byte[] bytes) {
		if (internal)
			return;
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
            midi.synchronize(bytes);
            RTLogger.log(this, "MidiSync CONTINUE");
        }
	}


	/** Run sequencers every timing clock msg, adjust for swing, check steps/bars */
	private synchronized void midi24() {
		midi.queue(MIDI_TICK);
		midiPulseCount++;
		if (midiPulseCount >= MIDI_24)
			midiPulseCount = 0;
		if (midiPulseCount == 0) { // new Beat
			if (!internal)
				averageTempo(tempo);
			if (active) {
				beat++;
				if (beat >= timeSig.beats)
					newBar();
				announce(Property.BEAT, beat);
			}
		}

		if (midiPulseCount % unit == 0) {
			step = beat * timeSig.div + midiPulseCount / unit;

			if (!eighths)
				offBeat = !offBeat;
			else if (step % 2 == 0)
				offBeat = !offBeat;

			passItOn(STEP, step);
			if (active) {
				zone.getSeq().step(step);
			}
		}
		if (!active)
			return;

		zone.getSeq().percent(beat + midiPulseCount / (float)MIDI_24);
	}

	public synchronized void setSwing(float s) throws NumberFormatException {
		if (s < -0.5f || s > .5f)
			throw new NumberFormatException("swing and a miss: " + s);
		swing = s;
		updateSwing();
		SwingUtilities.invokeLater(() -> MainFrame.update(zone.getMidiGui()));
	}

	private synchronized void updateSwing() {
		jackFramesPerTick = WavConstants.FPS / (tempo * 0.4f); // tempo / 60 * 24

		if (timeSig.div == 3) // square triplets
			offFrames = onFrames = jackFramesPerTick;
		else {
			offFrames = jackFramesPerTick + jackFramesPerTick * swing;
			onFrames = 2 * jackFramesPerTick - offFrames;
		}
	}

	private synchronized void newBar() {
		bar++; // new Bar
		beat = 0;
		announce(Property.BARS, bar);
		if (bar % length == 0 && !debounce.doubleTap())
			announce(Property.BOUNDARY, bar / length);
	}

	@Override public synchronized void begin() {
		lastPulse = 0;
	    midiPulseCount = -1;
	    step = 0;
	    offBeat = false;
		beat = -1;
		bar = 0;
		active = true;
		announce(TRANSPORT, JackTransportState.JackTransportStarting);
		midi.synchronize(MIDI_START);
		offSync();
	}

	@Override public synchronized void end() {
	    active = false;
	    midi.synchronize(MIDI_STOP);
	    passItOn(TRANSPORT, JackTransportState.JackTransportStopped);
	}

	@Override public synchronized void reset() {
		step = beat = bar = 0;
        announce(Property.STATUS, "reset");
	}

	public synchronized void toggle() {
		if (active)
			end();
		else begin();
	}

	public synchronized void setLength(int bars) {
		if (length == bars)
			return;
		length = bars;
		SwingUtilities.invokeLater(() -> MainFrame.update(this));
	}

	@Override public synchronized void setTimeSig(Signature time) {
		if (timeSig == time)
			return;
		timeSig = time;
		unit = MIDI_24 / timeSig.div;
		updateSwing();
		passItOn(Property.SIGNATURE, timeSig);
//		Folder.refillAll();
		SwingUtilities.invokeLater(() -> MainFrame.update(this)); // both update and announce?
	}

	private synchronized void averageTempo(float avg) {
		if (lastPulse <= 0) {
			lastPulse = System.currentTimeMillis();
			return;
		}
		long now = System.currentTimeMillis();
		float tempo2 = bpmPerBeat(now - lastPulse);
		lastPulse = now;
		if ((int)tempo2 != (int)tempo)
			setTempo((tempo2 + avg) / 2f);
	}

	public synchronized void skipBar() {
		bar++;
		announce(Property.BARS, bar);
	}

	@Override public synchronized void setTempo(float bpm) {
		if (bpm > MAX_TEMPO || bpm < MIN_TEMPO)
			return;

		tempo = bpm;
		updateSwing();

		// OSC integration would go here:
		// if (!internal && oscHandler != null) {
		//     oscHandler.send("/tempo", Math.round(tempo));
		// }

		passItOn(Property.TEMPO, tempo);
	}

	public void inputTempo() {
        String input = Gui.inputBox("Tempo:");
        if (input == null || input.isEmpty()) return;
        try {
        	setTempo(Float.parseFloat(input));
        } catch (Throwable t) {
        	RTLogger.log(this, t.getMessage() + " -> " + input);
        }
	}

	// External Midi interface
	@Override public synchronized void start() {
		if (internal)
			return;
		// if (oscHandler != null) oscHandler.send("/start");
	}

	@Override public synchronized void stop() {
		if (internal)
			return;
		// if (oscHandler != null) oscHandler.send("/stop");
	}

	@Override public synchronized void cont() {
		if (internal)
			return;
		// if (oscHandler != null) oscHandler.send("/continue");
	}

	public synchronized void primary() {
		if (!internal)
			stop();

		internal = !internal;
		if (!internal)
			try {
				// create OSC handler when enabling external mode:
				// oscHandler = new OSCHandler(InetAddress.getLoopbackAddress(), OSC_PORT);
			} catch (Exception e) { RTLogger.warn(this, e); }
		setTempo(tempo);
	}

	@Override public synchronized void close() throws IOException {
		stop();
		// stop announcer
		announceRunning = false;
		if (announceThread != null) {
			announceThread.interrupt();
			try { announceThread.join(200); } catch (InterruptedException ignored) {}
			announceThread = null;
		}
		// remove from Services registry
		try { Services.remove(this); } catch (Throwable ignored) {}

		// If an OSCHandler were in use, close it here:
		// if (oscHandler != null) { oscHandler.close(); oscHandler = null; }
	}


	// Looper Integration
	public synchronized void loopCount(int count) {
		if (count == 0)
			bar = 0;

		if (!active) {
			announce(Property.BOUNDARY, count);
			if (onDeck)
				begin();
		}
		else if (!debounce.doubleTap())
			announce(Property.BOUNDARY, count);
	}

	/** set tempo based on length of looper recording and current number of measures */
	public synchronized void syncToLoop(Loop target) {
		if (target == null) return;
		onDeck = !onDeck;
		if (!onDeck)
			return;

		Looper looper = zone.getLooper();
		if (!looper.hasRecording())
			return;
		long millis = (long)(looper.getPrimary().seconds() * 1000);
		setTempo(Constants.computeTempo(millis, length * getMeasure()));
		// looper.clockSync(); // off FREE

		// Reset Chord Track
		Chords chords = zone.getChords();
		if (chords.isActive() && chords.getSection() != null)
			chords.click(chords.getSection().get(0));
	}

	public void offSync() {
		onDeck = false;
		SwingUtilities.invokeLater(() -> MainFrame.update(this));
	}

	/** Notification in background thread */
	private void passItOn(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}

	/**Notification in real-time (left synchronous) */
	private void announce(Property prop, Object value) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).update(prop, value);
	}

	public float syncUnit() {
		return millisPerBeat(getTempo()) / (float)getSubdivision(); // * 2
	}

}