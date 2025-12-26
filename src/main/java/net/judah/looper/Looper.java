package net.judah.looper;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.LoopWidget;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.omni.Recording;
import net.judah.omni.Threads;
import net.judah.util.Constants;
import net.judah.util.Memory;
import net.judah.util.RTLogger;

@Getter
public class Looper extends ArrayList<Loop> implements TimeListener, Updateable {

	static final int LOOPERS = 4;
	static final int COUNTUP = 6; // Gui feedback
	private static final String ZERO = "0.0s";

	private final JudahClock clock;
	private final Loop loopA;
	private final Loop loopB;
	private final Loop loopC;
	private final SoloTrack soloTrack;
	private final Memory mem;

	private final Vector<Loop> onDeck = new Vector<>();
	private final Vector<Channel> fx = new Vector<>();
	private final LoopWidget loopWidget = new LoopWidget(this);

	private Loop primary;
	private LoopType type;
	private String recordedLength = ZERO;
	private int measures;
	private int loopCount;
    private int countUp; // gui thread feedback

	public Looper(Collection<LineIn> sources, LineIn solo, JudahClock clock, Memory memory) {

		this.clock = clock;
		this.mem = memory;

		loopA = new Loop("A", "LoopA.png", this, sources);
		loopB = new Loop("B", "LoopB.png", this, sources);
		loopC = new Loop("C", "LoopC.png", this, sources);
		soloTrack = new SoloTrack(solo, this, sources);
		add(loopA);
		add(loopB);
		add(loopC);
		add(soloTrack);
		clock.addListener(this);
	}

	// TODO  multithread on Looper's own buffer
	/** play and/or record loops and samples in Real-Time thread */
	public void process(FloatBuffer left, FloatBuffer right) {
		forEach(loop->loop.process(left, right));
		if (hasRecording())
			countUp();
	}

	/** process() silently (keep sync'd while mains muted) */
	public void silently() {
		if (hasRecording()) {
			forEach(loop->loop.silently());
			countUp();
		}
	}

	/** @return false if looper is empty */
	public boolean hasRecording() {
		return primary != null;
	}

	/** @return frames used by primary loop or 0 */
	public int getLength() {
		return hasRecording() ? primary.getLength() : 0;
	}

	@Override public synchronized Loop remove(int index) {
		throw new InvalidParameterException("remove not implemented");
	}

	/** zeros-out loop recordings, ignore super.clear() */
	@Override public void clear() {
		throw new RuntimeException();
	}

	void setPrimary(Loop loop) {
		primary = loop;
		setRecordedLength(primary.seconds());
		catchUp(primary.getLength());

		String addendum = (type == LoopType.FREE) ?
				" " + recordedLength : " bars: " + measures;
		RTLogger.log("Primary", primary +  addendum);
		primary.getDisplay().setText(type == LoopType.FREE ? recordedLength : "" + measures );

		if (soloTrack.isRecording())
			soloTrack.capture(false);
		if (type == LoopType.BSYNC)
			type = LoopType.SYNC;
		clock.loopCount(loopCount); // here?
	}

	void setRecordedLength(float seconds) {
		recordedLength = Float.toString(seconds);
		if (recordedLength.length() > 4)
			recordedLength = recordedLength.substring(0, 4);
		recordedLength += "s";
		MainFrame.update(this);
	}

	public void delete() {
		primary = null;
		type = null;
		loopCount = measures = 0;
		for (int i = onDeck.size() - 1; i >= 0; i--) // clear onDeck
			MainFrame.update(onDeck.remove(onDeck.size() - 1));
		for (int i = 0; i < size(); i++)
			get(i).delete(); // clear loops
		recordedLength = ZERO;
		fx.clear();
		MainFrame.update(this);
	}

	public void delete(Loop loop) {
		if (primary == loop) { // next primary
			for (Loop next : this) {
				if (next != loop && next.getLength() > 0) {
					primary = next;
					setRecordedLength(primary.seconds()); // factor?
				}
			}
		}
		loop.delete();
	}

	/** make sure all loops have enough blank tape */
	void catchUp(final int frames) {
		for (Loop loop : this) {
			if (loop == primary)
				continue;
			if (loop.isRecording())
				continue; // solotrack, init primary
			if (loop.getTape().size() < frames) {
				Recording tape = loop.getTape();
				tape.add(mem.getFrame()); // instantly
				if (tape.size() < frames)
					Threads.execute(() -> {
						for (int i = tape.size(); i < frames; i++)
							tape.add(mem.getFrame());
				});
			}
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

	// TODO flag
	public void verseChorus() {
		forEach(loop->loop.verseChorus());
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
			soloTrack.capture(true);
		}
	}

	/** govern (synchronize) other loops */
	void boundary() {
		if (primary != null) {
			forEach(Loop::boundary);
			if (!onDeck.isEmpty()) {
				// progress onDeck
				Loop loop = onDeck.removeFirst();
				loop.timerOn();
				loop.capture(true);
			}
		}

		// trigger fx changes
		for (int i = fx.size() - 1; i >= 0; i--)
			fx.remove(i).toggleFx();
	}

	public void rewind() {
		stream().filter(loop -> loop.isPlaying()).forEach(loop -> loop.rewind());
	}

	@Override public void update() {
		stream().filter(loop -> loop.isPlaying())
			.forEach(loop -> loop.getDisplay().sweep());
		loopWidget.update();
	}

	@Override public void update(Property prop, Object value) {
		//if (primary != null) {

		if (prop == Property.BOUNDARY && hasRecording())
			boundary();
		else if (prop == Property.STATUS) // hard sync (scene?)
			for (Loop l : this)
				l.rewind();
		else if (prop == Property.BARS) {
			for (Loop loop : this) {
				if (!loop.isTimer())
					continue;

				if (loop.isRecording()) {
					int stopwatch = loop.count();
					if (type == LoopType.BSYNC) {
						if (stopwatch == measures)
							endCapture(loop);
						else
							loop.getDisplay().setText("+" + stopwatch); // measureFeedback
					}
					else if (type == LoopType.SYNC && stopwatch >= measures * loop.getFactor())
						endCapture(loop);
					// else measure feedback
				}
				else {
					loop.capture(true);
					checkSoloSync();
				}
			}
		}

		else if (prop == Property.BEAT) {
			for (Loop l : this)
				if (l.isTimer() && !l.isRecording())
					l.display.setText("- " + (clock.getMeasure() - ((int)value))); // pre-record countdown
		}

	}

	void increment() {
		++loopCount;
		if (!clock.isActive())
			clock.loopCount(loopCount);
		else if (type == LoopType.FREE)
			boundary();
	}

	private void countUp() {
		if (++countUp == COUNTUP) {
			countUp = 0;
			MainFrame.update(this); // schedule gui feedback
		}
	}

	void endCapture(Loop l) {
		l.capture(false);
		if (primary == null)
			setPrimary(l);
	}

	public void trigger() {
		if (primary != null)
			primary.capture(!primary.isRecording());
		else
			trigger(clock.isActive() ? loopA : loopC);
	}

	/** user engaged the record button, respond depending on loop type and current recording status */
    public void trigger(Loop loop) {

    	if (loop.isRecording()) {
			if (type == LoopType.BSYNC) {
				if (!hasRecording())
					sealBSync(loop);
			} else
    			endCapture(loop);
    	}
		else if (hasRecording())
			loop.capture(true); // everything ready for overdub
		else
			init(loop);

	}

    /** start recording, no primary yet
     * @param loop */
    private void init(Loop loop) {
    	type = getType(loop);
    	measures = (type == LoopType.SYNC ) ? JudahClock.getLength() : 0;
    	if (type == LoopType.FREE) {
        	loop.capture(true);
    		checkSoloSync();
    	}
    	else
    		loop.timerOn();
    }

    public void sealBSync(Loop loop) {
		measures = loop.getStopwatch() + 1;
		clock.setLength(measures);
		loopB.display.setText("!" + measures);
    }

	private LoopType getType(Loop loop) {

    	return loop == loopC ? LoopType.FREE : clock.isActive() ? loop == loopB ?
    			LoopType.BSYNC : LoopType.SYNC : LoopType.FREE;
    }

	public boolean isPrimary(Loop loop) {
		return loop == primary;
	}

	/** Check length of target loop against primary
	 * @param loop target loop
	 * @return the current tape counter of primary (or 0) */
	int conform(Loop loop) {
		if (primary == null)
			return 0;
		int target = (int) (loop.getFactor() * primary.getLength());

		if (loop.getLength() < target) {
			mem.catchUp(loop.getTape(), target);
			loop.setLength(target);
		}
		return primary.getTapeCounter().get(); // TODO if primary's factor is duplicated?
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder("Looper: ");

		if (hasRecording()) {
			sb.append(getType()).append(Constants.NL).append(Constants.NL);

			for (Loop l : this) {
				if (primary == l)
					sb.append("*");
				sb.append(l.getName()).append(" ");
				sb.append(l.getTapeCounter().get()).append("/").append(l.getLength());
				sb.append(Constants.NL);
			}
		}
		else {
			sb.append("Empty");
		}
	return sb.toString();
	}

}