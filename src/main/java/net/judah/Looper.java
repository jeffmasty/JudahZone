package net.judah;

import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.AudioMode;
import net.judah.clock.JudahClock;
import net.judah.looper.Loop;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.mixer.Channel;
import net.judah.mixer.DrumTrack;
import net.judah.plugin.Carla;
import net.judah.util.Icons;

/** use {@link #addSample(Loop)} instead of add() */
@RequiredArgsConstructor
public class Looper {

	public static final int LOOPERS = 4;
    @Getter private final Loop[] loops = new Loop[LOOPERS];

    private final List<JackPort> outports;

    @Getter private Loop loopA;
    @Getter private Loop loopB;
    @Getter private Loop loopC;
    @Getter private DrumTrack drumTrack;
    
    /** pause/unpause specific loops, clock-aware */
    @RequiredArgsConstructor @Getter
    private class Pause extends ArrayList<Loop> {
    	private final boolean activeClock;
    }
    /** pause/unpause specific loops */
    private Pause suspended = null; 
    
    public void add(Loop s) {
        s.setOutputPorts(outports);
        for (int i = 0; i < loops.length; i++)
        	if (loops[i] == null) {
        		loops[i] = s;
        		break;
        	}
    }

    public void clear() {
        for (Loop s : loops) {
            s.clear();
            s.play(true); // armed;
        }
        if (JudahClock.isLoopSync()) {
        	JudahClock.setOnDeck(SelectType.SYNC);
        	MainFrame.update(loopA);
        }
    }

    public void stopAll() {
        for (Loop s : loops) {
        	s.record(false);
            s.play(false);
        }
    }

    public void init(Carla carla) {
        // TODO...

        loopA = new Loop("A", this);
        loopA.setIcon(Icons.load("LoopA.png"));
        loopA.setReverb(carla.getReverb());
        //loopA.play(true); // armed;
        add(loopA);
        
        loopB = new Loop("B", this);
        loopB.setIcon(Icons.load("LoopB.png"));
        loopB.setReverb(carla.getReverb2());
        //loopB.play(true);
        add(loopB);

        loopC = new Loop("C", this);
        //loopC.play(true); // armed;
        loopC.setIcon(Icons.load("LoopC.png"));
        add(loopC);

        drumTrack = new DrumTrack(loopA, JudahZone.getChannels().getCalf(), this);
        drumTrack.setIcon(Icons.load("Drums.png"));
        add(drumTrack);

//        // TODO for MIDI and samples
//        loopD = new Recorder("D", ProcessAudio.Type.ONE_SHOT);
//        loopD.setIcon(Icons.load("LoopD.png"));
//        add(loopD);

        // drumTrack.toggle();

    }


    /** in Real-Time thread */
    public void process() {
        // do any recording or playing
        for (Loop sample : loops) {
            sample.process();
        }
    }

//    /** multi-threaded */
//    public void syncLoop(Loop source, Loop target) {
//    	if (source.getRecording() == null || source.getRecording().isEmpty()) {
//    		// nothing recorded yet, but we are setup to sync to master loop
//    		target.armRecord(source);
//    	}
//    	else {
//    		new Thread(() -> {
//	    		if (target.hasRecording()) 
//	    			target.duplicate(); 
//	    		else {
//	    			target.setRecording(new Recording(source.getRecording().size()));
//		    		target.getIsPlaying().set(AudioMode.STARTING);
//		    		if (target.isSync()) {
//            			target.setSync(false);
//            			target.record(true);
//            		}
//		    		
//	    		}}).start();
//    	}
//    }

    public Loop get(int i) {
        return loops[i];
    }

    public int size() {
        return loops.length;
    }

    public int indexOf(Channel loop) {
    	for (int i = 0; i < loops.length; i++)
    		if (loops[i] == loop)
    			return i;
    	return -1;
    }

	public void reset() {
			stopAll();
			new Thread() {
				@Override public void run() {
					try { // to get a process() in
						Thread.sleep(23);} catch (Exception e) {} 
					clear();
                    }}.start();
	}

	/** pause/unpause any running loops, stop/restart clock if it is running */
	public void pause(boolean pauseClock) {
		if (suspended == null) {
			boolean clock = JudahClock.getInstance().isActive();
			
			suspended = new Pause(pauseClock? false : clock);
			if (clock && pauseClock) 
				JudahClock.getInstance().end();
			for (Loop s : loops) 
				if (s.isPlaying() == AudioMode.RUNNING) {
					s.setTapeCounter(0);
					suspended.add(s);
				}
			stopAll();
		}
		else {
			for (Loop s : suspended) 
				s.play(true);
			if (suspended.isActiveClock()) 
				JudahClock.getInstance().begin();
			else if (pauseClock == false && JudahClock.getInstance().isActive())
				JudahClock.getInstance().begin(); // re-sync
			suspended = null;
		}
	}

	//    public boolean remove(Object o) {
	//        if (o instanceof Sample == false) return false;
	//        synchronized (this) {
	//            return loops.remove(o);
	//        }
	//    }


}