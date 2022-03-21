package net.judah.mixer;

import java.awt.Color;
import java.awt.Dimension;

import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.looper.Recorder;
import net.judah.util.Constants;
import net.judah.util.RainbowFader;

public class SyncWidget extends Menu implements TimeListener {

	private final Recorder loop;
	int local;
	static Dimension sz = new Dimension(40, 35);
	
	public SyncWidget(Recorder channel) {
		super(channel);
		loop = channel;
		setIcon(null);
		JudahClock.getInstance().addListener(this);
		setFont(Constants.Gui.FONT13);
		setText("hi.");
		
		setPreferredSize(sz);
		setMinimumSize(sz);
	}

	@Override
	public void setText(String text) {
		if (text.length() < 3)
			text = " " + text;
		super.setText(text);
	}
	
	public void updateLoop() {
		if (loop.hasRecording() && 100 * loop.getTapeCounter().intValue() / loop.getRecording().size() != local) {
			// every 5%
			local = 20 * loop.getTapeCounter().intValue() / loop.getRecording().size() ;
			setBackground(local == 0 ? Color.WHITE : RainbowFader.chaseTheRainbow(local * 5));
			setText(local * 5 + "%");
		}
	}
	
	@Override
	public void update() {
//		else 
//			if (JudahClock.isLoopSync()) {
//			if (loop.isRecording() == AudioMode.RUNNING) 
//				if (!getText().equals(JudahClock.display())) {
//					setText(JudahClock.display());
//				} 
//			else if (!getText().equals("" + JudahClock.getLength())) {
//				setText("" + JudahClock.getLength());
//			}
//		}
//		else if (loop.isRecording() == AudioMode.RUNNING) {
//			if (local != (System.currentTimeMillis() - loop.getRecording().getCreationTime()) / 1000) {
//				local = (int) ((System.currentTimeMillis() - loop.getRecording().getCreationTime()) / 1000);
//				setText(local + " s.");
//			}
//		}
//		else 
//			if (!getText().equals("hmm"))
//				setText("hmm");
//		
	}


//	@Override
//	public void update(int percent) {
//		if (local != percent) {
//			local = percent;
//			setText(local + "%");
//		}
//	}
	
	
	@Override
	public void update(Property prop, Object value) {
//		if (prop == Property.BEAT)
//			update();
	}
}
