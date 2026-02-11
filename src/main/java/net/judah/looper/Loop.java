package net.judah.looper;

import static judahzone.util.Constants.LEFT;
import static judahzone.util.Constants.RIGHT;
import static judahzone.util.Constants.STEREO;
import static judahzone.util.WavConstants.WAV_EXT;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import judahzone.api.RecordAudio;
import judahzone.data.Circular;
import judahzone.data.Recording;
import judahzone.gui.Icons;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.MP3;
import judahzone.util.Memory;
import judahzone.util.RTLogger;
import judahzone.util.WavFile;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.drums.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.LoopMix;
import net.judah.seq.track.DrumTrack;
import net.judah.synth.taco.TacoTruck;

/** Audio loop recorder with real-time overdub, thread-safe tape mutation,
    and lock-free buffer pooling to minimize RT allocations. */
public class Loop extends Channel implements RecordAudio, Runnable {
	public static final int INIT = 4096;
	private static final float UNITY = 1f;
	private static final int OVERDUB_QUEUE_CAPACITY = 64;

	@Getter protected final Looper looper;
	protected final JudahClock clock;

	private final AtomicReference<Recording> tape = new AtomicReference<>(new Recording());
	@Getter protected final LoopMix display;
	@Getter private final AtomicInteger tapeCounter = new AtomicInteger(0);

	private float[][] playBuffer;
	private int current;

	@Getter private boolean isRecording;
	@Getter private boolean dirty = false;
	@Getter private int length;
	@Getter private float factor = 1f;
	@Getter private boolean timer;
	@Getter private int stopwatch;
	private int boundary;
	private boolean leadIn;
	@Getter protected boolean drumTrack;

	private final Circular<float[][]> overdubPool = new Circular<>( // blank tape for recording
			OVERDUB_QUEUE_CAPACITY, () -> new float[STEREO][N_FRAMES]);
	private final BlockingQueue<RecordTask> recordQueue = new LinkedBlockingQueue<>();
	private final AtomicInteger overdubDropped = new AtomicInteger(0);
	private volatile boolean deleted = false;

	public Loop(String name, String icon, Looper loops, JudahClock clock) {
		super(name, true);
		setOnMixer(true);
		this.icon = Icons.get(icon);
		this.looper = loops;
		this.clock = clock;
		Recording initial = new Recording();
		for (int i = 0; i < INIT; i++)
			initial.add(new float[STEREO][N_FRAMES]);
		tape.set(initial);
		display = new LoopMix(this, looper);
		new Thread(this).start();
	}

	/** Unified recording task: overdub merge, direct write, or tape.add(). */
	private abstract class RecordTask {
		abstract void execute(Recording tape);
	}

	// Background Consumer Thread Handles All Mutations
	private final class OverdubTask extends RecordTask {
		final int index;
		final float[][] oldBuf;
		final float[][] newBuf;
		OverdubTask(int index, float[][] oldBuf, float[][] newBuf) {
			this.index = index;
			this.oldBuf = oldBuf;
			this.newBuf = newBuf;
		}
		@Override void execute(Recording tape) {
			if (deleted) return;
			float[][] merged = AudioTools.overdub(newBuf, oldBuf);
			final float[][] toStore;
			if (merged == newBuf || merged == oldBuf)
				toStore = AudioTools.clone(merged);
			else
				toStore = merged;
			if (index < tape.size())
				tape.set(index, toStore);
		}
	}

	private final class DirectWriteTask extends RecordTask {
		final int index;
		final float[][] buffer;
		DirectWriteTask(int index, float[][] buffer) {
			this.index = index;
			this.buffer = buffer;
		}
		@Override void execute(Recording tape) {
			if (!deleted && index < tape.size())
				tape.set(index, buffer);
		}
	}

	private final class AppendTask extends RecordTask {
		final float[][] buffer;
		AppendTask(float[][] buffer) {
			this.buffer = buffer;
		}
		@Override void execute(Recording tape) {
			if (!deleted)
				tape.add(buffer);
		}
	}

	public boolean isPlaying() { return dirty && length > 0; }

	public Recording getTape() { return tape.get(); }

	public void setRecording(Recording sample) {
		isRecording = false;
		rewind();
		tape.set(sample);
		MainFrame.update(display);
	}

	void setLength(int frames) { length = frames; }

	public final void rewind() { tapeCounter.set(0); }

	public final float seconds() { return length / Constants.fps(); }

	@Override public void delete() {
		isRecording = false;
		deleted = true;
		recordQueue.clear();

		Recording currentTape = tape.get();
		if (dirty) {
			if (length > 0) {
				currentTape.silence(length);
			} else {
				Recording fresh = new Recording();
				for (int i = 0; i < INIT; i++)
					fresh.add(new float[STEREO][N_FRAMES]);
				tape.set(fresh);
				currentTape = fresh;
			}
		}

		length = stopwatch = 0;
		factor = 1f;
		isRecording = timer = onMute = dirty = false;
		rewind();
		AudioTools.silence(left);
		AudioTools.silence(right);
		computeRMS(left, right);
		display.clear();
		deleted = false;
	}

	public void save() {
		if (length == 0) {
			RTLogger.log(this, "no recording");
			return;
		}
		File f = Folders.choose(Folders.getLoops());
		if (f == null) return;
		if (!f.getName().endsWith(WAV_EXT))
			f = new File(f.getAbsolutePath() + WAV_EXT);
		try {
			WavFile.save(tape.get().truncate(length), f);
		} catch (Throwable t) { RTLogger.warn(this, t); }
	}

	public void load(File dotWav, boolean primary) {
		try {
			if (!Memory.check(dotWav)) {
				RTLogger.warn(this, "O-o-M :( " + dotWav.getName());
				return;
			}
			rewind();
			length = 0;
			Recording loaded = MP3.load(dotWav);
			tape.set(loaded);
			length = loaded.size();
			dirty = true;
			if (primary) looper.setPrimary(this);
			MainFrame.update(display);
		} catch (Exception e) { RTLogger.warn(name, e); }
	}

	public void load(String name, boolean primary) {
		if (!name.endsWith(".wav")) name += ".wav";
		File file = new File(Folders.getLoops(), name);
		if (file.exists()) load(file, primary);
		else RTLogger.log(name, "Not a file: " + file.getAbsolutePath());
	}

	public void load(boolean primary) {
		File file = Folders.choose(Folders.getLoops());
		if (file != null) load(file, primary);
	}

	/** Sync loop to primary's boundary; hard-sync playhead to start if factor
	    aligns and optionally end recording if FREE type. */
	void boundary() {
		if (!dirty) return;
		boundary++;
		if (factor / boundary != 1) return;
		boundary = 0;

		if (isRecording && timer && looper.getType() == LoopType.FREE)
			looper.endCapture(this);

		if (tapeCounter.get() != 0) {
			rewind();
			MainFrame.update(display);
		}
	}

	/** Double loop length; create blank tape if needed or duplicate recorded frames. */
	public void duplicate() {
		Recording current = tape.get();
		if (length == 0) {
			length = looper.getLength() * 2;
			if (length == 0) return;
			factor *= 2;
			RTLogger.log(name, "Doubled Tape: " + length);
			return;
		}

		length *= 2;
		factor *= 2;
		if (this == looper.getPrimary())
			looper.setRecordedLength(seconds());
		if (length >= current.size())
			Memory.STEREO.catchUp(current, length);
		int half = length / 2;
		current.duplicate(half);
		RTLogger.log(name, "Doubled recorded frames: " + length);
		MainFrame.update(display);
	}

	/** Mute/unmute loop on verse/chorus if not drum track. */
	public void verseChorus() {
		if (!drumTrack) setOnMute(!isOnMute());
	}

	void timerOn() {
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

	@Override public void capture(boolean active) {
		if (active && !isRecording) startCapture();
		else if (!active && isRecording) endCapture();
		else return;
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
		if (length <= 0) {
			length = tapeCounter.get();
			rewind();
			display.update();
		}
		timerOff();
	}

	/** Advance playhead while loop is muted (keeping primary in sync). */
	public void silently() {
		if (!dirty) return;
		int frame = tapeCounter.getAndIncrement();
		if (frame >= length) {
			tapeCounter.set(0);
			if (this == looper.getPrimary()) looper.increment();
		}
		if (frame == 0 && this == looper.getPrimary()) looper.increment();
	}

	@Override public void processImpl() { }

	public void processLoop(float[] left, float[] right) {
		if (!dirty && length == 0) return;
		current = tapeCounter.getAndIncrement();
		if (isPlaying())
			playFrame(left, right);
		if (isRecording)
			recordFrame();
	}

	private boolean overflow(int val) {
		if (val < length) return false;
		tapeCounter.set(0);
		if (looper.isPrimary(this)) looper.increment();
		return true;
	}

	private void playFrame(float[] outLeft, float[] outRight) {
		if (overflow(current)) current = 0;
		if (onMute) return;

		Recording currentTape = tape.get();
		playBuffer = currentTape.get(current);
		AudioTools.copy(playBuffer[LEFT], left);
		AudioTools.copy(playBuffer[RIGHT], right);

		getGain().process(left, right);
		fx(left, right);
		computeRMS(left, right);

		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

	private void fx(float[] outLeft, float[] outRight) {
		hotSwap();
		for (int i = 0; i < active.size(); i++)
			active.get(i).process(left, right);
	}

	private void recordFrame() {
		Recording currentTape = tape.get();

		if (current < currentTape.size()) {
			// Existing frame: check if it's being played
			float[][] existing = currentTape.get(current);

			if (existing == playBuffer && playBuffer != null) {
				// Collision: create temp buffer and enqueue overdub task
				float[][] newBuffer = overdubPool.get();
				AudioTools.silence(newBuffer);

				captureInto(newBuffer);
				RecordTask task = new OverdubTask(current, existing, newBuffer);
				if (!recordQueue.offer(task)) {
					overdubDropped.incrementAndGet();
					RTLogger.warn(name, "Record queue full at idx " + current);
					synchronized (overdubPool) {
						try { overdubPool.add(newBuffer); } catch (Throwable ignored) { }
					}
				}
			}
			else {
				// Safe: no collision, enqueue direct write (no lock on RT path)
				float[][] newBuffer = new float[STEREO][N_FRAMES];
				AudioTools.silence(newBuffer);
				captureInto(newBuffer);

				RecordTask task = new DirectWriteTask(current, newBuffer);
				if (!recordQueue.offer(task)) {
					overdubDropped.incrementAndGet();
					RTLogger.warn(name, "Record queue full at idx " + current);
				}
			}
		} else {
			// New frame: allocate, record, enqueue append
			float[][] newBuffer = Memory.STEREO.getFrame();
			captureInto(newBuffer);

			RecordTask task = new AppendTask(newBuffer);
			if (!recordQueue.offer(task)) {
				overdubDropped.incrementAndGet();
				RTLogger.warn(name, "Record queue full, append dropped");
			} else {
				looper.catchUp(current);
			}
		}
	}

	private void captureInto(float[][] target) {
		float amp = UNITY;
		if (leadIn) {
			amp *= 0.33f;
			leadIn = false;
		}

		SoloTrack d = looper.getSoloTrack();
		LineIn solo = d.isSolo() ? d.getSoloTrack() : null;
		List<LineIn> processing = looper.candidates;

		for (int i = 0; i < processing.size(); i++) {
			LineIn in = processing.get(i);
			if (in.isOnMute() || in.isMuteRecord()) continue;
			if (in == solo && this != d) continue;
			if (solo != null && in != solo && this == d) continue;

			if (in instanceof Instrument)
				recordCh(in.getLeft(), in.getRight(), target, amp);
			else if (in instanceof TacoTruck synth) {
				if (!synth.isMuteRecord() && !synth.isOnMute())
					recordCh(synth.getLeft(), synth.getRight(), target, amp);
			} else if (in instanceof DrumMachine drumz) {
				for (DrumTrack track : drumz.getTracks())
					if (!track.getKit().isMuteRecord())
						recordCh(track.getKit().getLeft(), track.getKit().getRight(),
							target, UNITY);
			}
		}
	}

	private void recordCh(float[] sourceLeft, float[] sourceRight, float[][] target, float amp) {
		AudioTools.mix(sourceLeft, amp, target[LEFT]);
		AudioTools.mix(sourceRight, amp, target[RIGHT]);
	}

	/** Record consumer thread: process all tape mutations (overdub, direct write, append). */
	@Override public void run() {
		try {
			while (true) {
				RecordTask task = recordQueue.take();

				if (deleted) {
					// If it's an OverdubTask with a temp buffer, return it to pool
					if (task instanceof OverdubTask ot) {
						synchronized (overdubPool) {
							try { overdubPool.add(ot.newBuf); } catch (Throwable ignored) { }
						}
					}
					continue;
				}

				try {
					Recording currentTape = tape.get();
					task.execute(currentTape);

					// Return temp buffer to pool if applicable
					if (task instanceof OverdubTask ot) {
						synchronized (overdubPool) {
							try { overdubPool.add(ot.newBuf); } catch (Throwable ignored) { }
						}
					}
				} catch (Throwable t) {
					RTLogger.warn(name, t);
					if (task instanceof OverdubTask ot) {
						synchronized (overdubPool) {
							try { overdubPool.add(ot.newBuf); } catch (Throwable ignored) { }
						}
					}
				}
			}
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		} catch (Exception e) { RTLogger.warn(name, e); }
	}

	public int getOverdubDropped() { return overdubDropped.get(); }
}
