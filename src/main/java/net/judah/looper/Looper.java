package net.judah.looper;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.looper.sampler.Sample;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.SoloTrack;
import net.judah.plugin.Carla;
import net.judah.util.Icons;

@RequiredArgsConstructor @Getter
public class Looper extends ArrayList<Loop> {

	public static final int LOOPERS = 4; // anything above LOOPERS are samples and one-shots
	private final JackPort left;
	private final JackPort right;
	@Setter private long recordedLength;
    private Loop loopA;
    private Loop loopB;
    private Loop loopC;
    private SoloTrack drumTrack;
    
    /** pause/unpause specific loops, clock-aware */
    @RequiredArgsConstructor @Getter
    private class Pause extends ArrayList<Loop> {
    	private final boolean activeClock;
    }
    /** pause/unpause specific loops */
    private Pause suspended = null; 
    
    /** does not delete loops, clears their recordings */
	@Override
	public void clear() {
    	for (int i = 0; i < LOOPERS; i++) { // don't erase the sampler
    		Loop s = get(i);
        	s.record(false);
            s.clear();
            s.setActive(false);
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

    /** play and/or record loops and samples in Real-Time thread */
	public void process() {
    	for (Loop l : this)
    		l.process();
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
			if (loop != drumTrack && loop instanceof Sample == false)
				loop.setOnMute(!loop.isOnMute());
		}
	}

}
