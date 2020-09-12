package net.judah.looper;


import static net.judah.jack.AudioMode.*;
import static net.judah.mixer.MixerPort.ChannelType.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.jack.AudioMode;
import net.judah.jack.AudioTools;
import net.judah.jack.RecordAudio;
import net.judah.midi.MidiClient;
import net.judah.mixer.MixerPort;
import net.judah.util.Constants;

@Log4j
// TODO Loop substitute
public class Recorder extends Sample implements RecordAudio {

	@Getter protected Recording liveRecording;
	protected final transient AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
	@Setter protected transient List<MixerPort> inputPorts;
	
	private final transient Memory memory;
	private transient float[][] newBuffer;
	private transient FloatBuffer fromJack;
	private boolean firstLeft, firstRight;
	
	public Recorder(String name, Type type) {
		this.name = name;
		this.type = type;
		memory = new Memory(Constants.STEREO, MidiClient.getInstance().getBuffersize());
	}

	@Override
	public void record(boolean active) {

		AudioMode mode = isRecording.get();
		log.warn((active ? "Activate recording from " : "Inactivate recording from ") + mode);
		
		if (active && (liveRecording == null || mode == NEW)) {
			liveRecording = new LiveRecording(); // threaded to accept live stream
			isRecording.set(STARTING);
			log.warn(name + " recording starting");
		} else if (active && isPlaying.get() == RUNNING) {
			isRecording.set(STARTING);
			log.warn(name + " overdub starting");
		}
			
		if (mode == RUNNING && !active) {
			isRecording.set(STOPPING);
			recording = new Recording(liveRecording);
			isPlaying.compareAndSet(NEW, STOPPED);
			isPlaying.compareAndSet(ARMED, STARTING);
			isRecording.set(STOPPED);
			log.warn(name + " recording stopped, tape is " + recording.size() + " buffers long");
		}
	}

	@Override
	public AudioMode isRecording() {
		return isRecording.get();
	}

	@Override
	public void clear() {
		isRecording.compareAndSet(RUNNING, STOPPING);
		super.clear();
		isRecording.set(NEW);
		liveRecording = null;
	}

	/** for process() thread */
	private final boolean recording() {
		isRecording.compareAndSet(STOPPING, STOPPED);
		isRecording.compareAndSet(STARTING, RUNNING);
		if (isRecording.get() == RUNNING)
			for (MixerPort p : inputPorts)
				if (p.isOnLoop())
					return true;
		return false;
	}
	
	@Override
	public void process(int nframes) {
		super.process(nframes);
		if (!recording()) return;
		
		newBuffer = memory.getArray();
		firstLeft = true;
		firstRight = true;
		for (MixerPort p : inputPorts) {
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
	}
}

