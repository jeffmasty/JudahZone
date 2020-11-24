package net.judah.looper;


import static net.judah.jack.AudioMode.*;
import static net.judah.mixer.MixerPort.ChannelType.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.jack.AudioMode;
import net.judah.jack.AudioTools;
import net.judah.jack.RecordAudio;
import net.judah.midi.MidiClient;
import net.judah.mixer.MixerPort;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
public class Recorder extends Sample implements RecordAudio {

	protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
	@Getter protected final List<MixerPort> inputPorts = new ArrayList<>();
	
	private final Memory memory;
	private float[][] newBuffer;
	private FloatBuffer fromJack;
	private boolean firstLeft, firstRight;
	private int counter;
	
	public Recorder(String name, Type type) {
		this(name, type, JudahZone.getInputPorts(), JudahZone.getOutputPorts());
	}
	
	public Recorder(String name, Type type, List<MixerPort> inputPorts, List<MixerPort> outputPorts) {
		this.name = name;
		this.type = type;
		for (MixerPort p : inputPorts)
			this.inputPorts.add(new MixerPort(p));
		
		
		this.outputPorts = outputPorts;
		memory = new Memory(Constants.STEREO, MidiClient.getInstance().getBuffersize());
		isPlaying.set(NEW);
	}

	@Override
	public void record(boolean active) {

		AudioMode mode = isRecording.get();
		log.warn((active ? "Activate recording from " : "Inactivate recording from ") + mode);
		
		if (active && (recording == null || mode == NEW)) {
			recording = new Recording(true); // threaded to accept live stream
			isRecording.set(STARTING);
			Console.addText(name + " recording starting");
		} else if (active && (isPlaying.get() == RUNNING || isPlaying.get() == STARTING)) {
			isRecording.set(STARTING);
			Console.addText(name + " overdub starting");
		} else if (active && (recording != null || mode == STOPPED)) {
			isRecording.set(STARTING);
			Console.addText("silently overdubbing on " + name);
		}
		
			
		if (mode == RUNNING && !active) {
			isRecording.set(STOPPING);
			// recording = new Recording(liveRecording);
			length = recording.size();
			isPlaying.compareAndSet(NEW, STOPPED);
			isPlaying.compareAndSet(ARMED, STARTING);
			isRecording.set(STOPPED);
			Console.addText(name + " recording stopped, tape is " + recording.size() + " buffers long");
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
		recording = null;
	}

	@Override
	public String toString() {
		return "Loop " + name;
	}

	@Override
	public void setRecording(Recording sample) {
		super.setRecording(sample);
		isRecording.set(STOPPED);
		sample.startListeners();
	}

	public void mute(String channel, boolean mute) {
		for (MixerPort p : inputPorts) 
			if (p.getName().contains(channel)) {
				p.setOnLoop(!mute);
				
				log.info( (mute ? "Muted " : "Unmuted ") + p.getName() + " on recording track: " + getName());
			}
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
	
	////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

	@Override
	public void process(int nframes) {
		counter = tapeCounter.get();
		recordedBuffer = null;
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
		if (hasRecording()) {
			if (recordedBuffer == null) {
				assert !playing();
				readRecordedBuffer();
			}
			recording.dub(newBuffer, recordedBuffer, counter);
		}
		else 
			recording.add(newBuffer);
	}
	

}

