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
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public class Looper extends Vector<Loop> implements TimeListener, Cmdr {

	public static final int LOOPERS = 4;
	public static final int BSYNC_UP = Integer.MAX_VALUE;
	public static final int BSYNC_DOWN = 1000000;
	public static final int SYNC_INIT = -1;
	private static final String ZERO = "0.0s";
	private static final String BSYNC = " 🔁 ";
	
	private final JudahClock clock;
	private final JackClient jackClient;
	private final Memory memory;
	private final Loop loopA;
	private final Loop loopB;
	private final Loop loopC;
	private final SoloTrack soloTrack;
	private final Vector<LoopMix> displays = new Vector<>();
	private Loop countdown;
	private final Vector<Loop> onDeck = new Vector<>();
	private final Vector<Channel> fx = new Vector<>();
	private final String[] keys;
	private String recordedLength = ZERO;
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
		loopC = new Loop("C", "LoopC.png", Type.FREE, this, sources, l, r, memory);
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
		recordedLength = Float.toString(primary.seconds());
		if (recordedLength.length() > 4)
			recordedLength = recordedLength.substring(0, 4);
		recordedLength += "s";
		
		int tape = primary.getTapeCounter().get();
		for (int i = 0; i < size(); i++) {
			Loop loop = get(i);
			if (loop == primary)
				continue;
			if (loop.isRecording() && loop instanceof SoloTrack && ((SoloTrack)loop).isSolo())
				loop.record(false); // soloTrack
			loop.getTapeCounter().set(tape);
		}
		MainFrame.update(loopC);
		MainFrame.update(this);
		RTLogger.log("Primary", primary + " frames: " + primary.length);
	}

	/** zeros-out loop recordings, ignore super.clear() */
	@Override
	public void clear() {
		primary = null;
		loopB.type = Type.BSYNC;
		loopC.type = Type.FREE;
		counter = SYNC_INIT;
		for (int i = 0; i < size(); i++)
			get(i).clear(); // clear loops
		for (int i = onDeck.size() - 1; i >= 0; i--) // clear onDeck
			MainFrame.update(onDeck.remove(onDeck.size() - 1));
		countdown = null;
		recordedLength = ZERO;
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

	public void loopCount(int count) {
		if (countdown != null) {
			countdown.record(false);
			countdown = null;
		}
		if (!onDeck.isEmpty()) {
			countdown = onDeck.remove(0);
			countdown.record(true);
		}
		
		for (int i = fx.size() - 1; i >= 0; i--)
			fx.remove(i).toggleFx();
	
		clock.loopCount(count);
	}

	public void syncFx(Channel ch) {
		if (fx.remove(ch) == false)
			fx.add(ch);
		MainFrame.update(ch);
	}

	public void rewind() {
		for (Loop l : this) {
			if (l.isPlaying())
				l.rewind();
		}
	}

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

	@Override
	public Loop resolve(String key) {
		for (int i = 0; i < size(); i++)
			if (get(i).getName().equals(key))
				return get(i);
		return null;
	}

	@Override
	public void execute(Param p) {
		Loop loop = resolve(p.val);
		if (loop == null)
			return;
		switch (p.getCmd()) {
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
				syncUp(loop, JudahClock.getLength() - 1);
			break;
		case RecEnd:
			loop.record(false);
			break;
		case Sync:
			onDeck(loop);
			break;
		default:
			throw new InvalidParameterException("" + p);
		}
	}

	public LoopMix getDisplay(Loop loop) {
		for (LoopMix display : displays)
			if (display.getLoop() == loop)
				return display;
		return null;
	}

	public boolean isOnDeck(Loop loop) {
		return onDeck.contains(loop);
	}

	public void tail(Loop loop) {
		LoopMix sync = getDisplay(loop);
		bars = BSYNC_DOWN;
		sync.setUpdate("xxx");
	}

	public void syncUp(Loop loop, int length) {
		if (countdown == loop) { 
			syncDown();
			return;
		}
		countdown = loop;
		counter = SYNC_INIT;
		bars = length > 0 ? length : JudahClock.getLength();
		if (loop.getType() == Type.BSYNC) {
			this.bars = BSYNC_UP;
			getDisplay(countdown).setUpdate(BSYNC);
		}
		else 
			getDisplay(countdown).setUpdate(clock.countdown() + "");
	}

	public void syncDown() {
		if (countdown == null)
			return;
		getDisplay(countdown).setUpdate(null);
		countdown = null;
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS) {
			updateBar((int) value);
			if ((int) value % JudahClock.getLength() == 0) 
				for (int i = 0; i < size(); i++)
					get(i).boundary(); // sync loops to clock
		}
		else if (prop == Property.BEAT)
			updateBeat();
	}

	private void updateBar(int bar) {
		if (countdown == null) return;
		counter++;
		if (bars == BSYNC_DOWN) { // BSYNC ended
			countdown.record(false);
			clock.setLength(counter);
		} else if (countdown.isRecording == false) {
			countdown.record(true);
			checkSoloSync();
		} else if (bars == counter) {
			countdown.record(false);
		}
	}

	private void updateBeat() {
		if (countdown == null)
			return;
		if (countdown.isRecording && countdown.getLength() == 0) {
			int measure = clock.getMeasure();
			String show = "" + (bars == BSYNC_DOWN ? "BDN" : bars < 100 ? bars - counter : counter + 1);
			getDisplay(countdown).setUpdate("<html><u>" + (1 + clock.getBeat() % measure) + "<br/></u>" + show + "</html>");
		}
		else if (counter <= SYNC_INIT)  // not started, display beats until start
			getDisplay(countdown).setUpdate(clock.countdown() + "");
		else 
			getDisplay(countdown).setUpdate(null);
	}

	@Override
	public synchronized Loop remove(int index) {
		throw new InvalidParameterException("remove not implemented");
	}
}