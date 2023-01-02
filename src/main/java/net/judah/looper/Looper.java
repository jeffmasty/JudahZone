package net.judah.looper;

import static net.judah.api.Notification.Property.LOOP;

import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.Notification.Property;
import net.judah.api.ProcessAudio.Type;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeNotifier;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;

@RequiredArgsConstructor @Getter
public class Looper extends ArrayList<Loop> implements TimeListener, TimeNotifier {

	public static final int LOOPERS = 4; 
	private final JackPort left;
	private final JackPort right;
    private final Loop loopA;
    private final Loop loopB;
    private final Loop loopC;
    private final SoloTrack soloTrack;
    private long recordedLength;
    private Loop primary;
    
    @Getter private final ArrayList<Loop> onDeck = new ArrayList<>(); 
    protected final ArrayList<TimeListener> listeners = new ArrayList<>();

    
	public Looper(JackPort l, JackPort r, Zone sources, LineIn solo, JudahClock clock) {
        left = l;
        right = r;
		loopA = new Loop("A", this, sources, "LoopA.png", Type.SYNC, clock);
        loopB = new Loop("B", this, sources, "LoopB.png", Type.SYNC, clock);
        loopC = new Loop("C", this, sources, "LoopC.png", Type.FREE, clock);
        soloTrack = new SoloTrack(solo, this, sources, "LoopD.png", clock);
        add(loopA);
        add(loopB);
        add(loopC);
        add(soloTrack);
        clock.addListener(this);
	}
	
	/** play and/or record loops and samples in Real-Time thread */
	public void process() {
    	for (Loop l : this)
    		l.process();
    }

    public void setRecordedLength(long length, Loop primary) {
    	this.primary = primary;
    	recordedLength = length;
    	new ArrayList<>(listeners).forEach(
    			listener -> {listener.update(LOOP, Status.NEW);});
    }
    
    public Loop byName(String search) {
    	for (Loop l : this) 
				if (l.getName().equals(search))
					return l;
    	return null;
    }

    public void reset() {
			new Thread(() -> {
				try { // to get a process() in
					Thread.sleep(23);
				} catch (Exception e) {} 
				clear();
			}).start();
	}

    // ---- Sync Section ----------------
    @Override public void addListener(TimeListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
            if (l instanceof Channel) {
            	MainFrame.update(l);
            }
        }
    }

    @Override public void removeListener(TimeListener l) {
        listeners.remove(l);
    }

    /** deletes loops */
	@Override
	public void clear() {
        recordedLength = 0;
		for (int i = 0; i < LOOPERS; i++) { // don't erase the sampler
    		Loop s = get(i);
        	s.record(false);
            s.clear();
            s.setActive(false);
        }
		new ArrayList<TimeListener>(listeners).forEach(listener -> 
        		listener.update(LOOP, Status.TERMINATED));
		MainFrame.update(loopA);
    }

    public int indexOf(Channel loop) {
    	for (int i = 0; i < size(); i++)
    		if (get(i) == loop)
    			return i;
    	return -1;
    }

    
	public void onDeck(Loop loop) {
		if (onDeck.contains(loop) == false)
			onDeck.add(loop);
		else 
			onDeck.remove(loop);
		MainFrame.update(loop);
	}

	public void checkOnDeck() {
		if (onDeck.isEmpty())
			return;
		Loop start = onDeck.remove(0);
		start.record(true);
		start.getSync().syncUp(0);
	}
    
	public void verseChorus() {
		for (Loop loop : this) {
			if (loop.getType() != Type.DRUMTRACK)
				loop.setOnMute(!loop.isOnMute());
		}
	}

	public void head() {
		for (Loop l : this) {
			l.setOnMute(false);
			if (l.hasRecording())
				l.setTapeCounter(0);
		}
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			for (Loop loop : this) {

				if (!loop.isActive() || loop.getType() == Type.FREE) continue;
				int tape = loop.getTapeCounter().get();
				// if near loop border, hard sync to start
				if (tape != 0 && (loop.getRecording().size() - tape < 5 || tape < 4)) {
					loop.setTapeCounter(loop.getRecording().size() - 1);
				}
			}
		}
	}

	
}


//    /** pause/unpause specific loops */
//    private Pause suspended = null; 
//	/** pause/unpause specific loops, clock-aware */
//    @RequiredArgsConstructor @Getter
//    private class Pause extends ArrayList<Loop> {
//    	private final boolean activeClock; }
//	/** pause/unpause any running loops, stop/restart clock if it is running */
//	public void pause(boolean pauseClock) {
//		if (suspended == null) {
//			boolean clock = getClock().isActive();
//			suspended = new Pause(pauseClock? false : clock);
//			if (clock && pauseClock) 
//				getClock().end();
//			for (Loop s : this) 
//				if (s.isPlaying() == AudioMode.RUNNING) {
//					s.setTapeCounter(0);
//					suspended.add(s);
//		}}  else {
//			for (Loop s : suspended) 
//				s.setOnMute(true);
//			if (suspended.isActiveClock()) 
//				getClock().begin();
//			else if (pauseClock == false && getClock().isActive())
//				getClock().begin(); // re-sync
//			suspended = null;	}}

