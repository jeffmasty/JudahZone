package net.judah.looper.old;

import java.awt.FlowLayout;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.extern.log4j.Log4j;
import net.judah.looper.old.GLoop.Mode;

@SuppressWarnings("serial") @Log4j
public class LoopUI extends JPanel {

	final GLoop loop;
	final JLabel playingLbl, recordingLbl, lengthLbl, recLbl;//, tapeLbl;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	ScheduledFuture<?> polling;

	public LoopUI(GLoop loop) {
		this.loop = loop;
		setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));
		add(new JLabel(loop.getName()));
		playingLbl = new JLabel();
		recordingLbl = new JLabel();
		lengthLbl = new JLabel();
		recLbl = new JLabel();
//		tapeLbl = new JLabel();
		add(playingLbl); add(recordingLbl); add(lengthLbl); add(recLbl);
//		add(tapeLbl);
		update();


	}

	public void update() {
		boolean playing = loop.getPlayback().get() == Mode.RUNNING;
		boolean recording = loop.getRecording().get() == Mode.RUNNING;

		playingLbl.setText(playing ? "playing" : "");
		recordingLbl.setText(recording ? "recording" : "");
//		lengthLbl.setText("length: " + loop.getLoopLength());
//		recLbl.setText("liveRecording: " + loop.liveRecording.size()); // liveRecording.size());
//		String tape = "";
//		if (loop.tape.size() > 0)
//			tape = " length: " + loop.tape.get(0);
//		tapeLbl.setText("Tape: " + loop.tape.size() + tape);

		if (playing || recording) {
				polling();
		} else {
			noPolling();
		}
	}




	public boolean isRunning() {
		return polling != null;
	}

	public void polling() {
		if (isRunning()) return;
		log.info(loop.getName() + " polling started");
		polling = scheduler.scheduleAtFixedRate(new Runnable() {@Override public void run() { update(); }},
				1, 1, TimeUnit.SECONDS);
		scheduler.schedule(
    		new Runnable() {@Override public void run() {polling.cancel(true);}},
    		24, TimeUnit.HOURS);
	}
	public void noPolling() {
		if (!isRunning()) return;
		polling.cancel(true);
		polling = null;
	}





}
