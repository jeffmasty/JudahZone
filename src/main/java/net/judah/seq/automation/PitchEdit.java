package net.judah.seq.automation;

import java.awt.FlowLayout;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiConstants;
import net.judah.seq.Prototype;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.MidiTrack;
import net.judah.synth.taco.TacoSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// TODO
class PitchEdit extends AutoBox {

	MidiEvent existing;
	private Edit undo;

	private final Btn create = new Btn("New", e->create());
	private final Btn change = new Btn("Change", e->change());
	private final Btn delete = new Btn("Delete", e->delete());
	private final Btn exe = new Btn("Exe", e->exe());

	private final Box top = new Box(BoxLayout.PAGE_AXIS);
	private final Box algo = new Box(BoxLayout.PAGE_AXIS);
	private final JPanel checkbox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
	private final Pitch pitch = new Pitch(false);
	private final Pitch pitch2 = new Pitch(true);
	private final Tick steps = new Tick();
	private final Tick steps2 = new Tick();
	private final JCheckBox automation = new JCheckBox("Enabled");
	private final JTextField messages = new JTextField("5", 3);


	protected PitchEdit() {
		super(BoxLayout.LINE_AXIS);

		Box btns = new Box(BoxLayout.LINE_AXIS);
		btns.add(Box.createHorizontalStrut(15));
		btns.add(new JLabel("Pitch Bend"));
		btns.add(Box.createHorizontalStrut(25));
		btns.add(create);
		btns.add(change);
		btns.add(delete);
		btns.add(exe);

		top.add(btns);
		top.add(pitch);
		top.add(steps);

		JLabel auto = new JLabel("Automation");
		auto.setFont(Gui.BOLD12);
		checkbox.add(auto);
		checkbox.add(automation);
		automation.addChangeListener(l->enableAutomation(automation.isSelected()));

		checkbox.add(new JLabel(" "));
		checkbox.add(Gui.resize(messages, Size.COMBO_SIZE));
		checkbox.add(new JLabel(" Steps"));
		algo.add(checkbox);
		algo.add(pitch2);
		algo.add(steps2);
		algo.setBorder(Gui.SUBTLE);
		Box inner = new Box(BoxLayout.PAGE_AXIS);
		inner.add(Gui.wrap(top));
		inner.add(Gui.wrap(algo));

		add(Box.createHorizontalStrut(12));
		add(inner);
		add(Box.createHorizontalStrut(12));
		enableAutomation(false);
	}

 	private void enableAutomation(boolean enable) {
 		steps2.setEnabled(enable);
 		pitch2.setEnabled(enable);
 		messages.setEnabled(enable);
 		checkbox.setBackground(enable ? ENABLED : DISABLED);
 		algo.setBackground(enable ? ENABLED : DISABLED);
	}

	public PitchEdit edit(MidiEvent e) {
		existing = e;
		// TODO populate
		enableAutomation(false);
		return this;
	}

	@Override protected void setTrack(MidiTrack t) {
		track = t;
	}

	@Override public PitchEdit init(long tick) {
		existing = null;
		// TODO populate
		enableAutomation(true);
		return this;
	}

	@Override protected void pad1() {

	}

	@Override protected void pad2() {

	}

	@Override protected boolean doKnob(int idx, int value) {
		// TODO Auto-generated method stub
		return false;
	}

	void create() {
		int data1 = -1;
		try {
			ShortMessage msg = build();
			MidiEvent evt = new MidiEvent(msg, steps.getTick());
			existing = evt;
	 		undo = new Edit(Type.NEW, evt);
	 		if (automation.isSelected()) {
	 			// generate and add messages
	 			int count;
	 			try {
	 				count = Integer.parseInt(messages.getText());
	 			} catch (NumberFormatException nfe) {
	 				RTLogger.warn(this, messages.getText() + ": " + nfe.getMessage());
					return;
				}
	 			double interval = (steps2.getTick() - steps.getTick()) / (double)(count - 1);
	 			float mod = (pitch2.get() - pitch.get()) / (float) (count - 1);
	 			for (int step = 1; step < count; step++) {
	 				int data2 = pitch.get() + Math.round(step * mod);
	 				long ticker = steps.getTick() + Math.round(step * interval);
	 				ShortMessage unit = new ShortMessage(Midi.PITCH_BEND, track.getCh(), data1, data2);
	 				undo.getNotes().add(new MidiEvent(unit, ticker));
	 			}
			}
	 		track.getEditor().push(undo);
			change.setEnabled(true);
	 		delete.setEnabled(true);
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	int pitchToVal() {
		float factor = TacoSynth.bendFactor((ShortMessage)existing.getMessage(), 2);
		// -2 to 2?
		// convert to 127 based around 64
		return (int) ((factor + 2) * 32);
	}

	ShortMessage valToPitch(int val) {
//		float factor = bendFactor(m, modSemitones);
//		for (Voice v : notes.voices)
//			v.bend(factor);

		return null;
	}

	void change() {
		if (existing == null) {
			create();
			return;
		}
		try {
			ShortMessage target = build();
			Edit mod = new Edit(Type.MOD, existing, new MidiEvent(target, steps.getTick()));
			if (automation.isSelected()) {
				mod.setDestination(new Prototype(pitch2.get(), steps2.getTick()));
				if (undo != null)
					; // TODO
			}
			track.getEditor().push(mod);
		}
		catch (InvalidMidiDataException ie) {
			RTLogger.warn(this, ie);
		}
	}

	void delete() {
		if (undo != null) {
			Edit del = new Edit(Type.DEL, undo.getNotes());
			track.getEditor().push(del);
			undo = null;
			return;
		}
		if (existing == null)
			return;
		Edit edit = new Edit(Type.DEL, existing, null);
		if (automation.isSelected()) {
			// TODO
		}
		delete.setEnabled(false);
		track.getEditor().push(edit);
	}

	void exe() {
		try {
			track.send(build(), 0);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

	private ShortMessage build() throws InvalidMidiDataException {
		int data1 = -1; // TODO
		return new ShortMessage(Midi.PITCH_BEND, track.getCh(), data1, pitch.get());
	}


	protected class Pitch extends JPanel {

		private JLabel display = new JLabel("  0");
		private final JSlider pitch = new JSlider(0, 128);

		Pitch(boolean automation) {
			super(new FlowLayout(FlowLayout.CENTER, 3, 3));
			Gui.resize(pitch, WIDE);
			add(new JLabel(automation ? "End Pitch: " : "Pitch: "));
			add(pitch);
			add(display);
			setEnabled(true);
			pitch.setMajorTickSpacing(32);
			pitch.setMinorTickSpacing(16);
			pitch.setPaintTicks(true);
			pitch.addChangeListener(l->text(pitch.getValue()));
			pitch.setValue(MidiConstants.CUTOFF + 1);
		}

		public void knob(int value) {
			set(Constants.ratio(value, 127));
		}

		@Override public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			setBackground(enabled ? ENABLED: DISABLED);
		}

		void set(int val) {
			pitch.setValue(val);
			text(val);
		}
		int get() {
			return pitch.getValue();
		}

		public void text(int val) {
			String out = " ";
//			if (val < 100) out += " ";
//			if (val < 10) out += " ";
//			out += val;
			display.setText(out);
		}


	}



}

