package net.judah.widgets;

import java.awt.Dimension;

import javax.swing.JLabel;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.looper.Loop;
import net.judah.midi.JudahClock;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class SyncWidget extends JLabel implements TimeListener {

	@Setter @Getter int bars;
	private final Loop loop;
	int local;
	static Dimension sz = new Dimension(25, 35);
	private int counter = -1;
	private final JudahClock clock;

	public SyncWidget(Loop channel, JudahClock clock) {
		super("", JLabel.CENTER);
		this.loop = channel;
		this.clock = clock;
		setIcon(null);
		setFont(Constants.Gui.BOLD13);
		setPreferredSize(sz);
		setMinimumSize(sz);

	}

//	public void updateLoop() {
//		if (loop.isActive())
//			// update sensitive to length of loop
//			if (loop.hasRecording() && 100 * loop.getTapeCounter().intValue() / loop.getRecording().size() != local) {
//				// every 5%
//				local = 20 * loop.getTapeCounter().intValue() / loop.getRecording().size() ;
//				setBackground(local == 0 ? Color.WHITE : RainbowFader.chaseTheRainbow(local * 5));
//				setText(local * 5 + "");
//			}
//	}
	
	public void update() {
		if (loop.hasRecording()) {
			setText(loop.getName());
		}
		else if (loop.isRecording() == AudioMode.RUNNING) {
			StringBuffer sb = new StringBuffer("<html>").append(clock.getBeat());
			sb.append("<br>").append(clock.getLength() * clock.getMeasure()).append("</html>");
			setText(sb.toString());
		}
		else if (loop == JudahZone.getLooper().getLoopA()) {
				// display sync measures
				if (clock.getLength() > 9)
					setText(clock.getLength() + "");
				else 
					setText("-" + clock.getLength() + "-");
			}
		else 			
			setText(loop.getName());
		
	}

	@Override public void update(Notification.Property prop, Object value) {
		if (Notification.Property.BEAT == prop) {
			if (counter < 0) { // not started, display beats until start
				JudahZone.getMixer().getFader(loop).update();
				int countdown = clock.getBeat() % clock.getMeasure() - clock.getMeasure();
				setText(countdown + "");
			}
			else if (loop.isRecording() == AudioMode.RUNNING) { 
				// recording, display bars.beats until finish
				StringBuffer sb = new StringBuffer("<html>");
				sb.append(1 + clock.getBeat() % clock.getMeasure());
				sb.append("<br/>").append(bars - counter).append("</html>");
				setText(sb.toString());
			}
			return;
		}
		
		if (Notification.Property.BARS != prop) return;
		if (bars == Integer.MAX_VALUE && counter == Integer.MIN_VALUE) {
			endRecord(); // bSync
			RTLogger.log(this, "Recording ended");
			return;
		}
			
		if (counter < 0) {
			loop.record(true);
			setText("Go ! ");
		}
		counter ++;
		if (counter == bars) {
			endRecord();
			clock.listen(loop);
		}

	}
	
	private void endRecord() {
		loop.record(false);
		clock.removeListener(this);
	}

	public void syncDown() {
		clock.removeListener(this);
		JudahZone.getMixer().getFader(loop).update();
	}
	
	public void syncUp(int init) {
		setBars(clock.getLength());
		counter = init;
		clock.addListener(this);
		JudahZone.getMixer().getFader(loop).update();
	}

	public void bSync(int counter) {
		bars = Integer.MAX_VALUE;
		this.counter = counter;
		clock.addListener(this);
		JudahZone.getMixer().getFader(loop).update();
	}
	
	public void syncUp() {
		syncUp(-1);
	}

}
