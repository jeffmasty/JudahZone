package net.judah.seq;

import static net.judah.JudahZone.getClock;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.ModalDialog;
import net.judah.gui.widgets.Slider;
import net.judah.seq.Edit.Type;
import net.judah.util.RTLogger;

public class Duration { // TODO TimeListener TimeSig
	private static final int WIDTH = 500;
	private enum Input {TICKS, STEPS, END, SLIDER};
	private final MusicBox view;
	private final long on;
	private int stpz;
	private long durr, off;
	private MidiPair init;
	private final int measure = getClock().getSteps();
	private long quanta;
	
	private Slider slider;
	private JTextField ticks, steps, end;
	
	private class Durr extends JTextField {
		private final Input type;
		
		Durr(long value, Input t) {
			super(4);
			type = t;
			setText("" + value);
			addActionListener(e->Duration.this.update(type, getText()));
		}
	}
	
	public Duration(MusicBox box) {
		this.view = box;
		if (view.getSelected().isEmpty()) {
			RTLogger.log(this, "Nothing selected");
			on = -1;
			return;
		}
		init = view.getSelected().get(0);
		on = init.getOn().getTick();
		quanta = view.track.getStepTicks();
		off = init.getOff() == null ? Integer.MAX_VALUE : init.getOff().getTick();
		compute();
		
		ticks = new Durr(durr, Input.TICKS);
		steps = new Durr(stpz, Input.STEPS);
		end = new Durr(off, Input.END);

		slider = new Slider(0, measure * 2, null);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(measure / view.clock.getTimeSig().beats);
		slider.setMinorTickSpacing(1);
		slider.setSnapToTicks(true);
		slider.setMinimumSize(new Dimension(WIDTH / 2, 50));
		slider.setPreferredSize(new Dimension(  (WIDTH / 3) * 2   , 60));

		slider.setValue(stpz);
		slider.addChangeListener(e->update(Input.SLIDER, slider.getValue()));
		
		JPanel top = new JPanel();
		top.add(new JLabel("steps"));
		top.add(steps);
		top.add(new JLabel("ticks"));
		top.add(ticks);
		top.add(new JLabel(on + " to"));
		top.add(end);
		
		JPanel pnl = new JPanel();
		pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));
		pnl.add(top);
		pnl.add(Gui.wrap(slider));
		pnl.add(Gui.wrap(
				new Btn("Ok", e->ok()), 
				new Btn("Cancel", e->ModalDialog.getInstance().setVisible(false))));
		pnl.setName("Duration");
		new ModalDialog(Gui.wrap(pnl), new Dimension(400, 190), MainFrame.getKnobMode());
	}

	private void compute() {
		durr = off - on;
		stpz = (int) (durr / quanta);
		if (durr % quanta > 0)
			stpz++;
	}

	private void ok() {
		ModalDialog.getInstance().setVisible(false);
		Edit e = new Edit(Type.LENGTH, new ArrayList<MidiPair>(view.selected));
		e.setDestination(new Prototype(0, Long.parseLong(ticks.getText())));
		view.push(e);
	}

	void update(Input type, String txt) {
		try {
			update(type, Long.parseLong(txt));
		} catch (Throwable t) {
			RTLogger.warn(this, txt + ": " + t.getMessage());
		}
	}
	
	void update(Input type, long value) {
		if (value < 1) return;
		
		switch(type) {
			case TICKS: off = on + value; break;
			case SLIDER:
			case STEPS: off = on + value * quanta; break;
			case END: if (value > on) off = value; break; 
		}
		compute();
		
		if (slider.getValue() != stpz)
			slider.setValue(stpz);
		if (false == ticks.getText().equals(Long.toString(durr)))
			ticks.setText(Long.toString(durr));
		if (false == steps.getText().equals(Integer.toString(stpz)))
			steps.setText(Integer.toString(stpz));
		if (false == end.getText().equals(Long.toString(off)))
			end.setText(Long.toString(off));
		
	}

	
//	/**
//	 * @param in source note (off is null for drums)
//	 * @param destination x = +/-ticks,   y = +/-data1
//	 * @return new midi
//	 */
//	public static MidiPair compute(MidiPair in, Prototype destination, MidiTrack t) {
//		if (in.getOn().getMessage() instanceof ShortMessage == false)
//			return in;
//		MidiEvent on = trans((ShortMessage)in.getOn().getMessage(), in.getOn().getTick(), destination, t);
//		MidiEvent off = null;
//		if (in.getOff() != null)
//			off = trans((ShortMessage)in.getOff().getMessage(), in.getOff().getTick(), destination, t);
//		return new MidiPair(on, off);
//	}
//	
//	private static MidiEvent trans(ShortMessage source, long sourceTick, Prototype destination, MidiTrack t) {
//		long window = t.getWindow();
//		long start = t.getFrame() * t.getWindow();
//		long tick = sourceTick + destination.getTick() * t.getStepTicks();
//		if (tick < start) tick += window;
//		if (tick >= start + window) tick -= window;
//		int data1 = source.getData1() + destination.getData1();
//		if (data1 < 0) data1 += 127;
//		if (data1 > 127) data1 -= 127;
//		return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick);
//	}



}
