package net.judah.looper;
import static net.judah.jack.AudioMode.*;
import static net.judah.jack.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Status;
import net.judah.api.TimeListener.Property;
import net.judah.jack.AudioMode;
import net.judah.jack.RecordAudio;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
public class Recorder extends Sample implements RecordAudio {

	@Getter protected final List<JackPort> inputPorts = new ArrayList<>();
	protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);

	private final Memory memory;
	private float[][] newBuffer;
	private FloatBuffer fromJack;
	private boolean firstTime;
	private int counter;
	@Getter private long recordedLength = -1; 
	private long _start;
	
	public Recorder(String name, Type type) {
		this(name, type, JudahZone.getInPorts(), JudahZone.getOutPorts());
	}
	
	public Recorder(String name, Type type, List<JackPort> inputPorts, List<JackPort> outputPorts) {
		this.name = name;
		this.type = type;
		this.inputPorts.addAll(inputPorts);
		this.outputPorts.addAll(outputPorts);
		memory = new Memory(Constants.STEREO, JudahMidi.getInstance().getBufferSize());
		isPlaying.set(NEW);
	}

	@Override
	public void record(boolean active) {

		AudioMode mode = isRecording.get();
		log.trace((active ? "Activate recording from " : "de-activate recording from ") + mode);
		
		if (active && (recording == null || mode == NEW)) {
			recording = new Recording(true); // threaded to accept live stream
			isRecording.set(STARTING);
			
			listeners.forEach(listener -> {listener.update(Property.STATUS, Status.ACTIVE); });
			
			Console.addText(name + " recording starting");
		} else if (active && (isPlaying.get() == RUNNING || isPlaying.get() == STARTING)) {
			isRecording.set(STARTING);
			Console.addText(name + " overdub starting");
		} else if (active && (recording != null || mode == STOPPED)) {
			isRecording.set(STARTING);
			Console.addText("silently overdubbing on " + name);
		}
		
		else if (mode == RUNNING && !active) {
			isRecording.set(STOPPING);
			
			if (length == null) // first time
				listeners.forEach(listener -> {listener.update(Property.STATUS, Status.TERMINATED);});
			
			length = recording.size();
			recordedLength = System.currentTimeMillis() - _start;
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
		return this.getClass().getSimpleName() + " " + name;
	}

	@Override
	public void setRecording(Recording sample) {
		super.setRecording(sample);
		isRecording.set(STOPPED);
		sample.startListeners();
	}

	// TODO
	public void mute(String channel, boolean mute) {
		Console.info("Mute Record Channel currently not implemented");
//		for (MixerPort p : inputPorts) 
//			if (p.getName().contains(channel)) {
//				p.setOnLoop(!mute);
//				
//				log.info( (mute ? "Muted " : "Unmuted ") + p.getName() + " on recording track: " + getName());
//			}
	}

	/** for process() thread */
	private final boolean recording() {
		isRecording.compareAndSet(STOPPING, STOPPED);
			
		if (isRecording.compareAndSet(STARTING, RUNNING)) {
			_start = System.currentTimeMillis();
		}

		if (isRecording.get() == RUNNING)
			for (Channel m : JudahZone.getChannels())
				if (!m.isMuteRecord()) return true;
		return false;
	}
	
	////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

	@Override
	public void process() {
		counter = tapeCounter.get();
		super.process();
		if (!recording()) return;
		
		newBuffer = memory.getArray();
		firstTime = true;
		
		for (Channel channel : JudahZone.getChannels()) {
			if (channel.isOnMute() || channel.isMuteRecord())
				continue;
			if (channel.isSolo() && type != Type.SOLO) continue;
			if (!inputPorts.contains(channel.getLeftPort())) continue;
			
			fromJack = channel.getLeftPort().getFloatBuffer();
			if (firstTime) {
				FloatBuffer.wrap(newBuffer[LEFT_CHANNEL]).put(fromJack); // processEcho
				if (channel.isStereo()) 
					FloatBuffer.wrap(newBuffer[RIGHT_CHANNEL]).put(
							channel.getRightPort().getFloatBuffer()); 
				else {
					fromJack.rewind();
					FloatBuffer.wrap(newBuffer[RIGHT_CHANNEL]).put(fromJack);
				}
				firstTime = false;
			}
			else { 
				processAdd(fromJack, newBuffer[LEFT_CHANNEL]);
				processAdd( channel.isStereo() ? 
						channel.getRightPort().getFloatBuffer() :
						fromJack, newBuffer[RIGHT_CHANNEL]);
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

