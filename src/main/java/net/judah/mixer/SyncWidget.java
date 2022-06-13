package net.judah.mixer;

import java.awt.Color;
import java.awt.Dimension;

import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.looper.Loop;
import net.judah.util.Constants;
import net.judah.util.RainbowFader;

public class SyncWidget extends Menu implements TimeListener {

	private final Loop loop;
	int local;
	static Dimension sz = new Dimension(40, 35);
	
	public SyncWidget(Loop channel) {
		super(channel);
		loop = channel;
		setIcon(null);
		setFont(Constants.Gui.BOLD13);
		setText(loop.getName() + " ");
		
		setPreferredSize(sz);
		setMinimumSize(sz);
		JudahClock.getInstance().addListener(this);

	}

	@Override
	public void setText(String text) {
		if (text.length() < 3)
			text = " " + text;
		super.setText(text);
	}
	
	public void updateLoop() {
		// TODO update sensitive to length of loop
		if (loop.hasRecording() && 100 * loop.getTapeCounter().intValue() / loop.getRecording().size() != local) {
			// every 5%
			local = 20 * loop.getTapeCounter().intValue() / loop.getRecording().size() ;
			setBackground(local == 0 ? Color.WHITE : RainbowFader.chaseTheRainbow(local * 5));
			setText(local * 5 + "%");
		}
	}
	
	@Override
	public void update() {
		
		if (loop.isPlaying() != AudioMode.RUNNING && !loop.hasRecording()) 
			if (loop == JudahZone.getLooper().getLoopA()) {
				if (JudahClock.isLoopSync()) {
					// display sync measures
					setText("-- " + JudahClock.getLength() + " --");
					setBackground(Color.WHITE);
				}
				else {
					setText("free ");
				}
			}
			else 				
				setText("");
		
	}

	@Override
	public void update(Notification.Property prop, Object value) {
//		if (prop == Property.BEAT)
//			update();
	}
}
