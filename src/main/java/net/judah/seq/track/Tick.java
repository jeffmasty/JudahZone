package net.judah.seq.track;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.util.Constants;

public class Tick extends JPanel {
	private static final Border GREEN = BorderFactory.createEtchedBorder(Pastels.GREEN, Pastels.MY_GRAY);

	private MidiTrack track;
	JComboBox<Integer> frame = new JComboBox<Integer>();
	JComboBox<Integer> beat = new JComboBox<Integer>();
	JComboBox<Integer> step = new JComboBox<Integer>();
	JTextField ticks = new JTextField(6);
	private ActionListener listen = e->recalc();

	public Tick(MidiTrack t) {
		this();
		setTrack(t);
	}

	public Tick() {
		setLayout(new GridLayout(2, 4));
		frame.addActionListener(listen);
		beat.addActionListener(listen);
		step.addActionListener(listen);
		add(new JLabel("Frame"));
		add(new JLabel("Beat"));
		add(new JLabel("Step"));
		add(new JLabel("Tick"));

		add(frame);
		add(beat);
		add(step);
		add(ticks);
		ticks.setHorizontalAlignment(JTextField.CENTER);
		ticks.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {quantizable();}
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setBackground(enabled ? CCEdit.ENABLED : CCEdit.DISABLED);
		frame.setEnabled(enabled);
		beat.setEnabled(enabled);
		step.setEnabled(enabled);
		ticks.setEnabled(enabled);
	}

	public long getTick() throws NumberFormatException {
		return Long.parseLong(ticks.getText());
	}

	private void recalc() {
		if (track == null) return;
		long note = track.getClock().getTimeSig().div * track.getStepTicks();
		long target = frame.getSelectedIndex() * track.getWindow() +
				beat.getSelectedIndex() *  note +
				step.getSelectedIndex() * track.getStepTicks();
		quantizable(target);
	}

	private void quantizable() {
		try {
			Long tick = Long.parseLong(ticks.getText());
			quantizable(tick);
		} catch (NumberFormatException nfe) {
			ticks.setBorder(null);
		}
	}

	public void quantizable(long tick) {
		long note = track.getClock().getTimeSig().div * track.getStepTicks();
		long remainder = tick % track.getWindow() % note;

		change(frame, (int) (tick / track.getWindow()));
		change(beat, (int) ((tick % track.getWindow()) / note));
		change(step, (int) (remainder / track.getStepTicks()));

		if (false == ticks.getText().equals("" + tick))
			ticks.setText("" + tick);

		boolean quantized = remainder % track.getStepTicks() == 0;
		ticks.setBorder(quantized ? GREEN : null );
		ticks.setFont(quantized ? Gui.BOLD12 : Gui.FONT12);
	}

	void change(JComboBox<Integer> it, int idx) {
		if (it.getSelectedIndex() == idx)
			return;
		it.removeActionListener(listen);
		it.setSelectedIndex(idx);
		it.addActionListener(listen);
	}

	void reload(JComboBox<Integer> it, int start, int total, int selected) {
		it.removeActionListener(listen);
		it.removeAll();
		for (int i = start; i < start + total; i++)
			it.addItem(i);
		it.setSelectedIndex(selected);
		it.addActionListener(listen);
	}

	void setTrack(MidiTrack t) {
		track = t;
		if (frame.getItemCount() != track.getFrames())
			reload(frame, 0, track.getFrames(), track.getFrame());

		int beats = 2 * track.getClock().getTimeSig().beats;
		if (beat.getItemCount() != beats)
			reload(beat, 1, beats, 0);

		int div = track.getClock().getTimeSig().div;
		if (step.getItemCount() != div)
			reload(step, 1, div, 0);
	}

	public void beatKnob(int value) {
		beat.setSelectedIndex(Constants.ratio(value, beat.getItemCount() - 1));
	}

	public void stepKnob(int value) {
		step.setSelectedIndex(Constants.ratio(value, step.getItemCount() - 1));
	}

	public void frameKnob(int value) {
		frame.setSelectedIndex(Constants.ratio(value, track.getFrames() - 1));
	}

}
