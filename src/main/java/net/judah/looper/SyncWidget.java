package net.judah.looper;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.AudioMode;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.util.Constants;
import net.judah.util.RainbowFader;

public class SyncWidget extends JLabel implements TimeListener {

	public static enum SelectType {
		ERASE, SYNC
	};

	@Setter @Getter int bars;
	private final Loop loop;
	int local;
	static Dimension sz = new Dimension(25, 35);
	JudahClock clock;
	private int counter = -1;

	SyncWidget(Loop channel) {
		super("", JLabel.CENTER);
		loop = channel;
		setIcon(null);
		setFont(Constants.Gui.BOLD13);
		
		setPreferredSize(sz);
		setMinimumSize(sz);

	}

	public void updateLoop() {
		if (loop.isDirty())
			// TODO update sensitive to length of loop
			if (loop.hasRecording() && 100 * loop.getTapeCounter().intValue() / loop.getRecording().size() != local) {
				// every 5%
				local = 20 * loop.getTapeCounter().intValue() / loop.getRecording().size() ;
				setBackground(local == 0 ? Color.WHITE : RainbowFader.chaseTheRainbow(local * 5));
				setText(local * 5 + "");
			}
	}
	
//	@Override
	public void update() {
		if (loop.hasRecording()) return;
		
		if (loop.isRecording() == AudioMode.RUNNING) {
			StringBuffer sb = new StringBuffer("<html>").append(JudahClock.getBeat());
			sb.append("<br>").append(JudahClock.getLength() * JudahClock.getInstance().getMeasure()).append("</html>");
			setText(sb.toString());
		}
//		else if (clock.getSynchronized() != null) { // if onDeck
//		int countdown = 
//				JudahClock.getBeat() % clock.getMeasure() - clock.getMeaTsure();
//			setText(countdown + " !");
//		}
		else if (loop == JudahZone.getLooper().getLoopA()) {
				if (JudahClock.isLoopSync()) {
					// display sync measures
					if (JudahClock.getLength() > 9)
						setText(JudahClock.getLength() + "");
					else 
						setText("-" + JudahClock.getLength() + "-");
					// setBackground(Color.WHITE);
				}
				else {
					setText("free ");
				}
			}
		else 			
			setText(loop.getName());
		
	}

	@Override public void update(Notification.Property prop, Object value) {
		if (Notification.Property.BEAT == prop) {
			if (counter < 0) { // not started, display beats until start
				loop.getFader().background();
				int countdown = JudahClock.getBeat() % clock.getMeasure() - clock.getMeasure();
				setText(countdown + "");
			}
			else if (loop.isRecording() == AudioMode.RUNNING) { 
				// recording, display bars.beats until finish
				StringBuffer sb = new StringBuffer("<html>").append(JudahClock.getBeat() % clock.getMeasure() + 1);
				sb.append("<br/>").append(bars - counter).append("</html>");
				setText(sb.toString());
			}
		}
		
		if (Notification.Property.BARS != prop) return;
		
		if (counter < 0) {
			JudahClock.setBeat(0);
			loop.record(true);
			setText("Go !");
		}

		counter ++;
		if (counter == bars) {
			new Thread(() -> {
				loop.record(false);
				clock.removeListener(this);
				clock.listen(loop);
			}).start();
		}
	}

	public void syncUp() {
		setBars(JudahClock.getLength());
		counter = -1;
		clock = JudahClock.getInstance();
		clock.addListener(this);
		loop.getFader().background();
	}

}
