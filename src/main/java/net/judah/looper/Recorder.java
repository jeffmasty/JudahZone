package net.judah.looper;
import static net.judah.api.AudioMode.*;
import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.RecordAudio;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.midi.JudahMidi;
import net.judah.mixer.ChannelGui.Output;
import net.judah.mixer.LineIn;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
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
    Sample master;

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
    public void update(Property prop, Object value) {
        if (Property.STATUS == prop && Status.TERMINATED == value) {
            JudahZone.getDrummachine().play(false);
            setRecording(new Recording(master.getRecording().size(), true));
            play(true);
            record(true);
            master.removeListener(this);
            Console.info("Sync'd B recording. buffers: " + getRecording().size());
            new Thread() {
                @Override
                public void run() {
                    try {Thread.sleep(8);} catch(Throwable t) { }
                    JudahZone.getDrummachine().play(false);
                };

            }.start();

        }
    }

    public void armRecord(Sample loopA) {
        master = loopA;
        loopA.addListener(this);
        if (gui != null) ((Output)gui).armRecord(true);
        Console.info("Loop B sync'd and armed");
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

            if (length == null) {// first time
                ArrayList<TimeListener> l = new ArrayList<>(listeners);
                l.forEach(listener -> {listener.update(Property.STATUS, Status.TERMINATED);});
            }

            length = recording.size();
            recordedLength = System.currentTimeMillis() - _start;
            isPlaying.compareAndSet(NEW, STOPPED);
            isPlaying.compareAndSet(ARMED, STARTING);
            isRecording.set(STOPPED);
            Console.addText(name + " recording stopped, tape is " + recording.size() + " buffers long");
        }
        if (gui != null) gui.update();
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
        isRecording.compareAndSet(STOPPING, STOPPED);

        if (isRecording.compareAndSet(STARTING, RUNNING)) {
            _start = System.currentTimeMillis();
        }

        if (isRecording.get() == RUNNING)
            for (LineIn m : JudahZone.getChannels())
                if (!m.isMuteRecord()) return true;
        return false;
    }

}


// TODO public void mute(String channel, boolean mute) {
// for (MixerPort p : inputPorts)
//	if (p.getName().contains(channel)) {
//		p.setOnLoop(!mute);
//		log.info( (mute ? "Muted " : "Unmuted ") + p.getName() + " on recording track: " + getName()); } }

