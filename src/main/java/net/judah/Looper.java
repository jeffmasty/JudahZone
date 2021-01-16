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
import net.judah.mixer.bus.Reverb;
import net.judah.util.Console;
import net.judah.util.Icons;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper implements Iterable<Sample> {

    private final ArrayList<Sample> loops = new ArrayList<>();

    private final List<JackPort> outports;
    @Getter private DrumTrack drumTrack;
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
        drumTrack.clear();
        for (Sample s : loops) {
            s.clear();
            s.play(true); // armed;
        }
    }

    public void stopAll() {
        drumTrack.record(false);
        drumTrack.play(false);
        for (Sample s : loops) {
            if (s instanceof Recorder)
                ((Recorder) s).record(false);
            s.play(false);
        }
    }

    public void init(Reverb reverb) {
        // TODO...
        Recorder loopA = new Recorder("A", ProcessAudio.Type.FREE);
        loopA.setIcon(Icons.load("LoopA.png"));
        loopA.setReverb(reverb);
        loopA.play(true); // armed;
        add(loopA);
        Recorder loopB = new Recorder("B", ProcessAudio.Type.FREE);
        loopB.setIcon(Icons.load("LoopB.png"));
        loopB.setReverb(reverb);
        loopB.play(true);
        add(loopB);

        drumTrack = new DrumTrack(loops.get(0),
                JudahZone.getChannels().getDrums());
        drumTrack.setIcon(Icons.load("Drums.png"));
        drumTrack.setReverb(reverb);
        drumTrack.toggle();


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
        if (loops.get(0).getRecording() == null || loops.get(0).getRecording().isEmpty()) {
            // Arm Record on B
            ((Recorder)loops.get(1)).armRecord(loops.get(0));
            return;
        }
        ((Recorder)loops.get(0)).record(false);
        loops.get(1).setRecording(new Recording(loops.get(0).getRecording().size(), true));
        Console.info("Slave b. buffers: " + loops.get(1).getRecording().size());
        loops.get(1).play(true);
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