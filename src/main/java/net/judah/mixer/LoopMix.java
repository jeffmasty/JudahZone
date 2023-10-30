package net.judah.mixer;

import static net.judah.gui.Gui.font;

import java.awt.Color;

import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.api.PlayAudio.Type;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;

public class LoopMix extends MixWidget implements Updateable {
	public static final Color CLOCKSYNC = YELLOW;
	public static final Color ONDECK = PINK;
	public static final Color PLAIN = BLUE;
	public static final Color PLAYING = GREEN;
	public static final Color RECORDING = RED;
	public static final Color MUTED = Color.BLACK;
	public static final Color QUIET = Color.GRAY;
	
	@Getter private final Loop loop;
	private final Looper looper;
	private final JToggleButton rec = new JToggleButton("rec");
	private String update = "";
	
	public LoopMix(Loop l, Looper looper) {
		super(l, looper);
		this.loop = l;
		this.looper = looper;
		if (loop.getType() == Type.FREE)
			rec.setText("free");
		rec.addActionListener(e -> loop.trigger());
		mute.addActionListener(e -> channel.setOnMute(!channel.isOnMute()));
		if (loop == looper.getSoloTrack()) {
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
		if (looper.getSoloTrack().isSolo() && loop == looper.getSoloTrack())
			sync.setBackground(CLOCKSYNC);
		else 
			sync.setBackground(null);
		
		if (loop.isRecording())
			rec.setSelected(true);
		else if (looper.getCountdown() == loop)
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
		if (update != null && looper.getCountdown() == loop) {
			if (title.getText().equals(update) == false)
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
		else if (looper.getCountdown() == loop)
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
		if (loop == looper.getLoopA()) 
			text = JudahClock.getLength() < 10 ? 
				"-" + JudahClock.getLength() + "-" :
				"" + JudahClock.getLength();
		else 
			text = channel.getName();
		if (!title.getText().equals(text))
			title.setText(text);
	}
	
	public void setUpdate(String string) {
		if (string == null && update == null)
			return;
		if (update != null && update.equals(string))
			return;
		update = string;
		MainFrame.update(this);
	}

}
