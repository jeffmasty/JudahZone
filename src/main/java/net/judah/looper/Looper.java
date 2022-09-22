package net.judah.looper;

import static net.judah.JudahZone.*;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.carla.TalReverb;
import net.judah.drumz.JudahDrumz;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.Channels;
import net.judah.mixer.SoloTrack;
import net.judah.synth.JudahSynth;
import net.judah.util.Icons;

@RequiredArgsConstructor @Getter
public class Looper extends ArrayList<Loop> {

	public static final int LOOPERS = 4; // anything above LOOPERS are samples and one-shots
	private final JackPort left;
	private final JackPort right;
    private final Loop loopA;
    private final Loop loopB;
    private final Loop loopC;
    private final SoloTrack drumTrack;
	@Setter private long recordedLength;
    
	public Looper(JackPort l, JackPort r, Channels ch, JudahSynth[] synths, JudahDrumz[] beats) {
        left = l;
        right = r;
		loopA = new Loop("A", this, ch, synths, beats);
        loopC = new Loop("C", this, ch, synths, beats);
        loopB = new Loop("B", this, ch, synths, beats);
        drumTrack = new SoloTrack(getInstruments().getCalf(), this, ch, synths, beats);
        drumTrack.setIcon(Icons.load("Drums.png"));
        add(loopA);
        add(loopB);
        add(loopC);
        add(drumTrack);
	}
	
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
			boolean clock = getClock().isActive();
			
			suspended = new Pause(pauseClock? false : clock);
			if (clock && pauseClock) 
				getClock().end();
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
				getClock().begin();
			else if (pauseClock == false && getClock().isActive())
				getClock().begin(); // re-sync
			suspended = null;
		}
	}

	public void verseChorus() {
		for (Loop loop : this) {
			if (loop != drumTrack)
				loop.setOnMute(!loop.isOnMute());
		}
	}

	public void setReverb(TalReverb rev1, TalReverb rev2) {
		loopA.setReverb(rev1);
        loopB.setReverb(rev2);
	}

}
