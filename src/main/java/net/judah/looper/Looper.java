package net.judah.looper;

import static net.judah.api.Notification.Property.LOOP;

import java.util.ArrayDeque;
import java.util.ArrayList;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.judah.api.Notification.Property;
import net.judah.api.ProcessAudio.Type;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.LoopWidget;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.util.Constants;

@RequiredArgsConstructor 
public class Looper extends ArrayList<Loop> implements TimeListener {
	public static final int LOOPERS = 4;

	@Getter private final JudahClock clock;
    @Getter private final Loop loopA;
    @Getter private final Loop loopB;
    @Getter private final Loop loopC;
    @Getter private final SoloTrack soloTrack;
    @Getter private long recordedLength;
    @Getter private Loop primary;
    @Getter private final ArrayDeque<Loop> onDeck = new ArrayDeque<>();

    private int syncCounter;
    @Setter protected LoopWidget feedback;

	public Looper(JackPort l, JackPort r, Zone sources, LineIn solo, JudahClock clock) {
		this.clock = clock;
		loopA = new Loop("A", this, sources, "LoopA.png", Type.SYNC, clock, l, r);
        loopB = new Loop("B", this, sources, "LoopB.png", Type.BSYNC, clock, l, r);
        loopC = new Loop("C", this, sources, "LoopC.png", Type.FREE, clock, l, r);
        soloTrack = new SoloTrack(solo, this, sources, "LoopD.png", clock, l, r);
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
    	if (feedback != null && ++syncCounter > 8) { 
    		MainFrame.update(feedback);
        	syncCounter = 0;
        }

    }

    void setRecordedLength(long length, Loop primary) {
    	this.primary = primary;
    	recordedLength = length;
    	clock.letItBeKnown(LOOP, Status.NEW);
    }
    
    public Loop byName(String search) {
    	for (Loop l : this) 
				if (l.getName().equals(search))
					return l;
    	return null;
    }

    public void reset() { // get a process() in
    	Constants.timer(23, ()->clear());
	}

    /** deletes loops */
	@Override
	public void clear() {
        recordedLength = 0;
        primary = null;
		for (int i = 0; i < LOOPERS; i++) { // don't erase the sampler
    		Loop s = get(i);
        	s.record(false);
            s.clear();
            s.setActive(false);
        }
        if (feedback != null)
        	MainFrame.update(feedback);
        loopC.setType(Type.FREE);
        // flush onDeck?
    }

    public int indexOf(Channel loop) {
    	for (int i = 0; i < size(); i++)
    		if (get(i) == loop)
    			return i;
    	return -1;
    }

    
    public void flush() {
    	for (int i = onDeck.size() - 1; i >= 0; i--) {
    		MainFrame.update(onDeck.removeLast());
    	}
    }
    
	public void onDeck(Loop loop) {
		if (onDeck.contains(loop) == false)
			onDeck.add(loop);
		else 
			onDeck.remove(loop);
		MainFrame.update(loop);
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
		if (primary == null)
			return;
		if (prop == Property.BARS && (int)value == clock.getLength()) {
			for (Loop loop : this) {
				if (!loop.isActive() || loop.getType() == Type.FREE) continue;
				// loop boundary, hard sync to start
				loop.setTapeCounter(loop.getRecording().size() - 1);
			}
		}
	}

	void topUp(int count) {
		
		if (false == onDeck.isEmpty() && recordedLength > 0) { 
			Loop start = onDeck.poll();
			clock.syncUp(start, 0);
			start.record(true);
		}
		clock.letItBeKnown(LOOP, count);
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

