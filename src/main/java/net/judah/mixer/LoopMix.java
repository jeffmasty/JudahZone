package net.judah.mixer;

import static net.judah.gui.Gui.font;

import java.awt.Color;

import javax.swing.JToggleButton;

import net.judah.api.PlayAudio.Type;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;

/** displays Loop's label and btns and synchronizes recording by listening to the Clock */
public class LoopMix extends MixWidget implements Updateable {
	public static final Color CLOCKSYNC = YELLOW;
	public static final Color ONDECK = PINK;
	public static final Color PLAIN = BLUE;
	public static final Color PLAYING = GREEN;
	public static final Color RECORDING = RED;
	public static final Color MUTED = Color.BLACK;
	public static final Color QUIET = Color.GRAY;

	/** bars until recording stops */
	private final Loop loop;
	private final Looper looper;
	private final JToggleButton rec = new JToggleButton("rec");
	private String update;

	public LoopMix(Loop l, Looper looper) {
		super(l, looper);
		this.loop = l;
		this.looper = looper;
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
		if (update != null && loop.getStopwatch() > Loop.OFF) {
			if (! title.getText().equals(update))
				title.setText(update);
		}
		else
			staticTitle();

		if (loop == looper.getSoloTrack() && looper.getSoloTrack().isSolo() )
			sync.setBackground(CLOCKSYNC);
		else
			sync.setBackground(null);

		if (loop.isRecording())
			rec.setSelected(true);
		else if (loop.isTimer())
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

		mute.setBackground(loop.isOnMute() ? PURPLE : null);
		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());

		Color bg = PLAIN;
		if (loop.isRecording())
			bg = RECORDING;
		else if (loop.isTimer())
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
			int countdown = JudahClock.getLength();
			text = countdown < 10 ? "-" + countdown + "-" : "" + countdown;
		}
		else
			text = channel.getName();
		if (!title.getText().equals(text))
			title.setText(text);
	}

	public void clear() {
		update = null;
		MainFrame.update(this);
	}

	public void measureFeedback() {
		int count = loop.getStopwatch() + 1;
		String txt;
		switch (loop.getType()) {
			case BSYNC:
				txt = "+" + count;
				break;
			case FREE:
				txt = " ⏺ ";
				break;
			default:
				txt = "<html><u>" + count + "<br/></u>" + loop.getMeasures() + "</html>";
		}
		setFeedback(txt);
	}

	public void setFeedback(String string) {
		if (update != null && update.equals(string))
			return;
		if (update == null && string == null)
			return;
		update = string;
		MainFrame.update(this);
	}


}
