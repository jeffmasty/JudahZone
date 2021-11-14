package net.judah.looper;

import static net.judah.JudahZone.getCarla;
import static net.judah.JudahZone.getEffectsL;
import static net.judah.JudahZone.getEffectsR;
import static net.judah.JudahZone.getMasterTrack;
import static net.judah.api.AudioMode.ARMED;
import static net.judah.api.AudioMode.NEW;
import static net.judah.api.AudioMode.RUNNING;
import static net.judah.api.AudioMode.STARTING;
import static net.judah.api.AudioMode.STOPPED;
import static net.judah.api.AudioMode.STOPPING;
import static net.judah.util.AudioTools.processAdd;
import static net.judah.util.AudioTools.processMix;
import static net.judah.util.Constants.LEFT_CHANNEL;
import static net.judah.util.Constants.RIGHT_CHANNEL;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.ProcessAudio;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeNotifier;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.util.AudioTools;
import net.judah.util.Console;

@Log4j
public class Sample extends Channel implements ProcessAudio, TimeNotifier {

    private final int bufferSize = JudahMidi.getInstance().getBufferSize();

    @Getter protected Recording recording;
    @Getter protected final List<JackPort> outputPorts = new ArrayList<>();
    @Setter @Getter protected Type type;

    @Getter protected final AtomicInteger tapeCounter = new AtomicInteger();
    @Getter protected final AtomicReference<AudioMode> isPlaying = new AtomicReference<>(STOPPED);
    @Getter private int loopCount = 0;
    @Getter protected Integer length;

    protected final ArrayList<TimeListener> listeners = new ArrayList<>();
    /** TODO synchronized loops or Time Master sync.. */
    @Getter @Setter protected boolean sync = false;

    // for process()
    private int updated; // tape position counter
    private FloatBuffer toJackLeft, toJackRight;
    protected float[][] recordedBuffer;
    private final float[] workL = new float[bufferSize];
    private final float[] workR = new float[bufferSize];
    private final FloatBuffer bufL = FloatBuffer.wrap(workL);
    private final FloatBuffer bufR = FloatBuffer.wrap(workR);

    public Sample(String name, Recording recording, Type type) {
        super(name);
        this.recording = recording;
        length = recording.size();
        setReverb(getCarla().getReverb());
    }

    protected Sample() {super("?"); }

    @Override public void setOutputPorts(List<JackPort> ports) {
        synchronized (outputPorts) {
            outputPorts.clear();
            outputPorts.addAll(ports);
        }
    }

    public int getSize() {
        if (recording == null) return 0;
        return recording.size();
    }

    @Override public final AudioMode isPlaying() {
        return isPlaying.get();
    }

    /** sets the playing flag (actual start/stop happens in the Jack thread) */
    @Override
    public void play(boolean active) {
        log.trace(name + " playing active: " + active);

        if (isPlaying.compareAndSet(NEW, active ? ARMED : NEW)) {
            if (active) Console.info(name + " Play is armed.");
        }
        isPlaying.compareAndSet(ARMED, active ? ARMED : ( hasRecording() ? STOPPED : NEW));

        isPlaying.compareAndSet(RUNNING, active ? RUNNING : STOPPING);

        if (isPlaying.compareAndSet(STOPPED, active ? STARTING : STOPPED)) {
            if (active)
                Console.info(name + " playing. sample has " + recording.size() + " buffers.");
        }
        if (gui != null) gui.update();
    }
    public boolean hasRecording() {
        return recording != null && length != null && length > 0;
    }

    @Override public void clear() {
        boolean wasRunning = false;
        if (isPlaying.compareAndSet(RUNNING, STOPPING)) {
            wasRunning = true;
            try { // to get a process() in
                Thread.sleep(20); } catch (Exception e) {	}
        }
        isPlaying.set(wasRunning ? ARMED : NEW);
        tapeCounter.set(0);
        recording = null;
        length = null;
        if (gui != null) gui.update();
        Console.info(name + " flushed.");
    }

    public void setRecording(Recording sample) {
        if (recording != null)
            recording.close();
        recording = sample;
        length = recording.size();
        Console.info("Recording loaded, " + length + " frames.");
        isPlaying.set(STOPPED);
    }

    public void erase() {
    	if (recording != null) {
    		recording.close();
    	}
    	length = 0;
    	recording.clear();
    	Console.info("Erased: " + getName());
    }
    
    @Override public String toString() {
        return "Sample " + name;
    }

    @Override public void addListener(TimeListener l) {
        if (!listeners.contains(l))
            listeners.add(l);
    }

    @Override public void removeListener(TimeListener l) {
        listeners.remove(l);
    }

    public void setTapeCounter(int current) {
        tapeCounter.set(current);
    }

    ////////////////////////////////////
    //     Process Realtime Audio     //
    ////////////////////////////////////
    @Override
    public void process() {

        if (!playing()) return;

        readRecordedBuffer();

        if (isOnMute()) return; // ok, we're on mute and we've progressed the tape counter.

        // gain & pan stereo
        float leftVol = (volume / 37f) * (1 - pan);
        float rightVol = (volume / 37f) * pan;

        AudioTools.processGain(recordedBuffer[LEFT_CHANNEL], workL, leftVol);
        AudioTools.processGain(recordedBuffer[RIGHT_CHANNEL], workR, rightVol);

        if (eq.isActive()) {
            eq.process(workL, true);
            eq.process(workR, false);
        }

        if (compression.isActive()) {
            compression.process(workL, bufL);
            compression.process(workR, bufR);
        }
        if (chorus.isActive())
            chorus.processStereo(bufL, bufR);
        if (overdrive.isActive()) {
            overdrive.processAdd(bufL);
            overdrive.processAdd(bufR);
        }

        if (delay.isActive()) {
            delay.processAdd(bufL, bufL, true);
            delay.processAdd(bufR, bufR, false);
        }
        if (cutFilter.isActive()) {
            cutFilter.process(bufL, bufR, 1);
        }

        // output
        if (reverb.isActive()) { // external reverb
            toJackLeft = getEffectsL().getFloatBuffer();
            toJackLeft.rewind();
            toJackRight = getEffectsR().getFloatBuffer();
            toJackRight.rewind();
            processAdd(bufL, getMasterTrack().getGainL(), toJackLeft); // grab master track gain
            processAdd(bufR, getMasterTrack().getGainR(), toJackRight); // on the way out to reverb
        }
        else {
            toJackLeft = outputPorts.get(LEFT_CHANNEL).getFloatBuffer();
            toJackLeft.rewind();
            toJackRight = outputPorts.get(RIGHT_CHANNEL).getFloatBuffer();
            toJackRight.rewind();
            processMix(workL, toJackLeft);
            processMix(workR, toJackRight);
        }

    }

    protected final boolean playing() {
        isPlaying.compareAndSet(STOPPING, STOPPED);
        isPlaying.compareAndSet(STARTING, RUNNING);
        return isPlaying.get() == RUNNING;
    }

    protected void readRecordedBuffer() {

        recordedBuffer = recording.get(tapeCounter.get());

        updated = tapeCounter.get() + 1;
        if (updated == recording.size()) {
            if (type == Type.ONE_TIME) {
                isPlaying.set(STOPPING);
                new Thread() { @Override public void run() {
                    JudahZone.getLooper().remove(Sample.this);
                }}.start();
            }
            updated = 0;
            loopCount++;
            new Thread() { @Override public void run() {
                for (int i = 0; i < listeners.size(); i++)
                    listeners.get(i).update(Property.LOOP, loopCount);
            }}.start();
        }
        tapeCounter.set(updated);
    }


}
