package net.judah.mixer;

import static net.judah.gui.Gui.font;

import java.awt.Color;

import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.PlayAudio.Type;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;

/** displays Loop's label and btns and synchronizes recording by listening to the Clock */
public class LoopMix extends MixWidget implements Updateable, TimeListener {
	public static final Color CLOCKSYNC = YELLOW;
	public static final Color ONDECK = PINK;
	public static final Color PLAIN = BLUE;
	public static final Color PLAYING = GREEN;
	public static final Color RECORDING = RED;
	public static final Color MUTED = Color.BLACK;
	public static final Color QUIET = Color.GRAY;

	public static final int OFF = -1;
	public static final int FREE = 0;
	public static final int BSYNC = 1;
	
	/** bars until recording stops */
	private final Loop loop;
	private final Looper looper;
	private final JudahClock clock;
	private final JToggleButton rec = new JToggleButton("rec");
	private int countdown = OFF;
	@Getter protected boolean queued;
	private String update;
	
	public LoopMix(Loop l, Looper looper) {
		super(l, looper);
		this.loop = l;
		this.looper = looper;
		this.clock = looper.getClock();
		if (loop.getType() == Type.FREE)
			rec.setText("free");
		rec.addActionListener(e -> loop.trigger());
		mute.addActionListener(e -> channel.setOnMute(!channel.isOnMute()));
		if (loop instanceof SoloTrack) {
			sync.setText("solo");
			sync.addActionListener(e -> looper.getSoloTrack().toggle());
		}
		else 
			sync.addActionListener(e -> looper.onDeck(loop));
		sidecar.add(font(rec));
		sidecar.add(font(mute));
		sidecar.add(font(sync));
	}

	@Override protected Color thisUpdate() {
		if (loop == looper.getSoloTrack() && looper.getSoloTrack().isSolo() )
			sync.setBackground(CLOCKSYNC);
		else 
			sync.setBackground(null);
		
		if (loop.isRecording())
			rec.setSelected(true);
		else if (queued)
			rec.setSelected(true);
		else rec.setSelected(false);
		
		if (loop.getType() == Type.FREE) {
			if (! rec.getText().equals("free")) rec.setText("free");
		} else if (! rec.getText().equals("rec")) 
			rec.setText("rec");
		
		if (loop == looper.getSoloTrack()) {
			sync.setSelected(looper.getSoloTrack().isSolo());
			sync.setBackground(sync.isSelected() ? CLOCKSYNC: null);
		}
		else 
			sync.setSelected(looper.getOnDeck().contains(loop));
		if (update != null && countdown > OFF) {
			if (! title.getText().equals(update))
				title.setText(update);
		}
		else
			staticTitle();
		
		mute.setBackground(loop.isOnMute() ? PURPLE : null);
		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());
		
		Color bg = PLAIN;
		if (loop.isRecording()) 
			bg = RECORDING;
		else if (queued)
			bg = CLOCKSYNC;
		else if (looper.isOnDeck(loop))
			bg = ONDECK;
		else if (channel.isOnMute())  // line in/master track 
			bg = MUTED;
		else if (quiet())
			bg = QUIET;
		else if (loop.isPlaying())
			bg = PLAYING;
		return bg;
	}

	private void staticTitle() {
		String text;
		if (loop == looper.getLoopA()) { 
			int bars = JudahClock.getLength();
			text = bars < 10 ? "-" + bars + "-" : "" + bars;
		}
		else 
			text = channel.getName();
		if (!title.getText().equals(text))
			title.setText(text);
	}
	
	public void setFeedback(String string) {
		if (string == null) {
			clock.removeListener(this); // if on sync
			countdown = OFF;
			queued = false;
		}
		if (update != null && update.equals(string))
			return;
		update = string;
		MainFrame.update(this);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BEAT && loop.getType() != Type.FREE)
			setFeedback("<html><u>" + (int)value + "<br/></u>" + countdown + "</html>");
		else if (prop == Property.BARS && loop.getType() != Type.FREE) 
			updateBar((int)value);
		else if (prop == Property.LOOP && loop.getType() == Type.FREE && queued && loop.isRecording())
			loop.record(false); // stop recording sync'd loops in FREE mode 
	}
	
	private void updateBar(int bar) {
		if (!loop.isRecording()) { // queued
			loop.record(true);
			return;
		}
		
		if (countdown == OFF) {
			loop.record(false);
		}
		
		countdown += (loop.getType() == Type.BSYNC) ? 1 : -1;
		if (loop.getType() == Type.SYNC && countdown == 0) {
			loop.record(false);
		}
		
	}

	public void bsyncDown() {
		clock.setLength(countdown);
		countdown = OFF;
		setFeedback(" 🔁 ");
	}

	public void setCountdown() {
		Type type = loop.getType();
		if (type == Type.FREE) {
			countdown = FREE;
			queued = true;
		}
		else if (type == Type.BSYNC)
			countdown = BSYNC;
		else 
			countdown = looper.getBars() * loop.getFactor();
	
		clock.addListener(this);
		MainFrame.update(this);
	}

	public void queue() {
		queued = !queued;
		if (queued) {
			clock.addListener(this);
			setCountdown(); 
		}
		MainFrame.update(this);
	}

}
