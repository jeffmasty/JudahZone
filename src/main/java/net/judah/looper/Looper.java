package net.judah.looper;

import java.security.InvalidParameterException;
import java.util.Vector;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.PlayAudio.Type;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.LoopMix;
import net.judah.mixer.Zone;
import net.judah.song.Cmdr;
import net.judah.song.Param;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public class Looper extends Vector<Loop> implements TimeListener, Cmdr {
	
	public static final int LOOPERS = 4;
	public static final int BSYNC_UP = Integer.MAX_VALUE;
	public static final int BSYNC_DOWN = 1000000;
	public static final int SYNC_INIT = -1;
	
	private final JudahClock clock;
	private final JackClient jackClient;
	private final Memory memory;
	private final Loop loopA;
    private final Loop loopB;
    private final Loop loopC;
    private final SoloTrack soloTrack;
    private final Vector<LoopMix> displays = new Vector<>();
    private final Vector<Loop> onDeck = new Vector<>();
    private final Vector<LoopMix> sync = new Vector<>();
    private final String[] keys;
    private String recordedLength = "0.0s";
    private Loop primary;
    private int barCounter;
	private int counter = SYNC_INIT;
	private int bars;
	private int countUp;
    
	public Looper(JackPort l, JackPort r, Zone sources, LineIn solo, JudahClock clock, JackClient client) {
		this.clock = clock;
		this.jackClient = client;
		memory = new Memory(Constants.STEREO, Constants.bufSize());
		loopA = new Loop("A", "LoopA.png", Type.SYNC, this, sources, l, r, memory);
        loopB = new Loop("B", "LoopB.png", Type.BSYNC, this, sources, l, r, memory);
        loopC = new Loop("C", "LoopCA.png", Type.FREE, this, sources, l, r, memory);
        soloTrack = new SoloTrack(solo, this, sources, l, r, memory);
        add(loopA);
        add(loopB);
        add(loopC);
        add(soloTrack);
        keys = new String[size()];
        for (int i = 0; i < size(); i++) {
        	displays.add(new LoopMix(get(i), this));
        	keys[i] = get(i).getName();
        }
        clock.addListener(this);
	}

	/** play and/or record loops and samples in Real-Time thread */
	public void process() {
		forEach(loop->loop.process());
		if (hasRecording() && ++countUp == 8) {
			MainFrame.update(this); // LoopWidget feedback
			countUp = 0;
		}
    }

	public boolean hasRecording() {
		return primary != null && primary.getLength() != 0;
	}
	
	public int getLength() {
		return hasRecording() ? primary.getLength() : 0;
	}
	
    void setPrimary(Loop primary) {
    	this.primary = primary;
    	clock.reset();
    	MainFrame.update(loopC);
    	recordedLength = Float.toString(primary.seconds());
    	if (recordedLength.length() > 4)
				recordedLength = recordedLength.substring(0, 4);
    	recordedLength += "s";
    	MainFrame.update(this);  
    	int tape = primary.getTapeCounter().get();
    	
    	for (int i = 0; i < size(); i++) {
    		Loop loop = get(i);
    		if (loop == primary) continue;
    		if (loop.isRecording()) loop.record(false); // soloTrack
    		loop.getTapeCounter().set(tape);
    	}
    	RTLogger.log("Primary", primary + " frames: " +  primary.length);
    }
    
    /** deletes loop recordings, ignore super.clear() */
	@Override public void clear() {
        primary = null;
        loopB.type = Type.BSYNC;
        loopC.type = Type.FREE;
        counter = SYNC_INIT;
        for (int i = 0; i < size(); i++)
        	get(i).clear(); // clear loops
		for(int i = sync.size() - 1; i >= 0; i--) 
			MainFrame.update(sync.remove(i)); // clear sync 
    	for (int i = onDeck.size() - 1; i >= 0; i--) // clear onDeck
    		MainFrame.update(onDeck.remove(onDeck.size() - 1));
		recordedLength = "0.0s";
    	MainFrame.update(this);
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

	public void checkSoloSync() {
		if (soloTrack.isSolo() && onDeck.contains(soloTrack)) 
			soloTrack.record(true);
	}

	public void loopCount(Loop looper, Object broadcast) {
		if (looper != primary)
			return;
		checkSyncDown();
		checkOnDeck();
		clock.loopCount(broadcast);
	}	

	private void checkOnDeck() {
		if (onDeck.isEmpty()) return;
		Constants.execute(()->{
			Loop startup = onDeck.remove(0);
			syncUp(startup, -1);
			startup.record(true);
		});
	}
	
	private void checkSyncDown() {
		if (sync.isEmpty()) return;
		LoopMix display = sync.get(0);
		if (display.getLoop().isRecording) {
			sync.remove(display);
			display.getLoop().endRecord();
		}
	}

	public void rewind() {
		for (Loop l : this) {
			if (l.isPlaying())
				l.rewind();
		}
	}

	void catchUp(Loop loop, int frames) {
		if (primary == null)
		Constants.execute(()->{
			for (Loop l : this) {
				if (l == loop) continue;
				if (l.isRecording()) 
					continue; // solotrack
				l.catchUp(frames);
		}});
	}

    public Loop byName(String key) {
    	for (Loop l : this) 
				if (l.getName().equals(key))
					return l;
    	return loopA; // fail
    }

    public int indexOf(Channel loop) {
    	for (int i = 0; i < size(); i++)
    		if (get(i) == loop)
    			return i;
    	return -1;
    }
    
    @Override public Loop resolve(String key) {
    	for (int i = 0; i < size(); i++)
    		if (get(i).getName().equals(key))
				return get(i);
		return null;
	}
    
	@Override public void execute(Param p) {
		Loop loop = resolve(p.val);
		if (loop == null)
			return;
		switch(p.getCmd()) {
			case Delete:
				loop.clear();
				break;
			case Dup:
				loop.doubled();
				break;
			case Record:
				if (!clock.isActive())
					break; // ignore in edit mode
				loop.record(true);
				if (loop.getType() != Type.FREE)
					syncUp(loop, 0);
				MainFrame.update(loop);
				break;
			case RecEnd:
				loop.record(false);
				break;
			case Sync:
				onDeck(loop);
				break;
			default: throw new InvalidParameterException("" + p);
		}
	}
	public LoopMix getDisplay(Loop loop) {
		for (LoopMix display : displays)
			if (display.getLoop() == loop)
				return display;
		return null;
	}
	
	public boolean isSync(Loop loop) {
		return sync.contains(getDisplay(loop));
	}
	
	public void tail(Loop loop) {
		LoopMix sync = getDisplay(loop);
		bars = BSYNC_DOWN;
		sync.setUpdate("BDN");
	}
	
	public void syncUp(Loop loop, int init) {
		LoopMix widget = getDisplay(loop);
		if (sync.contains(widget)) {
			syncDown(loop);
			return;
		}
		sync.add(widget); 
		counter = SYNC_INIT;
		bars = JudahClock.getLength();
		if (!hasRecording() && loop.getType() == Type.BSYNC) {
			bars = BSYNC_UP;
			widget.setUpdate(" 🔁 ");
		}
		MainFrame.update(loop);
	}
	
	public void syncDown(Loop loop) {
		LoopMix display = getDisplay(loop);
		if (sync.remove(display)) {
			display.setUpdate(null);
			MainFrame.update(display);
		}
	}

	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS && (int)value == 0)
			for (int i = 0; i < size(); i++) 
				get(i).boundary(); // sync loops to clock
		if (sync.isEmpty()) 
			return;
		else if (prop == Property.BARS) 
			updateBar((int)value);
		else if (prop == Property.BEAT) 
			updateBeat();
	}
	
	private void updateBar(int bar) {
		counter ++;
		for (int i = sync.size() - 1; i >= 0; i--) {
			LoopMix widget = sync.get(i);
			Loop loop = widget.getLoop();
			if (bars == BSYNC_DOWN) { // BSYNC ended
				loop.record(false); 
				clock.setLength(counter);
			}
			else if (loop.isRecording == false && clock.getBeat() % clock.getMeasure() == 0) {
				loop.record(true);
				checkSoloSync();
				widget.setUpdate("Go!");
			}
			else if (counter == bars) {
				if (!onDeck.isEmpty() && onDeck.get(0) == loop)
					onDeck.remove(loop);
				loop.endRecord();
			}
		}
	}
	
	private void updateBeat() {
		if (counter <= SYNC_INIT) { // not started, display beats until start
			int countdown = clock.getBeat() % clock.getMeasure() - clock.getMeasure();
			for (LoopMix widget : sync) 
				widget.setUpdate(countdown + "");
		}
		else 
			for (LoopMix widget : sync) 
				if (widget.getLoop().isRecording()) {
					int measure = clock.getMeasure();
					StringBuffer sb = new StringBuffer().append(1 + clock.getBeat() % measure);
					sb.append("/").append(measure);
					int showBars = bars < 100 ? bars - counter : counter + 1;
					widget.setUpdate("<html>"+ sb.toString() +"<br/>"+ showBars +"</html>");
				}
	}
	
}