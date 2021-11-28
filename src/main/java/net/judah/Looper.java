package net.judah;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.AudioMode;
import net.judah.api.ProcessAudio;
import net.judah.clock.JudahClock;
import net.judah.looper.Recorder;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.ChannelGui.Output;
import net.judah.mixer.DrumTrack;
import net.judah.plugin.Carla;
import net.judah.util.Icons;
import net.judah.util.RTLogger;

/** use {@link #addSample(Sample)} instead of add() */
@RequiredArgsConstructor
public class Looper implements Iterable<Sample> {

    private final ArrayList<Sample> loops = new ArrayList<>();

    private final List<JackPort> outports;

    @Getter private DrumTrack drumTrack;
    @Getter private Recorder loopA;
    @Getter private Recorder loopB;
    @Getter private Recorder loopC;
    @Getter private Recorder loopD;
    
    private LooperGui gui;
    /** pause/unpause specific loops, clock-aware */
    @RequiredArgsConstructor @Getter
    private class Pause extends ArrayList<Sample> {
    	private final boolean activeClock;
    }
    /** pause/unpause specific loops */
    private Pause suspended = null; 
    
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
            ((Output)s.getGui()).armRecord(false);
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

        add(drumTrack);
        add(loopA);
        add(loopB);

        loopC = new Recorder("C", ProcessAudio.Type.FREE);
        loopC.play(true); // armed;
        loopC.setIcon(Icons.load("LoopC.png"));
        add(loopC);

        loopD = new Recorder("D", ProcessAudio.Type.ONE_SHOT);
        loopD.setIcon(Icons.load("LoopD.png"));
        add(loopD);

        drumTrack.toggle();

    }


    /** in Real-Time thread */
    public void process() {
        // do any recording or playing
        for (Sample sample : loops) {
            sample.process();
        }
    }

    public void syncLoop(Recorder source, Recorder target) {
    	if (source.getRecording() == null || source.getRecording().isEmpty()) 
    		// nothing recorded yet, but we are setup to sync to master loop
    		target.armRecord(source);
    	else if (target.hasRecording()) 
    		target.duplicate(); 
    	else {
    		target.setRecording(new Recording(source.getRecording().size(), true)); // normal op 
    		target.setTapeCounter(source.getTapeCounter().get());
    		target.play(true);
    		RTLogger.log(this, target.getName() + " sync'd to " + source.getName() + " " + 
    			+ target.getRecording().size() + " frames");
    	}
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

	public void reset() {
			stopAll();
			new Thread() {
				@Override public void run() {
					clear();
					//getDrumTrack().toggle();
                    }}.start();
	}

	/** pause/unpause any running loops, stop/restart clock if it is running */
	public void pause(boolean pauseClock) {
		if (suspended == null) {
			boolean clock = JudahClock.getInstance().isActive();
			
			suspended = new Pause(pauseClock? false : clock);
			if (clock && pauseClock) 
				JudahClock.getInstance().end();
			for (Sample s : loops) 
				if (s.isPlaying() == AudioMode.RUNNING) {
					s.setTapeCounter(0);
					suspended.add(s);
				}
			stopAll();
		}
		else {
			for (Sample s : suspended) 
				s.play(true);
			if (suspended.isActiveClock()) 
				JudahClock.getInstance().begin();
			else if (pauseClock == false && JudahClock.getInstance().isActive())
				JudahClock.getInstance().begin(); // re-sync
			suspended = null;
		}
	}


}