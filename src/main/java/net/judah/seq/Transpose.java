package net.judah.seq;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Integers;
import net.judah.gui.widgets.ModalDialog;
import net.judah.midi.Midi;
import net.judah.seq.Edit.Type;

/** provides a form to enter specific transposition amounts */
public class Transpose {
	
	private final MusicBox view;
	private final Integers steps, tones, octaves;
	
	public Transpose(MusicBox view) {
		
		this.view = view;
		steps = new Integers(view.clock.getSteps() * -1, view.clock.getSteps());
		tones = new Integers(-11, 11);
		octaves = new Integers(-5, 5);
		
		JPanel pnl = new JPanel(new GridLayout(4, 2));
		
		pnl.add(new JLabel("Steps"));  
		pnl.add(steps);
		pnl.add(new JLabel("Octaves")); // +/- 5
		pnl.add(octaves);
		pnl.add(new JLabel("SemiTones")); // +/- 11
		pnl.add(tones);

		pnl.add(new Btn("Ok", e->ok()));
		pnl.add(new Btn("Cancel", e->ModalDialog.getInstance().setVisible(false)));
		pnl = Gui.wrap(pnl);
		pnl.setName("Transpose");
		new ModalDialog(pnl, new Dimension(220, 160), MainFrame.getKnobMode());
	}

	private void ok() {
		ModalDialog.getInstance().setVisible(false);
		Edit e = new Edit(Type.TRANS, new ArrayList<MidiPair>(view.selected));
		e.setDestination(new Prototype((Integer)steps.getSelectedItem(), 
				(Integer)octaves.getSelectedItem() * 12 + (Integer)tones.getSelectedItem()));
		view.push(e);
	}

	/**
	 * @param in source note (off is null for drums)
	 * @param destination x = +/-ticks,   y = +/-data1
	 * @return new midi
	 */
	public static MidiPair compute(MidiPair in, Prototype destination, MidiTrack t) {
		if (in.getOn().getMessage() instanceof ShortMessage == false)
			return in;
		MidiEvent on = trans((ShortMessage)in.getOn().getMessage(), in.getOn().getTick(), destination, t);
		MidiEvent off = null;
		if (in.getOff() != null)
			off = trans((ShortMessage)in.getOff().getMessage(), in.getOff().getTick(), destination, t);
		return new MidiPair(on, off);
	}
	
	private static MidiEvent trans(ShortMessage source, long sourceTick, Prototype destination, MidiTrack t) {
		long window = t.getWindow();
		long start = t.getCurrent() * t.getBarTicks();
		long tick = sourceTick + destination.getTick() * t.getStepTicks();
		if (tick < start) tick += window;
		if (tick >= start + window) tick -= window;
		int data1 = source.getData1() + destination.getData1();
		if (data1 < 0) data1 += 127;
		if (data1 > 127) data1 -= 127;
		return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick);
	}


}
