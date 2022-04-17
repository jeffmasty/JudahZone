package net.judah.looper;
import static net.judah.api.AudioMode.ARMED;
import static net.judah.api.AudioMode.NEW;
import static net.judah.api.AudioMode.RUNNING;
import static net.judah.api.AudioMode.STARTING;
import static net.judah.api.AudioMode.STOPPED;
import static net.judah.api.AudioMode.STOPPING;
import static net.judah.util.AudioTools.processAdd;
import static net.judah.util.Constants.LEFT_CHANNEL;
import static net.judah.util.Constants.RIGHT_CHANNEL;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.Looper;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.api.Notification;
import net.judah.api.RecordAudio;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.midi.JudahMidi;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Recorder extends Sample implements RecordAudio, TimeListener {

    protected final AtomicReference<AudioMode> isRecording = new AtomicReference<>(AudioMode.NEW);
    @Getter @Setter protected boolean overwrite;
    @Getter protected final List<JackPort> inputPorts = new ArrayList<>();

    private final Memory memory;
    private float[][] newBuffer;
    private FloatBuffer fromJack;
    private boolean firstTime;
    private int counter;
    @Getter private long recordedLength = -1;
    private long _start;
    @Getter protected Sample primary;

    public Recorder(String name, Type type) {
        this(name, type, JudahZone.getInPorts(), JudahZone.getOutPorts());
    }

    public Recorder(String name, Type type, List<JackPort> inputPorts, List<JackPort> outputPorts) {
    	super(name);
        this.inputPorts.addAll(inputPorts);
        this.outputPorts.addAll(outputPorts);
        memory = new Memory(Constants.STEREO, JudahMidi.getInstance().getBufferSize());
        isPlaying.set(NEW);
    }

    @Override
    public void update(Notification.Property prop, Object value) {
        if (Notification.Property.STATUS == prop && Status.TERMINATED == value) {
        	if (primary.getRecording() != null) {
        		setRecording(new Recording(primary.getRecording().size(), true));
        		RTLogger.log(this, name + " sync'd recording. buffers: " + getRecording().size());
        	}
       		play(true);
            record(true);
            primary.removeListener(this);
            primary = null;
        }
    }

    public void armRecord(Sample loopA) {
    	if (primary == null) {
	        primary = loopA;
	        loopA.addListener(this);
	        sync = false;
    	}
    	else {
    		loopA.removeListener(this);
    		primary = null;
    		sync = true;
    	}
    	MainFrame.update(this);
    }

	@Override
    public void record(boolean active) {

        AudioMode mode = isRecording.get();

        if (active && (recording == null || mode == NEW)) {
            recording = new Recording(true); // threaded to accept live stream
            isRecording.set(STARTING);
            new ArrayList<TimeListener>(listeners).forEach(
            		listener -> {listener.update(Notification.Property.STATUS, Status.ACTIVE);});
            RTLogger.log(this, name + " recording starting");
        } else if (active && (isPlaying.get() == RUNNING || isPlaying.get() == STARTING)) {
            isRecording.set(STARTING);
            RTLogger.log(this, name + " overdub starting");

        } else if (active && (recording != null || mode == STOPPED)) {
            isRecording.set(STARTING);
            RTLogger.log(this, name + "silently overdubbing");
        }

        else if (mode == RUNNING && !active) {
            isRecording.set(STOPPING);

            if (length == null) { // Initial Recording, start other loopers
                Looper looper = JudahZone.getLooper();
                if (this == looper.getLoopA()) {
            		if (looper.getDrumTrack().isSync()) {
            			looper.getDrumTrack().record(false);
            		}
            		for (Sample s : looper.getLoops()) {
	            		if (s == this) continue;
	            		if (s.isSync()) s.sync = false;
	            		else looper.syncLoop(this, (Recorder)s); // preset blank loop of proper size
	        		}
            		new Thread( () -> {new ArrayList<>(listeners).forEach(
	                		listener -> {listener.update(Notification.Property.STATUS, Status.TERMINATED);});}).start();
            	}
            	RTLogger.log(this, name + " initial recording " + recording.size() + " buffers long");
            }
            else {
            	RTLogger.log(this, name + " overdubbed.");
            }
            length = recording.size();
            
            recordedLength = System.currentTimeMillis() - _start;
            isPlaying.compareAndSet(NEW, STOPPED);
            isPlaying.compareAndSet(ARMED, STARTING);
            isRecording.set(STOPPED);
            MainFrame.update(this);
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
        MainFrame.update(this);
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

        for (LineIn channel : JudahZone.getChannels()) {
            if (channel.isOnMute() || channel.isMuteRecord())
                continue;
            if (channel.isSolo() && type != Type.SOLO) continue;
            if (type == Type.SOLO && !channel.isSolo()) continue;

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
            if (overwrite) // uncommon
                recording.set(counter, newBuffer);
            else
                recording.dub(newBuffer, recordedBuffer, counter);
        }
        else
            recording.add(newBuffer);
    }

    /** for process() thread */
    private final boolean recording() {
        if (isRecording.compareAndSet(STOPPING, STOPPED)) MainFrame.update(this);

        if (isRecording.compareAndSet(STARTING, RUNNING)) {
            _start = System.currentTimeMillis();
            MainFrame.update(this);
        }

        if (isRecording.get() == RUNNING)
            for (LineIn m : JudahZone.getChannels())
                if (!m.isMuteRecord()) return true;
        return false;
    }

	public void duplicate() {
		new Thread( () -> {
		}).start();
		
			Recording source = getRecording();
			Recording dupe =  new Recording(source, 2, source.isListening());
			int offset = source.size();
			float[][] sound;
			for (int i = 0; i < offset; i++) {
				sound = source.elementAt(i);
				dupe.set(i, sound);
				dupe.set(i + offset, sound);
			}
			setRecording(dupe); 
			recordedLength = recordedLength * 2;
			RTLogger.log(this, name + " duplicated (" + recording.size() + " frames)");
	}

}


// TODO public void mute(String channel, boolean mute) {
// for (MixerPort p : inputPorts)
//	if (p.getName().contains(channel)) {
//		p.setOnLoop(!mute);
//		log.info( (mute ? "Muted " : "Unmuted ") + p.getName() + " on recording track: " + getName()); } }

