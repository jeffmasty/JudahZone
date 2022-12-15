package net.judah.mixer;

import java.awt.Color;

import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.api.AudioMode;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;

public class LoopFader extends ChannelFader {

	private final Loop loop;
	private final Looper looper;
	private final JudahClock clock;
	@Getter protected final JToggleButton rec = new JToggleButton("rec");
	
	public LoopFader(Loop loop, Looper looper) {
		super(loop);
		this.loop = loop;
		this.looper = looper;
		this.clock = loop.getClock();
		if (loop == looper.getLoopC())
			rec.setText("free");
		rec.addActionListener(e -> loop.trigger());
		mute.addActionListener(e -> mute());
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

	@Override
	protected Color thisUpdate() {
		if (looper.getSoloTrack().isSolo() && loop == looper.getSoloTrack())
			sync.setBackground(YELLOW);
		else 
			sync.setBackground(null);
		
//		loop.getSync().update();
		if (loop.isRecording() == AudioMode.RUNNING)
			rec.setSelected(true);
		else if (clock.getListeners().contains(loop.getSync()))
			rec.setSelected(true);
		else rec.setSelected(false);

		if (loop == looper.getSoloTrack()) {
			sync.setSelected(looper.getSoloTrack().isSolo());
			sync.setBackground(sync.isSelected() ? YELLOW : null);
		}
		else 
			sync.setSelected(looper.getOnDeck().contains(loop));
		
		if (loop == looper.getLoopA()) {
			if (clock.getLength() < 10)
				title.setText("-" + clock.getLength() + "-");
			else 
				title.setText("" + clock.getLength());
		}
		else 
			title.setText(" " + channel.getName() + " ");		
		mute.setBackground(loop.isOnMute() ? PURPLE : null);
		
		Color bg = BLUE;
		if (loop.isRecording() == AudioMode.RUNNING) 
			bg = RED;
		else if (loop.getClock().getListeners().contains(loop.getSync()))
			bg = YELLOW;
		else if (loop.hasRecording() && loop.isPlaying() == AudioMode.STOPPED)
			bg = Color.DARK_GRAY;
		else if (looper.getOnDeck().contains(loop)) 
			bg = PINK;
		else if (channel.isOnMute())  // line in/master track 
			bg = Color.BLACK;
		else if (channel.getGain().getVol() < 5)
			bg = Color.DARK_GRAY;
		else if (loop.isPlaying() == AudioMode.RUNNING && loop.isActive())
			bg = GREEN;

		return bg;
	}

}
