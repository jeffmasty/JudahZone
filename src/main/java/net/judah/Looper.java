package net.judah;

import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.api.AudioMode;
import net.judah.looper.Loop;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.SoloTrack;
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
    @Getter private SoloTrack drumTrack;
    
    @Getter @Setter private long recordedLength;
    
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
        recordedLength = 0;
    }

    public void stopAll() {
        for (Loop s : loops) {
        	s.record(false);
            s.play(false);
        }
    }

    public void init(Carla carla) {

        loopA = new Loop("A", this);
        loopA.setReverb(carla.getReverb());
        add(loopA);
        
        loopB = new Loop("B", this);
        loopB.setReverb(carla.getReverb2());
        add(loopB);

        loopC = new Loop("C", this);
        add(loopC);

        drumTrack = new SoloTrack(JudahZone.getChannels().getCalf(), this);
        drumTrack.setIcon(Icons.load("Drums.png"));
        add(drumTrack);

    }


    /** in Real-Time thread */
    public void process() {
        // do any recording or playing
        for (Loop sample : loops) {
            sample.process();
        }
    }


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
			new Thread(() -> {
				try { // to get a process() in
					Thread.sleep(23);
				} catch (Exception e) {} 
				clear();
			}).start();
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

	public void verseChorus() {
		for (Loop loop : loops) {
			if (loop != drumTrack)
				loop.setOnMute(!loop.isOnMute());
		}
		// Loop verse = getLoopA();
		// Loop chorus = getLoopB();
		// Cycle.setVerse(Cycle.isVerse()); // Cycle.setTrigger(true);
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
