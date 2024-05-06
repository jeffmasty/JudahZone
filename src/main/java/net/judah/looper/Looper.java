package net.judah.looper;

import java.security.InvalidParameterException;
import java.util.Vector;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.PlayAudio.Type;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.LoopWidget;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.Zone;
import net.judah.util.Constants;
import net.judah.util.Memory;
import net.judah.util.RTLogger;

@Getter
public class Looper extends Vector<Loop> implements TimeListener {

	public static final int LOOPERS = 4;
	private static final String ZERO = "0.0s";
	
	private final JudahClock clock;
	private final Loop loopA;
	private final Loop loopB;
	private final Loop loopC;
	private final SoloTrack soloTrack;
	private Loop primary;
	private String recordedLength = ZERO;
	int bars;
	private final Vector<Loop> onDeck = new Vector<>();
	private final Vector<Channel> fx = new Vector<>();
	private final LoopWidget loopWidget = new LoopWidget(this);

	public Looper(JackPort l, JackPort r, Zone sources, LineIn solo, JudahClock clock, Memory memory) {
		this.clock = clock;
		loopA = new Loop("A", "LoopA.png", Type.SYNC, this, sources, l, r, memory);
		loopB = new Loop("B", "LoopB.png", Type.BSYNC, this, sources, l, r, memory);
		loopC = new Loop("C", "LoopC.png", Type.FREE, this, sources, l, r, memory);
		soloTrack = new SoloTrack(solo, this, sources, l, r, memory);
		add(loopA);
		add(loopB);
		add(loopC);
		add(soloTrack);
		clock.addListener(this);
	}

	/** play and/or record loops and samples in Real-Time thread */
	public void process() {
		forEach(loop->loop.process());		
		if (hasRecording()) {
			loopWidget.countUp();
		}
	}
	/** process() silently (keep sync'd while mains muted) */
	public void silently() {
		if (!hasRecording())
			return;
		forEach(loop->loop.silently());		
		loopWidget.countUp();
	}

	public boolean hasRecording() {
		return primary != null && primary.getLength() != 0;
	}

	public int getLength() {
		return hasRecording() ? primary.getLength() : 0;
	}

	@Override public synchronized Loop remove(int index) {
		throw new InvalidParameterException("remove not implemented");
	}

	void setPrimary(Loop primary) {
		this.primary = primary;
		//clock.reset();
		
		int tape = primary.getTapeCounter().get();
		for (int i = 0; i < size(); i++) {
			Loop loop = get(i);
			if (loop == primary)
				continue;
			if (loop.isRecording() && loop instanceof SoloTrack && ((SoloTrack)loop).isSolo())
				loop.record(false); 
			loop.getTapeCounter().set(tape);
			loop.setType(primary.getType() == Type.FREE ? Type.FREE : 
					loop.type == Type.SOLO ?  Type.SOLO : Type.SYNC);
			
		}
		
		Type t = primary.getType();
		if (t != Type.SOLO) {
			for (Loop l : this)
				if (l != primary)
					l.setType(t);
		}
		if (t != Type.FREE)
			bars = JudahClock.getLength();

		setRecordedLength();
		RTLogger.log("Primary", primary + (primary.type == Type.FREE ? 
				(" frames: " + primary.length) : " bars: " + bars));
	}

	public void rewind() {
		for (Loop l : this) 
			if (l.isPlaying())
				l.rewind();
	}

	/** zeros-out loop recordings, ignore super.clear() */
	@Override public void clear() {
		primary = null;
		bars = 0;
		
		for (int i = 0; i < size(); i++)
			get(i).clear(); // clear loops
		for (int i = onDeck.size() - 1; i >= 0; i--) // clear onDeck
			MainFrame.update(onDeck.remove(onDeck.size() - 1));
		recordedLength = ZERO;
		MainFrame.update(loopWidget);
	}

	public void clear(Loop loop) {
		if (primary == loop) { // next primary
			for (Loop next : this) {
				if (next != loop && next.getLength() > 0) {
					primary = next;
					setRecordedLength();
				}
			}
		}
		loop.clear();
		loop.setType(primary.getType());
	}

	void setRecordedLength() {
		recordedLength = Float.toString(primary.seconds());
		if (recordedLength.length() > 4)
			recordedLength = recordedLength.substring(0, 4);
		recordedLength += "s";
		MainFrame.update(loopWidget);
	}
	
	/** make sure all loops have enough blank tape */
	void catchUp(Loop loop, int frames) {
		if (primary == null)
			Constants.execute(() -> {
				for (Loop l : this) {
					if (l == loop)
						continue;
					if (l.isRecording())
						continue; // solotrack
					l.catchUp(frames);
				}
			});
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

	public void verseChorus() {
		for (Loop loop : this) {
			if (!loop.isDrumTrack())
				loop.setOnMute(!loop.isOnMute());
		}
	}

	public void syncFx(Channel ch) {
		if (fx.remove(ch) == false)
			fx.add(ch);
		MainFrame.update(ch);
	}

	void loopCount(int count) {
		clock.loopCount(count);

		// trigger fx changes
		for (int i = fx.size() - 1; i >= 0; i--)
			fx.remove(i).toggleFx();
		
		// govern other loops
		for (int i = 0; i < size(); i++)
			get(i).boundary(); 
		

		// progress onDeck
		if (!onDeck.isEmpty())
			onDeck.remove(0).syncRecord();
		
	}

	public void onDeck(Loop loop) {
		if (onDeck.contains(loop) == false)
			onDeck.add(loop);
		else
			onDeck.remove(loop);
		MainFrame.update(loop.getDisplay());
	}

	public boolean isOnDeck(Loop loop) {
		return onDeck.contains(loop);
	}

	
	@Override
	public void update(Property prop, Object value) {
		if (prop != Property.BARS) 
			return;
		if (primary == null || primary.getType() == Type.FREE)
			return;
		if ((int)value % JudahClock.getLength() == 0)
			// sync loops to clock
			for (int i = 0; i < size(); i++)
				get(i).boundary(); 
	}
	
	void checkSoloSync() {
		if (onDeck.contains(soloTrack) && soloTrack.isSolo()) {
			onDeck.remove(soloTrack);
			soloTrack.syncRecord();
		}
	}

	public void clockSync() {
		forEach(loop -> loop.setType(loop.getType() == Type.SOLO ? Type.SOLO : Type.SYNC));
	}

	public int getBars() {
		if (bars == 0)
			return JudahClock.getLength();
		return bars;
	}

}