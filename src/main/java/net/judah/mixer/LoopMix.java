
package net.judah.mixer;

import static judahzone.gui.Gui.font;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JSlider;
import javax.swing.JToggleButton;

import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import judahzone.widgets.RainbowFader;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.LoopType;
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
	static final Dimension SWEEPER = new Dimension(16, RainbowFader.FADER_SIZE.height);

	/** bars until recording stops */
	private final Loop loop;
	private final Looper looper;
	private final JToggleButton rec = new JToggleButton("rec");
	private String text;
	private final JSlider sweeper = new JSlider(JSlider.VERTICAL);

	public LoopMix(Loop l, Looper looper) {
		super(l);

		this.loop = l;
		this.looper = looper;
		if (looper.getType() == LoopType.FREE)
			rec.setText("free");
		rec.addActionListener(e -> looper.trigger(loop));
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

		// custom loop feedback:
		bottom.add(Gui.resize(sweeper, SWEEPER));
	}

	@Override protected Color thisUpdate() {
		if (text != null && loop.getStopwatch() >= 0) {
			if (! title.getText().equals(text))
				title.setText(text);
		}
		else
			defaultText();

		if (loop == looper.getSoloTrack() && looper.getSoloTrack().isSolo() )
			sync.setBackground(CLOCKSYNC);
		else
			sync.setBackground(null);

		if (loop.isRecording())
			rec.setSelected(true);
		else if (loop.isTimer())
			rec.setSelected(true);
		else rec.setSelected(false);

		if (looper.getType() == LoopType.FREE) {
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



		if (loop.isPlaying())
			sweep();
		else if (sweeper.isVisible())
			sweeper.setVisible(false);

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

	private void defaultText() {
		String text;
		if (loop == looper.getPrimary() && looper.getType() != LoopType.FREE) {
			int val = looper.getMeasures();
			text = val < 10 ? "-" + val + "-" : "" + val;
		}
		else if (loop == looper.getFirst()) {
			int val = JudahClock.getLength();
			text = val < 10 ? "-" + val + "-" : "" + val;
		}
		else {
			text = channel.getName();
			if (looper.getPrimary() != null && looper.getType() != LoopType.FREE && loop.getFactor() > 1)
				text = (int)loop.getFactor() + "x"; // duplications
		}

		if (!title.getText().equals(text))
			title.setText(text);
	}

	public void clear() {
		text = null;
		MainFrame.update(this);
	}

	/** display  currentBar/total */
	public void measureFeedback() {
		int count = loop.getStopwatch();
		count++;
		String txt;
		switch (looper.getType()) {
			case BSYNC:
				txt = "+" + count;
				break;
			case FREE:
				txt = " ⏺ ";
				break;
			default:
				txt = "<html><u>" + count + "<br/></u>" + (int)(looper.getMeasures() * loop.getFactor()) + "</html>";
		}
		setText(txt);
	}

	public void setText(String string) {
		if (text != null && text.equals(string))
			return;
		if (text == null && string == null)
			return;
		text = string;
		MainFrame.update(this);
	}

	public void sweep() {
		sweeper.setValue((int)(100f - 100f * (loop.getTapeCounter().get() / (float)loop.getLength())));
		if (!sweeper.isVisible()) {
			sweeper.setVisible(true);
			bottom.doLayout();
		}
	}

}
