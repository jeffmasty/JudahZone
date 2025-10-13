package net.judah.midi;

//autowire JudahMidi, chords, sampler, looper, seq
import static net.judah.JudahZone.*;
import static net.judah.api.Notification.Property.STEP;
import static net.judah.api.Notification.Property.TRANSPORT;
import static net.judah.util.Constants.bpmPerBeat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackTransportState;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.Getter;
import net.judah.api.MidiClock;
import net.judah.api.Notification;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Folder;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.omni.WavConstants;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.IntProvider;
import net.judah.song.cmd.Param;
import net.judah.util.Constants;
import net.judah.util.Debounce;
import net.judah.util.RTLogger;

public class JudahClock implements MidiClock, TimeProvider, Cmdr {

	public static final int MAX_TEMPO = (int)Math.floor( WavConstants.FPS / MIDI_24 * 60f );

	public static final int OSC_PORT = 4040;

	/** current number of bars to record/computeTempo */
	@Getter private static int length = 4;
	@Getter private boolean active;
	@Getter private int bar;

	private final JudahMidi midi;

	@Getter private float tempo = 90;
	@Getter private Signature timeSig = Signature.FOURFOUR;
    /** current beat */
	@Getter private int beat;
	/** current step of current beat */
	@Getter private int step;
	/** clock ticks: send(T) vs. receive(F) */
	@Getter private boolean internal = true;
    @Getter private boolean onDeck;

	private long lastPulse = 0;
	private int midiPulseCount;
    private int unit = MIDI_24 / timeSig.div;
    private final ArrayList<TimeListener> listeners = new ArrayList<>();
    private final BlockingQueue<Notification> notifications = new LinkedBlockingQueue<>();
	private float jackFramesPerTick;
    private float callCounter = 0;
    private OSCPortOut osc;
	private final ArrayList<Object> oscData = new ArrayList<>();
	private Debounce debounce = new Debounce();

    /** sync to external Midi24 pulse */
	public JudahClock(JudahMidi parent) {
		midi = parent;

		Thread announce = new Thread(()->{
			try { while (true) {
					Notification n = notifications.take();
					new ArrayList<TimeListener>(listeners).forEach(
							listener -> {if (listener != null) listener.update(n.prop(), n.value());});
				}
			} catch (Exception e) { RTLogger.warn(this, e); }});
		announce.setPriority(9);
		announce.start();

		getServices().add(this);
	}

	@Override public void addListener(TimeListener l) 	 { if (!listeners.contains(l)) listeners.add(l); }
	@Override public boolean removeListener(TimeListener l) { return listeners.remove(l); }
	/** beats in the current TimeSignature */
	@Override public int getMeasure() 	{ return timeSig.beats; }
	public int getSteps() 				{ return timeSig.steps; }
	public int getSubdivision() 		{ return timeSig.div; }
	public boolean isEven() 		{ return bar % 2 == 0; }

	public void jackFrame() {
		if (!internal)
			return;
        callCounter += 1;
        if (callCounter >= jackFramesPerTick) {
            callCounter -= jackFramesPerTick;
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

	private void midi24() {
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
			passItOn(STEP, step);
			if (active) {
				getSeq().step(step);
			}
		}
		if (!active)
			return;
		float percent = beat + midiPulseCount / (float)MIDI_24; //  + (isEven() ? 0 : 1);
		getSeq().percent(percent);
	}

	@Override
    public void begin() {
		lastPulse = 0;
	    midiPulseCount = -1;
	    step = 0;
		beat = -1;
		bar = 0;
		active = true;
		announce(TRANSPORT, JackTransportState.JackTransportStarting);
		midi.synchronize(MIDI_START);
		offSync();
	}

	@Override
    public void end() {
	    active = false;
	    midi.synchronize(MIDI_STOP);
	    passItOn(TRANSPORT, JackTransportState.JackTransportStopped);
	}

	@Override
	public void reset() {
		step = beat = bar = 0;
        announce(Property.STATUS, "reset");
	}

	public void primary() {

		if (!internal)
			stop();

		internal = !internal;
		// TODO connect ports
		if (!internal)
			try {
				osc = external(); // TODO connect Midi Port ?
			} catch (Exception e) { RTLogger.warn(this, e); }
		setTempo(tempo);
	}

	/** @return external comm port to control clock */
	private OSCPortOut external() throws UnknownHostException, IOException {
		return new OSCPortOut(InetAddress.getLocalHost(), OSC_PORT);
	}

	public void toggle() {
		if (active)
			end();
		else begin();
	}

	public void setLength(int bars) {
		if (length == bars)
			return;
		length = bars;
		MainFrame.update(this);
	}

	@Override
	public void setTimeSig(Signature time) {
		if (timeSig == time)
			return;
		timeSig = time;
		unit = MIDI_24 / timeSig.div;
		passItOn(Property.SIGNATURE, timeSig);
		Folder.refillAll();
		MainFrame.update(this); // both update and announce?
	}

	private void averageTempo(float avg) {
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

	public void skipBar() {
		bar++;
		passItOn(Property.BARS, bar);
	}

	public int countdown() {
		return beat % timeSig.beats - timeSig.beats;
	}

	@Override
	public void setTempo(float bpm) {
		if (bpm > MAX_TEMPO)
			return;
		tempo = bpm;
			jackFramesPerTick = WavConstants.FPS / (tempo * 0.4f); // tempo / 60 * 24
		if (!internal) {
			oscData.clear();
			oscData.add(Math.round(tempo));
			send("/tempo");
		}
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

	@Override
	public void close() throws IOException {
		stop();
		if (osc != null && osc.isConnected()) {
			try {
				osc.disconnect();
			} catch (IOException e) {
				RTLogger.warn(this, e);
			}
		}
	}

	// External Midi interface
	@Override
	public void start() {
		if (internal)
			return;
		oscData.clear();
		send("/start");
	}

	@Override
	public void stop() {
		if (internal)
			return;
		oscData.clear();
		send("/stop");
	}

	@Override
	public void cont() {
		if (internal)
			return;
		oscData.clear();
		send("/continue");
	}

	private void send(String address) {
		if (osc == null)
			return; // error state
		try {
			if (!osc.isConnected())
				osc.connect();
			osc.send(new OSCMessage(address, oscData));
		} catch (Exception e) {
			RTLogger.warn(this, "tempo " + e.getMessage());
		}
	}

	private void newBar() {
		bar++; // new Bar
		beat = 0;
		announce(Property.BARS, bar);
		if (bar % length == 0 && !debounce.doubleTap())
			announce(Property.BOUNDARY, bar / length);
	}

	// Looper Integration
	public void loopCount(int count) {
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
	public void syncToLoop(Loop target) {
		if (target == null) return;
		onDeck = !onDeck;
		if (!onDeck)
			return;

		Looper looper = getLooper();
		if (!looper.hasRecording())
			return;
		long millis = (long)(looper.getPrimary().seconds() * 1000);
		setTempo(Constants.computeTempo(millis, length * getMeasure()));
		// looper.clockSync(); // off FREE

		// Reset Chord Track
		ChordTrack chords = getChords();
		if (chords.isActive() && chords.getSection() != null)
			chords.click(chords.getSection().get(0));
	}

	public void offSync() {
		onDeck = false;
		MainFrame.update(this);
	}

	/** Notification in background thread */
	private void passItOn(Property prop, Object value) {
		notifications.offer(new Notification(prop, value));
	}

	/**Notification in real-time */
	private void announce(Property prop, Object value) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).update(prop, value);
	}

	// CMD interface
	@Override public String[] getKeys() {
		return 	IntProvider.instance(40, 200, 2).getKeys();
	}

	@Override public Integer resolve(String key) {
		return Integer.parseInt(key);
	}

	@Override public void execute(Param p) {
		if (p.cmd == Cmd.Tempo)
			try { setTempo(Integer.parseInt(p.val));
			} catch (NumberFormatException e) {RTLogger.warn(this, "tempo: " + p.val);}
	}


}
