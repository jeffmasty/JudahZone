package net.judah.tracks;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.util.Slider;

public class MidiFeedback extends JPanel {

	private final JLabel resolution = new JLabel();
	private final JLabel beat = new JLabel();
	private final JLabel tempo = new JLabel();
	Sequencer seq;
	int bars;
	int barCount;
	int tick;
	
	Slider current = new Slider((e) -> {});
	
	public MidiFeedback() {
		
		JPanel beatPnl = new JPanel();
		beatPnl.setLayout(new BoxLayout(beatPnl, BoxLayout.PAGE_AXIS));
		beatPnl.add(beat);
		beatPnl.add(resolution);
		add(beatPnl);
		add(current);
		add(tempo);
	}
	
	public void setSequence(Sequencer sequencer, int bars) {
		Sequence sequence = sequencer.getSequence();
		this.bars = bars;
		barCount = 0;
		tick = -1;
		beat.setText(disp());
		resolution.setText("[" + sequence.getResolution() + ":" + sequence.getTickLength() + "]");
		this.seq = sequencer;
	}
	
	public void barCount(int barCount) {
		this.barCount = barCount;
		tick = 0;
		beat.setText(disp());
		if (barCount == 1) current.setValue(0);
	}
	
	public void tick() {
		tick ++;
		beat.setText(disp());
		tempo.setText(seq.getTempoInBPM() + "[" + seq.getTempoFactor() + "]");
		int pos = (int) (100 * (seq.getTickPosition() / (float)seq.getTickLength()));
		current.setValue(pos);
	}

	private String disp() {
		return barCount + "." + (tick + 1) + "/" + bars;
	}
	

}
