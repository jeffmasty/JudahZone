package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;
import static net.judah.util.Constants.STEREO;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.RecordAudio;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.LoopMix;
import net.judah.mixer.Zone;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.omni.Recording;
import net.judah.omni.Threads;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.synth.JudahSynth;
import net.judah.util.Folders;
import net.judah.util.Memory;
import net.judah.util.RTLogger;

public class Loop extends AudioTrack implements RecordAudio, TimeListener, Runnable {
	public static final float STD_BOOST = 3;
	public static final float DRUM_BOOST = 0.75f;
	public static final int OFF = -1;
	protected static int INIT = 2 ^ 13; // nice chunk of preloaded blank tape

	protected final Looper looper;
	protected final JudahClock clock;
    protected final Zone sources;
    protected final Memory memory;
    /** current frame */
    protected int current;

    @Getter protected final LoopMix display;
    @Getter protected boolean isRecording;
    @Getter protected boolean drumTrack;
    /** used frames of AudioTrack's blank tape vector */
    @Getter protected int length;
	@Getter protected int measures;
	@Getter private boolean timer;
	@Getter private int stopwatch;
    private boolean leadIn;
	private final Type reset;

    private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>();

    public Loop(String name, String icon, Type init, Looper loops, Zone sources,
    		JackPort l, JackPort r, Memory mem) {
    	super(name);
    	this.icon = Icons.get(icon);
    	this.looper = loops;
    	this.clock = looper.getClock();
    	this.sources = sources;
    	this.memory = mem;
    	this.leftPort = l;
    	this.rightPort = r;
    	this.reset = this.type = init;
    	for (int i = 0; i < INIT; i++)
    		recording.add(new float[STEREO][bufSize]);
    	display = new LoopMix(this, looper);
    	new Thread(this).start(); // overdub listening
    }

	@Override public void clear() {
        if (length != 0)
        	recording.silence(length);
		type = reset;
        length = current = measures = stopwatch = 0;
		isRecording = timer = onMute = false;
		playBuffer = null;
		clock.removeListener(this);
        tapeCounter.set(0);
        AudioTools.silence(left);
        AudioTools.silence(right);
        display.clear();
	}

    @Override public void setRecording(Recording music) {
    	isRecording = false;
    	super.setRecording(music);
    	MainFrame.update(display);
    }

    @Override public boolean isPlaying() {
    	return length > 0; }

    /** ratio of this length to primary's length (per duplications) */
    public float factor() {
    	if (looper.getPrimary() == null)
    		return 1;
    	if (measures == 0)
    		return 1;
    	return measures / (float)looper.getMeasures();
    }

	@Override public void run() {
		try {
			do { // overdub
				int idx = locationQueue.take();
				recording.set(idx, AudioTools.overdub(newQueue.take(), oldQueue.take()));
			} while (true);
		}
		catch (Exception e) { RTLogger.warn(this, e);}
	}

    /** loop boundary, hard sync to start.*/
    void boundary() {

    	if (tapeCounter.get() != 0)
    		tapeCounter.set(0);

    	if (type == Type.FREE) {  // TODO factorize
    		if (isRecording && timer)
    			capture(false);
    		return;
    	}
    }

    void catchUp(final int size) {
    	for (int i = recording.size(); i < size; i++)
    		recording.add(memory.getFrame());
    }

    public void save() {
		try {
			ToDisk.toDisk(recording, Folders.choose(Folders.getLoops()), length);
		} catch (Throwable t) { RTLogger.warn(this, t); }
	}

    public void load(File f, boolean primary) {
		clear();
		try {
			length = recording.load(f, 1);
			if (primary) {
				rewind();
				looper.setPrimary(this);
			}
			MainFrame.update(display);
		} catch (Exception e) { RTLogger.warn(this, e);}
    }
    public void load(String name, boolean primary) {
    	if (name.endsWith(".wav") == false)
    		name += ".wav";
    	File file = new File(Folders.getLoops(), name);
    	if (file.exists())
    		load(file, primary);
    	else
    		RTLogger.log(this, "Not a file: " + file.getAbsolutePath());
    }
	public void load(boolean primary) {
		File file = Folders.choose(Folders.getLoops());
		if (file == null) return;
		load(file, primary);
	}

	public void duplicate() {

		if (length == 0) { // create blank tape that is twice primary's length
			length = looper.getLength() * 2;
			if (length == 0) return; // no primary

			Threads.execute(()->catchUp(length));
			RTLogger.log(toString(), "Doubled Tape: " + length);
		}
		else {
			Threads.execute(()->{ // duplicate current recording
				if (this == looper.getPrimary() && type != Type.FREE)
					JudahZone.getClock().setLength(JudahClock.getLength() * 2);

				if (length * 2 >= recording.size())
					catchUp(length * 2);
				for (int i = 0; i < length; i++)
					AudioTools.copy(recording.get(i), recording.get(i + length));
				length *= 2;
				RTLogger.log(toString(), "Audio Duplicated: " + length);
			});
		}
		measures *= 2;
		if (this == looper.getPrimary())
			looper.setRecordedLength(seconds());
	}

	@Override
	public void setType(Type type) {
		this.type = type;
		MainFrame.update(display);
	}

    /** make sure there is enough blank tape as primary*/
	public void conform(Loop primary) {
		setType(primary.getType() == Type.FREE ? Type.FREE :
					type == Type.SOLO ?  Type.SOLO : Type.SYNC);
		catchUp(primary.length);
		if (isRecording)
			capture(false);
		measures = primary.measures;
	}

    //////////////////////////////////////
    // Recording Control/Sync/Countdown //
    //////////////////////////////////////

	void syncRecord() {
		capture(true);
		timerOn();
	}

	public void queue() {
		if (timer)
			timerOff();
		else
			timerOn();
	}

	private void timerOn() {
		timer = true;
		if (type == Type.FREE)
			return;
		stopwatch = 0;
		// set countdown for downbeat capture
		clock.addListener(this);
		MainFrame.update(display);
	}

	private void timerOff() {
		timer = false;
		clock.removeListener(this);
		MainFrame.update(display);
	}

	// user didn't know how many measures to record, but now user does know.
	public void endBSync() {
		measures = stopwatch + 1;
		clock.setLength(measures);
		display.setFeedback("[" + measures + "]");
	}

	/** user engaged the record button, setup depending on loop type */
    public void trigger() {
		if (isRecording()) {
			if (type == Type.BSYNC && !looper.hasRecording())
				endBSync();
			else
				capture(false);
		} else if (looper.hasRecording())
			capture(true); // everything ready for overdub
		else if (type == Type.FREE) {
			capture(true);
			if (looper.getPrimary() == null)
				looper.checkSoloSync();
		} else  // start recording on downbeat
			queue();
	}

    /** recording start/stop */
    @Override public void capture(boolean active) {
    	if (active && !isRecording)
    		startCapture();
    	else if (!active && isRecording)
    		endCapture();
    	else
    		return;
        MainFrame.update(display);
    }

    private void startCapture() {

    	isRecording = leadIn = true;
    	Loop primary = looper.getPrimary();

    	if (primary == null) { // this will be primary
    		current = 0;
    		tapeCounter.set(current);
    		if (type == Type.SYNC)
    			measures = JudahClock.getLength();
    		else if (type == Type.BSYNC) {
    			measures = -1;
    		}
    	}
    	else if (length == 0) {

    		length = primary.getLength();
    		if (length >= recording.size()) // live larger than pre-allocated blank tape
    			catchUp(length);

    		current = primary.getTapeCounter().get();
    		if (current >= recording.size()) {
				RTLogger.warn(name, new Throwable("CatchUp error: " + current + " vs. " + recording.size()));
				current = 0;
				tapeCounter.set(current);
    		}
			else if (tapeCounter.get() != current)
				tapeCounter.set(current);
    	}
    	display.measureFeedback();
    }

	private void endCapture() {

		isRecording = false;
		timerOff();

		if (looper.getPrimary() == null) {
			// this will be primary
			length = current;
			if (type != Type.BSYNC)
				measures = JudahClock.getLength();
			tapeCounter.set(0);
			looper.setPrimary(this);
		} else if (length == 0) // ??
			length = looper.getLength();
		display.clear();
	}

	/* clock listener */
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS && type != Type.FREE)
			updateBar((int)value);
		else if (prop == Property.BEAT && type != Type.FREE && looper.getPrimary() == null && !isRecording /* onDeck */)
			display.setFeedback("- " + (clock.getMeasure() - ((int)value))); // pre-record countdown
	}

	private void updateBar(int bar) {
		if (!isRecording) { // clock updates but not recording, start on downbeat
			startCapture();
			return;
		}

		stopwatch++;
		if (type == Type.FREE)
			return;
		if (stopwatch == measures)
			endCapture();
		else
			display.measureFeedback();
	}


    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////
    /** keep loops in time while mains muted */
	public void silently() {
		int frame = tapeCounter.getAndIncrement();
		if (frame == 0 && this == looper.getPrimary())
			clock.loopCount(looper.increment());
		if (frame >= length)
			tapeCounter.set(0);
    }

	@Override
	public void process() {
		current = tapeCounter.getAndIncrement();
		if (isPlaying())
			playFrame();
		if (isRecording)
			recordFrame();
		playBuffer = null;
    }

	private void playFrame() {
		if (current >= length) {
			current = 0;
			tapeCounter.set(current);
			if (this == looper.getPrimary())
				clock.loopCount(looper.increment());
		}

		if (current >= recording.size()) {
			RTLogger.log("Loop " + name, "play error " + recording.size() + " vs. " + current + " vs. " + length);
			current = 0;
			tapeCounter.set(current);
		}
		playBuffer = recording.get(current);
		if (!onMute)
			playFrame(leftPort.getFloatBuffer(), rightPort.getFloatBuffer());
	}

	private void recordFrame() {
		// merge live recording sources into newBuffer
		float[][] newBuffer = current < recording.size() ? recording.get(current) : memory.getFrame();

		float amp = STD_BOOST;
    	if (leadIn) { // scratchy if sound blasted into first frame TODO windowing
    		amp *= 0.33f;
    		leadIn = false;
    	}

    	LineIn solo = looper.getSoloTrack().isSolo() ? looper.getSoloTrack().getSoloTrack() : null;
    	for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord())continue;
            if (in == solo && type != Type.SOLO) continue;
            if (in != solo && type == Type.SOLO) continue;

            if (in instanceof Instrument)
            	recordCh(in.getLeft(), in.getRight(), newBuffer, amp);
            else if (in instanceof JudahSynth) {
            	JudahSynth synth = (JudahSynth)in;
            	if (!synth.isMuteRecord() && synth.isActive() && !synth.isOnMute())
            		recordCh(synth.getLeft(), synth.getRight(), newBuffer, amp);
            }
            else if (in instanceof DrumMachine)
            	for (MidiTrack track : ((DrumMachine)in).getTracks())
            		recordCh(((DrumTrack)track).getKit().getLeft(), ((DrumTrack)track).getKit().getRight(),
            				newBuffer, DRUM_BOOST);
        }

    	if (current < recording.size()) {
    		if (recording.get(current) != newBuffer) {
				// off-thread overdub
				oldQueue.add(playBuffer);
				newQueue.add(newBuffer);
				locationQueue.add(current);
    		}
    	}
    	else {
        	recording.add(newBuffer);
        	looper.catchUp(this, current);
    	}
	}

    private void recordCh(FloatBuffer sourceLeft, FloatBuffer sourceRight, float[][] target, float amp) {
    	AudioTools.add(amp, sourceLeft, target[LEFT]);
    	AudioTools.add(amp, sourceRight, target[RIGHT]);
    }

}
