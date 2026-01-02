package net.judah.looper;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;
import static judahzone.util.Constants.STEREO;
import static judahzone.util.WavConstants.WAV_EXT;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import judahzone.api.RecordAudio;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import judahzone.util.Circular;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.FromDisk;
import judahzone.util.MP3;
import judahzone.util.Memory;
import judahzone.util.RTLogger;
import judahzone.util.Recording;
import judahzone.util.WavFile;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.LoopMix;
import net.judah.seq.track.DrumTrack;
import net.judah.synth.taco.TacoTruck;


public class Loop extends Channel implements RecordAudio, Runnable {
	public static final int INIT = 4096; // nice chunk of preloaded blank tape
	private static final float STD_BOOST = 2.25f;
	private static final float DRUM_BOOST = 0.85f;

	@Getter protected final Looper looper;
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

    // Pool of temporary frames used by the audio thread when creating new overdub buffers.
    // Prefilled via factory; access is synchronized because both audio thread (producer)
    // and overdub consumer (background) will touch it.
    private static final int OVERDUB_QUEUE_CAPACITY = 64;
    private final Circular<float[][]> overdubPool = new Circular<>(OVERDUB_QUEUE_CAPACITY,
            () -> new float[STEREO][N_FRAMES]);
    private final BlockingQueue<OverdubTask> overdubQueue = new LinkedBlockingQueue<>();
    // concurrency helpers
    private final AtomicInteger overdubDropped = new AtomicInteger(0);
    private final Object tapeLock = new Object();
    private volatile boolean deleted = false;

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

    // Simple immutable task bundling index + old/new buffers
    private static final class OverdubTask {
        final int index;
        final float[][] oldBuf;
        final float[][] newBuf;
        OverdubTask(int index, float[][] oldBuf, float[][] newBuf) {
            this.index = index;
            this.oldBuf = oldBuf;
            this.newBuf = newBuf;
        }
    }

    public boolean isPlaying() { return dirty && length > 0; }

	public void setRecording(Recording sample) {
    	isRecording = false;
		rewind();
		synchronized (tapeLock) { tape = sample; }
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
        // stop any in-progress recording immediately and mark deleted
        isRecording = false;
        deleted = true;

        // drop pending overdub tasks so they won't reference old buffers after we truncate/reset the tape.
        overdubQueue.clear();

        // Mutate the tape under tapeLock
        synchronized (tapeLock) {
            if (dirty) {
                if (length > 0) {
                    // committed recording: silence the used frames as before
                    tape.silence(length);
                } else {
                    // initial/unfinished recording: discard any appended frames and reset
                    Recording fresh = new Recording();
                    for (int i = 0; i < INIT; i++) {
                        fresh.add(new float[STEREO][N_FRAMES]);
                    }
                    tape = fresh;
                }
            }
        }

        // reset loop state
        length = current = stopwatch = 0;
        factor = 1f;
		isRecording = timer = onMute = dirty = false;
		rewind();
		AudioTools.silence(left);
        AudioTools.silence(right);
        display.clear();

        // reset deleted flag so loop can be reused safely if desired
        deleted = false;
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
			tape = MP3.load(dotWav, 0.25f); // TODO LINE_LEVEL // TODO buffering
			length = tape.size();
			dirty = true;
			if (primary)
				looper.setPrimary(this);

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

	@Override
	public void process(float[] left, float[] right) {
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

	private void playFrame(float[] left, float[] right) {
		if (overflow(current))
			current = 0;

		synchronized (tapeLock) {
			if (current >= tape.size()) {
				RTLogger.log(name, "play error " + tape.size() + " vs. " + current + " vs. " + length);
				current = 0;
				tapeCounter.set(current);
			}
			playBuffer = tape.get(current);
		}
		if (!onMute)
			fx(left, right);
	}

	/** run active effects on the current frame being played */
	private void fx(float[] outLeft, float[] outRight) {
		AudioTools.replace(playBuffer[LEFT], left, gain.getLeft());
		AudioTools.replace(playBuffer[RIGHT], right, gain.getRight());
		active.forEach(fx -> fx.process(left, right));
		AudioTools.mix(left, gain.getGain(), outLeft);
		AudioTools.mix(right, gain.getGain(), outRight);
	}

	private void recordFrame() {
		// Decide whether to record into existing buffer or create an off-thread overdub task
		// If the buffer currently being played (playBuffer) is the same as tape.get(current),
		// we must not mutate it in-place — create a new buffer and enqueue an overdub task.
		// Otherwise it's safe to write directly into the existing tape frame.

		// Ensure we have a stable view of current index; current is set by caller.
		if (current < tape.size()) {
			// existing frame
			float[][] existing;
			synchronized (tapeLock) { existing = tape.get(current); }

			// if existing buffer is being played, create a new buffer for overdub and enqueue task
			if (existing == playBuffer && playBuffer != null) {
				// try to acquire a temporary buffer from pool (audio thread must not block)
				float[][] newBuffer = null;
				synchronized (overdubPool) {
					try {
						newBuffer = overdubPool.get(); // Circular.get() returns oldest and advances
						AudioTools.silence(newBuffer); // likely dirty
					} catch (Exception e) {
						// pool empty or error; fall back to Memory (still avoids allocating on audio path often)
						newBuffer = Memory.STEREO.getFrame();
					}
				}

				captureInto(newBuffer);

				// bundle task and offer non-blocking
				OverdubTask task = new OverdubTask(current, existing, newBuffer);
				boolean offered = overdubQueue.offer(task);
				if (!offered) {
					overdubDropped.incrementAndGet();
					RTLogger.warn(name, "Overdub queue full, dropped overdub at idx " + current);
					// return the temp buffer to pool if possible
					synchronized (overdubPool) {
						try { overdubPool.add(newBuffer); } catch (Throwable ignored) {}
					}
				}
			} else {
				// safe to record directly into the existing frame
				synchronized (tapeLock) {
					captureInto(existing);
				}
			}
		}
		else {
			// no existing frame: allocate new frame (from pool if possible), record into it, then append
			float[][] newBuffer = Memory.STEREO.getFrame();
			captureInto(newBuffer);
        	synchronized (tapeLock) {
        		tape.add(newBuffer);
        	}
        	looper.catchUp(current);
		}
	}

	/** helper to perform the per-source adds into the provided target buffer */
	private void captureInto(float[][] target) {
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
            	recordCh(in.getLeft(), in.getRight(), target, amp);
            else if (in instanceof TacoTruck synth) {
            	if (!synth.isMuteRecord() && !synth.isOnMute())
            		recordCh(synth.getLeft(), synth.getRight(), target, amp * synth.getGain().getPreamp());
            }
            else if (in instanceof DrumMachine drumz)
            	for (DrumTrack track : drumz.getTracks())
            		if (!track.getKit().isMuteRecord())
            			recordCh(track.getKit().getLeft(), track.getKit().getRight(),
            				target, DRUM_BOOST);
        }
	}

    private void recordCh(float[] sourceLeft, float[] sourceRight, float[][] target, float amp) {
    	AudioTools.mix(sourceLeft, amp, target[LEFT]);
    	AudioTools.mix(sourceRight, amp, target[RIGHT]);
    }

    /** overdub consumer */
	@Override public void run() {
		try {
			while (true) {
				OverdubTask task = overdubQueue.take(); // blocks consumer thread

				// If loop has been deleted, skip processing and return temp buffer to pool
				if (deleted) {
					synchronized (overdubPool) { try { overdubPool.add(task.newBuf); } catch (Throwable ignored) {} }
					continue;
				}

				try {
					// Merge new additions into existing buffer (background thread)
					float[][] merged = AudioTools.overdub(task.newBuf, task.oldBuf);

					// Ensure we don't store a buffer that is also the temporary newBuf/oldBuf.
					// If merged aliases one of those, clone for tape store so we can safely return temp buffer to pool.
					final float[][] toStore;
					if (merged == task.newBuf || merged == task.oldBuf) {
						toStore = AudioTools.clone(merged);
					} else {
						toStore = merged;
					}

					// protect tape mutation in case Recording is not thread-safe
					synchronized (tapeLock) {
						if (!deleted) {
							tape.set(task.index, toStore);
						}
					}

					// return the temporary newBuf to the pool for reuse
					synchronized (overdubPool) {
						try { overdubPool.add(task.newBuf); } catch (Throwable ignored) {}
					}
				} catch (Throwable t) {
					RTLogger.warn(name, t);
					// attempt to return temp buffer if possible
					synchronized (overdubPool) { try { overdubPool.add(task.newBuf); } catch (Throwable ignored) {} }
				}
			}
		}
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) { RTLogger.warn(name, e);}
	}

	/** expose overdub drop metric for diagnostics */
	public int getOverdubDropped() {
		return overdubDropped.get();
	}

}