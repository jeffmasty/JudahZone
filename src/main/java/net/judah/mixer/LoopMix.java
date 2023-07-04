package net.judah.mixer;

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

	private final Looper looper;
	@Getter private final Loop loop;
	@Getter private final JToggleButton rec = new JToggleButton("rec");
	private String update = "";
	
	public LoopMix(Loop l, Looper looper) {
		super(l);
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
			sync.setBackground(YELLOW);
		else 
			sync.setBackground(null);
		
		if (loop.isRecording())
			rec.setSelected(true);
		else if (looper.isSync(loop))
			rec.setSelected(true);
		else rec.setSelected(false);
		if (loop.getType() == Type.FREE) {
			if (! rec.getText().equals("free")) rec.setText("free");
		} else if (! rec.getText().equals("rec")) rec.setText("rec");

		if (loop == looper.getSoloTrack()) {
			sync.setSelected(looper.getSoloTrack().isSolo());
			sync.setBackground(sync.isSelected() ? YELLOW : null);
		}
		else 
			sync.setSelected(looper.getOnDeck().contains(loop));
		
		if (looper.isSync(loop) && update != null) {
			if (title.getText().equals(update) == false)
				title.setText(update);
		}
		else
			staticTitle();
		
		mute.setBackground(loop.isOnMute() ? PURPLE : null);
		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());
		
		Color bg = BLUE;
		if (loop.isRecording()) {
			bg = RED;
			// if (bars == BSYNC_DOWN)
			// TODO	bg = CLOCKSYNC;
		}
		else if (looper.isSync(loop))
			bg = CLOCKSYNC;
		else if (looper.getOnDeck().contains(loop)) 
			bg = ONDECK;
		else if (channel.isOnMute())  // line in/master track 
			bg = Color.BLACK;
		else if (quiet())
			bg = Color.GRAY;
		else if (loop.isPlaying())
			bg = GREEN;
		return bg;
	}

	private void staticTitle() {
		String text;
		if (loop == looper.getLoopA()) 
			text = JudahClock.getLength() < 10 ? 
				"-" + JudahClock.getLength() + "-" :
				"" + JudahClock.getLength();
		else 
			text = " " + channel.getName() + " ";
		if (!title.getText().equals(text))
			title.setText(text);
	}
	
	public void setUpdate(String string) {
		update = string;
		MainFrame.update(this);
	}

}
