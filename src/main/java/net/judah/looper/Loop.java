package net.judah.looper;

import static net.judah.Constants.*;
import static net.judah.looper.Loop.Mode.*;
import static net.judah.mixer.MixerPort.Type.*;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.Constants;
import net.judah.RTLogger;
import net.judah.jack.AudioTools;
import net.judah.mixer.MixerPort;

@Log4j
public class Loop implements LoopInterface {
    public static enum Mode {NEW, ARMED, STARTING, RUNNING, STOPPING, STOPPED};
    
	private final String name;

	private final int bufSize;
	private final Memory memory;
	private final List<MixerPort> inPorts;
	private final List<MixerPort> outPorts;
 
	protected Recording live, tape; // undo 

	@Getter private AtomicInteger tapeCounter = new AtomicInteger();
	@Getter protected AtomicReference<Mode> recording = new AtomicReference<>(NEW);
	@Getter protected AtomicReference<Mode> playing = new AtomicReference<>(NEW);
	
	// for tape position counter
	private int udpated;
	
	// for process()
	private HashSet<Integer> channelsNeeded = new HashSet<>();
	private float[][] newBuffer, recordedBuffer;
	private FloatBuffer fromJack;
	private FloatBuffer toJackLeft, toJackRight;
	private final float[] workArea;
	private boolean firstLeft, firstRight;
	private int z;

	public Loop(String name, JackClient client, List<MixerPort> inputPorts, List<MixerPort> outputPorts) throws JackException {
		this.name = name;
		this.bufSize = client.getBufferSize();
		this.inPorts = inputPorts;
		this.outPorts = outputPorts;
		for (MixerPort p : inputPorts) {
			if (p.isStereo() == false) 
				throw new JackException(name + " only handles stereo channels. " + Arrays.toString(inputPorts.toArray()));
		}
		if (outPorts.size() != 2) 
			throw new JackException(name + " only handles stereo loops.  " + Arrays.toString(outPorts.toArray()));
		memory = new Memory(Constants.STEREO, bufSize);
		workArea = new float[bufSize];
	}
	
	private int loopCount = 0;
	protected float[][] getCurrent() {
		return live.get(tapeCounter.get());
	}
	
	protected void updateCounter() {
		udpated = tapeCounter.get() + 1;
		if (udpated == live.size()) {
			RTLogger.log(this, name + "Loop count: " + ++loopCount);
			udpated = 0;
		}
		tapeCounter.set(udpated);
	}

	@Override
	public void record(boolean active) {

		Mode mode = recording.get();
		log.warn(name + " recording: " + active + " from: " + mode);
		
		if (active && (live == null || mode == NEW)) {
			live = new Recording();
			recording.set(STARTING);
			log.warn(name + " recording starting");
		} else if (active && playing.get() == RUNNING) {
			recording.set(STARTING);
			log.warn(name + " overdub");
		}
			
		if (mode == RUNNING && !active) {
			recording.set(STOPPING);
			tape = new Recording(live);
			playing.compareAndSet(NEW, STOPPED);
			playing.compareAndSet(ARMED, STARTING);
			recording.set(STOPPED);
			log.warn(name + " recording stopped, tape is " + tape.size() + " buffers long");
		}
	}

	@Override
	public void play(boolean active) {
		log.warn(name + " playing active: " + active);
		
		if (playing.compareAndSet(NEW, active ? ARMED : NEW)) {
			if (active) log.warn("Play is armed.");
		}
		playing.compareAndSet(ARMED, active ? ARMED : hasRecording() ? STOPPED : NEW);
		playing.compareAndSet(RUNNING, active ? RUNNING : STOPPING);
		
		if (playing.compareAndSet(STOPPED, active ? STARTING : STOPPED)) {
			if (active) 
				log.warn("playing starting. tape has " + tape.size() + " buffers.");
		}
		
	}
	private boolean hasRecording() {
		return tape != null && !tape.isEmpty();
	}

	@Override
	public void clear() {
		log.warn("clearing " + name);
		recording.compareAndSet(Mode.RUNNING, Mode.STOPPING);
		boolean wasRunning = false;
		if (playing.compareAndSet(Mode.RUNNING, Mode.STOPPING)) {
			wasRunning = true;
		}
		
		try { // get a process() in
			Thread.sleep(20);
		} catch (Exception e) {	}
		
		
		recording.set(NEW);
		playing.set(wasRunning ? ARMED : NEW);
		
		tapeCounter.set(0);
		tape = null;
		live = null;
	}

	@Override
	public void channel(int ch, boolean active) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void undo() {
		// TODO Auto-generated method stub
	}

	@Override
	public void redo() {
		// TODO Auto-generated method stub
	}



	////////////////////////////////////
	//     Process Audio
	////////////////////////////////////
	public void process(int nframes) {
		
		// output
		if (playing()) {
			toJackLeft = outPorts.get(LEFT_CHANNEL).getPort().getFloatBuffer();
			toJackLeft.rewind();
			toJackRight = outPorts.get(RIGHT_CHANNEL).getPort().getFloatBuffer();
			toJackRight.rewind();
			recordedBuffer = getCurrent();
			processMix(recordedBuffer[LEFT_CHANNEL], toJackLeft, nframes);
			processMix(recordedBuffer[RIGHT_CHANNEL], toJackRight, nframes);
		} 

		// input
		if (recording()) {
			newBuffer = memory.getArray();
			firstLeft = true;
			firstRight = true;
			for (MixerPort p : inPorts) {
				if (p.isOnLoop()) {
					fromJack = p.getPort().getFloatBuffer();
					if (p.getType() == LEFT) {
						if (firstLeft) {
							FloatBuffer.wrap(newBuffer[LEFT_CHANNEL]).put(fromJack); // processEcho
							firstLeft = false;
						} else {
							AudioTools.processAdd(fromJack, newBuffer[LEFT_CHANNEL]);
						}
					} else {
						if (firstRight) {
							FloatBuffer.wrap(newBuffer[RIGHT_CHANNEL]).put(fromJack);
							firstRight = false;
						} else {
							AudioTools.processAdd(fromJack, newBuffer[RIGHT_CHANNEL]);
						}
					}
				}
			}
			
			if (hasRecording()) 
				live.dub(newBuffer, recordedBuffer, tapeCounter.get());
			else 
				live.add(newBuffer);
			
		}
		
		if (playing.get() == RUNNING)
			updateCounter();
	}

	
	private void processMix(float[] in, FloatBuffer out, int nframes) {
		out.get(workArea);
		out.rewind();
		for (z = 0; z < nframes; z++) {
			out.put(workArea[z] + in[z]);
		}
		
	}
	
	/** for process() thread */
	private boolean playing() {
		playing.compareAndSet(STOPPING, STOPPED);
		playing.compareAndSet(STARTING, RUNNING);
		return playing.get() == RUNNING;
	}
	
	/** for process() thread */
	private boolean recording() {
		if (recording.compareAndSet(STOPPING, STOPPED)) {

		}
		recording.compareAndSet(STARTING, RUNNING);
		if (recording.get() == RUNNING)
			for (MixerPort p : inPorts)
				if (p.isOnLoop())
					return true;
		return false;
	}

	
	public void poll(HashSet<MixerPort> neededPorts) {
		if (recording.get() != RUNNING)
			return;
		for (MixerPort p : inPorts)
			if (p.isOnLoop())
				neededPorts.add(p);
	}
	
	public boolean isRecording() {
		if (recording.get() == RUNNING)
			for (MixerPort p : inPorts)
				if (p.isOnLoop())
					return true;
		return false;
	}
}
