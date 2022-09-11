package net.judah.looper;

import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.SoloTrack;
import net.judah.plugin.Carla;
import net.judah.util.Icons;

@RequiredArgsConstructor
public class Looper extends ArrayList<Loop> {

	public static final int LOOPERS = 4;
    private final List<JackPort> outports;
    @Getter @Setter private long recordedLength;

    @Getter private Loop loopA;
    @Getter private Loop loopB;
    @Getter private Loop loopC;
    @Getter private SoloTrack drumTrack;
    
    /** pause/unpause specific loops, clock-aware */
    @RequiredArgsConstructor @Getter
    private class Pause extends ArrayList<Loop> {
    	private final boolean activeClock;
    }
    /** pause/unpause specific loops */
    private Pause suspended = null; 
    
    @Override
	public boolean add(Loop loop) {
        loop.setOutputPorts(outports);
        return super.add(loop);
    }

    /** does not delete loops, clears their recordings */
    @Override 
	public void clear() {
        for (Loop s : this) {
        	s.record(false);
            s.clear();
            s.dirty = false;
        }
        if (JudahClock.isLoopSync()) {
        	JudahClock.setOnDeck(SelectType.SYNC);
        	MainFrame.update(loopA);
        }
        recordedLength = 0;
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
        for (Loop loop : this) {
            loop.process();
        }
    }

    public int indexOf(Channel loop) {
    	for (int i = 0; i < size(); i++)
    		if (get(i) == loop)
    			return i;
    	return -1;
    }

	public void reset() {
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
			for (Loop s : this) 
				if (s.isPlaying() == AudioMode.RUNNING) {
					s.setTapeCounter(0);
					suspended.add(s);
				}
		}
		else {
			for (Loop s : suspended) 
				s.setOnMute(true);
			if (suspended.isActiveClock()) 
				JudahClock.getInstance().begin();
			else if (pauseClock == false && JudahClock.getInstance().isActive())
				JudahClock.getInstance().begin(); // re-sync
			suspended = null;
		}
	}

	public void verseChorus() {
		for (Loop loop : this) {
			if (loop != drumTrack)
				loop.setOnMute(!loop.isOnMute());
		}
	}

}
