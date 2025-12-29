package net.judah.looper;

import static net.judah.util.Constants.LEFT;
import static net.judah.util.Constants.RIGHT;
import static net.judah.util.Constants.STEREO;
import static net.judah.util.WavConstants.WAV_EXT;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import net.judah.api.RecordAudio;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.LoopMix;
import net.judah.seq.track.DrumTrack;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.FromDisk;
import net.judah.util.Memory;
import net.judah.util.RTLogger;
import net.judah.util.Recording;
import net.judah.util.WavFile;


public class Loop extends Channel implements RecordAudio, Runnable {
	public static final int INIT = 4096; // nice chunk of preloaded blank tape
	private static final float STD_BOOST = 2.25f;
	private static final float DRUM_BOOST = 0.85f;

	protected final Looper looper;
	protected final JudahClock clock;
    protected final Collection<LineIn> sources;

	@Getter private Recording tape = new Recording(); // RMS Recording?
    @Getter protected final LoopMix display;
	@Getter private final AtomicInteger tapeCounter = new AtomicInteger(0);
	/** current frame */
	private float[][] playBuffer;
    private int current;
    @Getter private boolean isRecording;
    @Getter private boolean dirty = false;
    /** used frames of AudioTrack's tape vector */
    @Getter private int length;
    @Getter private float factor = 1f;
    @Getter private boolean timer;
	@Getter private int stopwatch;
	private int boundary;
	private boolean leadIn;
	@Getter protected boolean drumTrack; // loopD not muted on verse/chorus changes // TODO setter/feedback

    private final BlockingQueue<float[][]> newQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<float[][]> oldQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<Integer> locationQueue = new LinkedBlockingQueue<>();

    public Loop(String name, String icon, Looper loops, Collection<LineIn> sources) {
		super(name, true);
    	this.icon = Icons.get(icon);
    	this.looper = loops;
    	this.clock = looper.getClock();
    	this.sources = sources;
    	for (int i = 0; i < INIT; i++)
    		tape.add(new float[STEREO][N_FRAMES]);
    	display = new LoopMix(this, looper);
    	new Thread(this).start(); // overdub listening
    }

    public boolean isPlaying() {
    	return dirty && length > 0; }

	public void setRecording(Recording sample) {
    	isRecording = false;
		rewind();
		tape = sample;
    	MainFrame.update(display);
	}

	void setLength(int frames) {
		length = frames;
	}

	public final void rewind() {
		tapeCounter.set(0);
	}

	public final float seconds() {
		return length / Constants.fps();
	}

	@Override public void delete() {
        if (dirty)
        	tape.silence(length);
        length = current = stopwatch = 0;
        factor = 1f;
		isRecording = timer = onMute = dirty = timer = false;
		rewind();
		AudioTools.silence(left);
        AudioTools.silence(right);
        display.clear();
	}

	public void save() {
		if (length == 0) {
			RTLogger.log(this, "no recording");
			return;
		}
		File f = Folders.choose(Folders.getLoops());
		if (f == null)
			return;
		if (!f.getName().endsWith(WAV_EXT))
			f = new File(f.getAbsolutePath() + WAV_EXT);
		try {
			WavFile.save(tape.truncate(length), f);
		} catch (Throwable t) { RTLogger.warn(this, t); }
	}

    public void load(File dotWav, boolean primary) {
		try {

		    if (!FromDisk.canLoadRecording(dotWav)) {
		        RTLogger.warn(this, "O-o-M :( " + dotWav.getName());
		        return;
		    }
			rewind();
			length = 0;
			tape = Recording.load(dotWav); // TODO buffering
			length = tape.size();
			dirty = true;
			if (primary) {
				rewind();
				looper.setPrimary(this);
			}
			MainFrame.update(display);
		} catch (Exception e) { RTLogger.warn(name, e);}
    }
    public void load(String name, boolean primary) {
    	if (name.endsWith(".wav") == false)
    		name += ".wav";
    	File file = new File(Folders.getLoops(), name);
    	if (file.exists())
    		load(file, primary);
    	else
    		RTLogger.log(name, "Not a file: " + file.getAbsolutePath());
    }
	public void load(boolean primary) {
		File file = Folders.choose(Folders.getLoops());
		if (file == null) return;
		load(file, primary);
	}

    /** primary loop/clock boundary event, compensate for duplications then hard sync to start*/
    void boundary() {
    	if (!dirty) /*&& type == Type.FREE)*/
    		return;
    	boundary ++;
    	if (factor / boundary != 1)
    		return;
		boundary = 0;

		if (isRecording && timer && looper.getType() == LoopType.FREE)
			looper.endCapture(this);

    	if (tapeCounter.get() != 0) {  // governor triggered
    		rewind();
    		MainFrame.update(display);
    	}

    }

	public void duplicate() {

		if (length == 0) { // create blank tape that is twice primary's length
			length = looper.getLength() * 2;
			if (length == 0) return; // no primary
			factor *= 2;
			RTLogger.log(name, "Doubled Tape: " + length);
			return;
		}

		length *= 2;
		factor *= 2;
		if (this == looper.getPrimary()) {
			looper.setRecordedLength(seconds());
		}
		if (length >= tape.size())
			Memory.STEREO.catchUp(tape,  length);
		int half = length / 2;
		tape.duplicate(half);
		RTLogger.log(name, "Doubled recorded frames: " + length);
		MainFrame.update(display);
	}

	public void verseChorus() {
		if (!drumTrack)
			setOnMute(!isOnMute());
	}


    ////////////////////////////////////////
    // Controller: Capture/Sync/Countdown //
    ////////////////////////////////////////

	void timerOn() {
		// set countdown for downbeat capture
		timer = true;
		MainFrame.update(display);
	}

	void timerOff() {
		timer = false;
		MainFrame.update(display);
	}

	int count() {
		stopwatch++;
		display.measureFeedback();
		return stopwatch;
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
    	if (!dirty) {
    		if (looper.hasRecording())
    			tapeCounter.set(looper.conform(this));
    		else
    			rewind();
    		dirty = true;
    	}
		if (timer) {
			boundary = 0;
			stopwatch = 0;
		}
		isRecording = leadIn = true;
    }

	private void endCapture() {
		isRecording = false;
		display.clear();
		if (length <= 0) { // initial recording
			length = tapeCounter.get();
			rewind();
		}
		timerOff();
	}

    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////

    /** keep loops in time while mains muted */
	public void silently() {
		if (!dirty)
			return;
		int frame = tapeCounter.getAndIncrement();
		if (frame >= length) {
			tapeCounter.set(0);
			if (this == looper.getPrimary())
				looper.increment();
		}
		if (frame == 0 && this == looper.getPrimary())
			looper.increment();
		if (frame >= length)
			tapeCounter.set(0);
    }

	@Override
	public void processImpl() {
		// no-op
	}

	public void process(FloatBuffer left, FloatBuffer right) {
		if (!dirty && length == 0)
			return;
		current = tapeCounter.getAndIncrement();
		if (isPlaying())
			playFrame(left, right);
		if (isRecording)
			recordFrame();
    }

	/** checks val against length, side effect if over && primary: notifies looper */
	private boolean overflow(int val) {
		if (val < length)
			return false;
		tapeCounter.set(0);
		if (looper.isPrimary(this))
			looper.increment();
		return true;
	}

	private void playFrame(FloatBuffer left, FloatBuffer right) {
		if (overflow(current))
			current = 0;

		if (current >= tape.size()) {
			RTLogger.log(name, "play error " + tape.size() + " vs. " + current + " vs. " + length);
			current = 0;
			tapeCounter.set(current);
		}
		playBuffer = tape.get(current);
		if (!onMute)
			fx(left, right);
	}

	/** run active effects on the current frame being played */
	private void fx(FloatBuffer outLeft, FloatBuffer outRight) {
		AudioTools.replace(playBuffer[LEFT], left, gain.getLeft());
		AudioTools.replace(playBuffer[RIGHT], right, gain.getRight());
		active.forEach(fx -> fx.process(left, right));
		AudioTools.mix(left, gain.getGain(), outLeft);
		AudioTools.mix(right, gain.getGain(), outRight);
	}

	private void recordFrame() {
		// merge live recording sources into newBuffer
		float[][] newBuffer = current < tape.size() ? tape.get(current) : Memory.STEREO.getFrame();

		float amp = STD_BOOST;
    	if (leadIn) { // scratchy if sound blasted into first frame (rough windowing)
    		amp *= 0.33f;
    		leadIn = false;
    	}

    	SoloTrack d = looper.getSoloTrack();
    	LineIn solo = d.isSolo() ? d.getSoloTrack() : null;

    	for (LineIn in : sources) {
        	if (in.isOnMute() || in.isMuteRecord()) continue;
            if (in == solo && this != d) continue;
            if (solo != null && in != solo && this == d) continue;

            if (in instanceof Instrument)
            	recordCh(in.getLeft(), in.getRight(), newBuffer, amp);
            else if (in instanceof TacoTruck synth) {
            	if (!synth.isMuteRecord() && !synth.isOnMute())
            		recordCh(synth.getLeft(), synth.getRight(), newBuffer, amp * synth.getGain().getPreamp());
            }
            else if (in instanceof DrumMachine drumz)
            	for (DrumTrack track : drumz.getTracks())
            		if (!track.getKit().isMuteRecord())
            			recordCh(track.getKit().getLeft(), track.getKit().getRight(),
            				newBuffer, DRUM_BOOST);
        }

    	if (current < tape.size()) {
    		if (tape.get(current) != newBuffer) {
				// off-thread overdub
				oldQueue.add(playBuffer);
				newQueue.add(newBuffer);
				locationQueue.add(current);
    		}
    	}
    	else {
        	tape.add(newBuffer);
        	looper.catchUp(current);
    	}
	}

    private void recordCh(FloatBuffer sourceLeft, FloatBuffer sourceRight, float[][] target, float amp) {
    	AudioTools.add(amp, sourceLeft, target[LEFT]);
    	AudioTools.add(amp, sourceRight, target[RIGHT]);
    }

    /** overdub */
	@Override public void run() {
		try {
			do {
				int idx = locationQueue.take();
				tape.set(idx, AudioTools.overdub(newQueue.take(), oldQueue.take()));
			} while (true);
		}
		catch (Exception e) { RTLogger.warn(name, e);}
	}

}
