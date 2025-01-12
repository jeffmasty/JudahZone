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
import net.judah.omni.Threads;
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
	private final Vector<Loop> onDeck = new Vector<>();
	private final Vector<Channel> fx = new Vector<>();
	private final LoopWidget loopWidget = new LoopWidget(this);
	private int loopCount;

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
		if (hasRecording())
			loopWidget.countUp();
	}
	/** process() silently (keep sync'd while mains muted) */
	public void silently() {
		if (hasRecording()) {
			forEach(loop->loop.silently());
			loopWidget.countUp();
		}
	}

	public boolean hasRecording() {
		return primary != null && primary.getLength() != 0;
	}

	public int getLength() {
		return hasRecording() ? primary.getLength() : 0;
	}

	public int getMeasures() {
		return hasRecording() ? primary.getMeasures() : 0;
	}

	@Override public synchronized Loop remove(int index) {
		throw new InvalidParameterException("remove not implemented");
	}

	void setPrimary(Loop primary) {
		this.primary = primary;
		setRecordedLength(primary.seconds());
		stream().filter(loop-> loop != primary).forEach(loop->loop.conform(primary));
		clock.loopCount(loopCount);

		RTLogger.log("Primary", primary + " frames: " + primary.length + " bars: " + primary.getMeasures());
	}

	void setRecordedLength(float seconds) {
		recordedLength = Float.toString(seconds);
		if (recordedLength.length() > 4)
			recordedLength = recordedLength.substring(0, 4);
		recordedLength += "s";
		MainFrame.update(loopWidget);
	}

	public void rewind() {
		for (Loop l : this)
			if (l.isPlaying())
				l.rewind();
	}

	/** zeros-out loop recordings, ignore super.clear() */
	@Override public void clear() {
		primary = null;
		loopCount = 0;
		for (int i = onDeck.size() - 1; i >= 0; i--) // clear onDeck
			MainFrame.update(onDeck.remove(onDeck.size() - 1));
		for (int i = 0; i < size(); i++)
			get(i).clear(); // clear loops
		recordedLength = ZERO;
		MainFrame.update(loopWidget);
	}

	public void clear(Loop loop) {
		if (primary == loop) { // next primary
			for (Loop next : this) {
				if (next != loop && next.getLength() > 0) {
					primary = next;
					setRecordedLength(primary.seconds()); // factor?
				}
			}
		}
		loop.clear();
		if (primary != null)
			loop.setType(primary.getType());
	}


	/** make sure all loops have enough blank tape */
	void catchUp(Loop primary, int frames) {

		for (Loop l : this) {
			if (l == primary)
				continue;
			if (l.isRecording())
				continue; // solotrack
			l.catchUp(frames);
		}
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


	public void onDeck(Loop loop) {
		if (onDeck.contains(loop))
			onDeck.remove(loop);
		else
			onDeck.add(loop);
		MainFrame.update(loop.getDisplay());
	}

	public boolean isOnDeck(Loop loop) {
		return onDeck.contains(loop);
	}

	void checkSoloSync() {
		if (onDeck.contains(soloTrack) && soloTrack.isSolo()) {
			onDeck.remove(soloTrack);
			soloTrack.trigger();
		}
	}

	/** govern (synchronize) other loops */
	void boundary() {
		stream().filter(element -> element != primary).forEach(Loop::boundary);

		// progress onDeck
		if (!onDeck.isEmpty())
			Threads.execute(()->onDeck.removeFirst().syncRecord());

		// trigger fx changes
		for (int i = fx.size() - 1; i >= 0; i--)
			fx.remove(i).toggleFx();
	}


	@Override
	public void update(Property prop, Object value) {
		if (primary == null)
			return;
		if (prop == Property.LOOP && primary.getType() == Type.FREE)
			boundary();
		else if ( (prop == Property.STATUS || prop == Property.BOUNDARY) && primary.getType() != Type.FREE)
			boundary();
	}

	public void clockSync() {
		forEach(loop -> loop.setType(loop.getType() == Type.SOLO ? Type.SOLO : Type.SYNC));
	}

	public int increment() {
		return ++loopCount;
	}

}