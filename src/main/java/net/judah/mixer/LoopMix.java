package net.judah.mixer;

import java.awt.Color;

import javax.swing.JToggleButton;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.util.RTLogger;

public class LoopMix extends MixWidget implements TimeListener, Updateable {
	public static final int BSYNC_UP = Integer.MAX_VALUE;
	public static final int BSYNC_DOWN = 1000000;

	private final Looper looper;
	private final JudahClock clock;
	@Getter private final Loop loop;
	@Getter protected final JToggleButton rec = new JToggleButton("rec");
	@Setter @Getter int bars;
	private int counter = -1;
	
	public LoopMix(Loop l, Looper looper) {
		super(l);
		this.loop = l;
		this.looper = looper;
		this.clock = looper.getClock();
		if (loop == looper.getLoopC())
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

	@Override
	protected Color thisUpdate() {
		if (looper.getSoloTrack().isSolo() && loop == looper.getSoloTrack())
			sync.setBackground(YELLOW);
		else 
			sync.setBackground(null);
		
		if (loop.isRecording() == AudioMode.RUNNING)
			rec.setSelected(true);
		else if (clock.isSync(loop))
			rec.setSelected(true);
		else rec.setSelected(false);

		if (loop == looper.getSoloTrack()) {
			sync.setSelected(looper.getSoloTrack().isSolo());
			sync.setBackground(sync.isSelected() ? YELLOW : null);
		}
		else 
			sync.setSelected(looper.getOnDeck().contains(loop));
		
		if (loop.isRecording() != AudioMode.RUNNING) 
			staticTitle();
		
		mute.setBackground(loop.isOnMute() ? PURPLE : null);
		if (mute.isSelected() != channel.isOnMute())
			mute.setSelected(channel.isOnMute());
		
		Color bg = BLUE;
		if (loop.isRecording() == AudioMode.RUNNING) {
			bg = RED;
			if (bars == BSYNC_DOWN)
				bg = CLOCKSYNC;
		}
		else if (clock.isSync(loop))
			bg = CLOCKSYNC;
		else if (loop.hasRecording() && loop.isPlaying() == AudioMode.STOPPED)
			bg = Color.DARK_GRAY;
		else if (looper.getOnDeck().contains(loop)) 
			bg = ONDECK;
		else if (channel.isOnMute())  // line in/master track 
			bg = Color.BLACK;
		else if (quiet())
			bg = Color.DARK_GRAY;
		else if (loop.isPlaying() == AudioMode.RUNNING && loop.isActive())
			bg = GREEN;

		return bg;
	}

	private void staticTitle() {
		if (loop == looper.getLoopA()) {
			if (clock.getLength() < 10)
				title.setText("-" + clock.getLength() + "-");
			else 
				title.setText("" + clock.getLength());
		}
		else 
			title.setText(" " + channel.getName() + " ");		

	}

	@Override public void update(Notification.Property prop, Object value) {
		if (Notification.Property.BEAT == prop) {
			if (counter < 0) { // not started, display beats until start
				JudahZone.getMixer().getFader(loop).update();
				int countdown = clock.getBeat() % clock.getMeasure() - clock.getMeasure();
				title.setText(countdown + "");
			}
			else if (loop.isRecording() == AudioMode.RUNNING) { 
				// recording, display bars.beats until finish
				int measure = clock.getMeasure();
				StringBuffer sb = new StringBuffer("<html>");
				sb.append(1 + clock.getBeat() % measure).append("/").append(measure);
				if (bars < 100)
					sb.append("<br/>").append("-").append(bars - counter);
				else 
					sb.append("<br/>").append((counter + 1));
				
				title.setText(sb.append("</html>").toString());
			}
			return;
		}
		
		if (Notification.Property.BARS != prop) return;
		if (bars == BSYNC_DOWN) {
			loop.record(false); // bSync
			clock.setLength(counter + 1);
			clock.setBar(0);
			RTLogger.log(this, "BSync ended");
			return;
		}
			
		if (counter < 0) {
			loop.record(true);
			title.setText("Go ! ");
		}
		counter ++;
		if (counter == bars) {
			loop.record(false);
//			clock.listen(loop);
		}
	}
	
	public void setup(int length, int init) {
		bars = length;
		counter = init;
		MainFrame.update(this);
	}

	
}
