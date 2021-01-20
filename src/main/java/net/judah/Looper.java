package net.judah;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.ProcessAudio;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.DrumTrack;
import net.judah.plugin.Carla;
import net.judah.util.Console;
import net.judah.util.Icons;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper implements Iterable<Sample> {

    private final ArrayList<Sample> loops = new ArrayList<>();

    private final List<JackPort> outports;

    @Getter private DrumTrack drumTrack;
    @Getter private Recorder loopA;
    @Getter private Recorder loopB;

    private LooperGui gui;

    public void add(Sample s) {
        s.setOutputPorts(outports);
        synchronized (this) {
            loops.add(s);
        }
        if (gui != null)
            gui.addSample(s);
    }

    public boolean remove(Object o) {
        if (o instanceof Sample == false) return false;
        if (gui != null)
            gui.removeSample((Sample)o);
        synchronized (this) {
            return loops.remove(o);
        }
    }

    public void clear() {
        for (Sample s : loops) {
            s.clear();
            s.play(true); // armed;
        }
    }

    public void stopAll() {
        for (Sample s : loops) {
            if (s instanceof Recorder)
                ((Recorder) s).record(false);
            s.play(false);
        }
    }

    public void init(Carla carla) {
        // TODO...

        loopA = new Recorder("A", ProcessAudio.Type.FREE);
        loopA.setIcon(Icons.load("LoopA.png"));
        loopA.setReverb(carla.getReverb());
        loopA.play(true); // armed;
        loopB = new Recorder("B", ProcessAudio.Type.FREE);
        loopB.setIcon(Icons.load("LoopB.png"));
        loopB.setReverb(carla.getReverb());
        loopB.play(true);
        drumTrack = new DrumTrack(loopA,
                JudahZone.getChannels().getDrums());
        drumTrack.setIcon(Icons.load("Drums.png"));
        drumTrack.setReverb(carla.getReverb());
        drumTrack.toggle();

        add(drumTrack);
        add(loopA);
        add(loopB);


    }


    /** in Real-Time thread */
    public void process() {
        // do any recording or playing
        for (Sample sample : loops) {
            sample.process();
        }
        drumTrack.process();
    }

    public void slave() {
        if (loopA.getRecording() == null || loopA.getRecording().isEmpty()) {
            // Arm Record on B
            loopB.armRecord(loopA);
            return;
        }
        loopB.record(false);
        loopB.setRecording(new Recording(loopA.getRecording().size(), true));
        Console.info("Slave b. buffers: " + loopB.getRecording().size());
        loopB.play(true);
    }

    public void registerListener(LooperGui looperGui) {
        gui = looperGui;
    }

    @Override
    public Iterator<Sample> iterator() {
        return loops.iterator();
    }

    public Sample get(int i) {
        return loops.get(i);
    }

    public int size() {
        return loops.size();
    }

    public Sample[] toArray() {
        return (loops.toArray(new Sample[loops.size()]));
    }

    public int indexOf(Channel loop) {
        return loops.indexOf(loop);
    }

}