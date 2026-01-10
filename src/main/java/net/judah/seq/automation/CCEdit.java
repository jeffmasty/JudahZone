package net.judah.seq.automation;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import net.judah.gui.Size;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.automation.Automation.CCData;
import net.judah.seq.track.Edit;
import net.judah.seq.track.Editor;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Prototype;
import net.judah.seq.track.Edit.Type;

class CCEdit extends AutoBox implements MidiConstants {

	private CCData existing;
	private Edit undoCurve;

	private final JComboBox<ControlChange> cc = new JComboBox<>(ControlChange.ORDERED);
	private final Btn create = new Btn("New", e->create());
	private final Btn change = new Btn("Change", e -> change());
	private final Btn delete = new Btn("Delete", e->delete());
	private final Btn select = new Btn("Select", e -> publishSelection());
	private final Btn exe = new Btn("Exe", e->exe());

	private final Box top = new Box(BoxLayout.PAGE_AXIS);
	private final Box algo = new Box(BoxLayout.PAGE_AXIS);
	private final JPanel checkbox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
	private final Val val = new Val(false, cc);
	private final Val val2 = new Val(true, cc);
	private final Tick steps;
	private final Tick steps2;
	private final JCheckBox automation = new JCheckBox("Enabled");
	private final JTextField messages = new JTextField("5", 3);

	protected CCEdit(MidiTrack owner) {

		super(BoxLayout.LINE_AXIS, owner);

		steps = new Tick(track);
		steps2 = new Tick(track);

		Box btns = new Box(BoxLayout.LINE_AXIS);
		btns.add(Gui.resize(cc, Size.WIDE_SIZE));
		btns.add(Box.createHorizontalStrut(12));
		btns.add(create);
		btns.add(change);
		btns.add(delete);
		btns.add(select);
		btns.add(exe);

		top.add(btns);
		top.add(val);
		top.add(steps);

		JLabel auto = new JLabel("Automation");
		auto.setFont(Gui.BOLD12);
		checkbox.add(auto);
		checkbox.add(automation);

		checkbox.add(new JLabel(" "));
		checkbox.add(Gui.resize(messages, Size.COMBO_SIZE));
		checkbox.add(new JLabel(" Steps"));

		algo.setBorder(Gui.SUBTLE);
		algo.add(checkbox);
		algo.add(val2);
		algo.add(steps2);

		automation.addChangeListener(l->enableAutomation(automation.isSelected()));
		cc.setFont(Gui.BOLD12);
		cc.addActionListener(e->ccChange());
		((JLabel)cc.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

		Box inner = new Box(BoxLayout.PAGE_AXIS);
		inner.add(Gui.wrap(top));
		inner.add(Gui.wrap(algo));
		inner.add(Box.createVerticalStrut(4));

		add(Box.createHorizontalStrut(12));
		add(inner);
		add(Box.createHorizontalStrut(12));

		init(0L);
	}

	private void ccChange() {
		cc.setToolTipText(((ControlChange)cc.getSelectedItem()).description);
		val.text(val.get()); // potentially CC switched to bool
	}

	public CCEdit edit(CCData data) {
		existing = data;
		undoCurve = null;
		if (data != null && data.e() != null && data.e().getMessage() instanceof ShortMessage msg) {
			cc.setSelectedItem(data.type());
			steps.quantizable(data.e().getTick());
			val.set(msg.getData2());
			delete.setEnabled(true);
			change.setEnabled(true);
			select.setEnabled(true);
		} else {
			init(track != null ? track.getLeft() : 0);
		}
		enableAutomation(false);
		return this;
	}

	@Override public CCEdit init(long tick) {
		existing = null;
		undoCurve = null;
		delete.setEnabled(false);
		change.setEnabled(false);
		select.setEnabled(false);
		cc.setSelectedItem(ControlChange.VOLUME);
		val.set(CUTOFF +1);
		steps.quantizable(tick);
		enableAutomation(true);
		return this;
	}

 	private void enableAutomation(boolean enable) {
 		steps2.setEnabled(enable);
 		val2.setEnabled(enable);
 		messages.setEnabled(enable);
 		checkbox.setBackground(enable ? ENABLED : DISABLED);
 		algo.setBackground(enable ? ENABLED : DISABLED);
	}

	void create() {
		try {
			ShortMessage msg = build();
			MidiEvent evt = new MidiEvent(msg, steps.getTick());
	 		Edit edit = new Edit(Type.NEW, evt);
			undoCurve = edit;

	 		if (automation.isSelected()) {
	 			int count;
	 			try {
	 				count = Integer.parseInt(messages.getText());
	 			} catch (NumberFormatException nfe) {
	 				RTLogger.warn(this, messages.getText() + ": " + nfe.getMessage());
					return;
				}
				if (count < 2) count = 2;
	 			double interval = (steps2.getTick() - steps.getTick()) / (double)(count - 1);
				float startVal = val.get();
				float endVal = val2.get();
	 			float delta = (endVal - startVal) / (count - 1);
	 			for (int step = 1; step < count; step++) {
	 				int data2 = Math.round(startVal + step * delta);
	 				long ticker = steps.getTick() + Math.round(step * interval);
					ControlChange type = (ControlChange)cc.getSelectedItem();
	 				ShortMessage unit = new ShortMessage(Midi.CONTROL_CHANGE, track.getCh(), type.data1, data2);
	 				edit.getNotes().add(new MidiEvent(unit, ticker));
	 			}
			}
	 		track.getEditor().push(edit);
			track.getEditor().publish(this, new ArrayList<>(edit.getNotes()));
		} catch (Throwable t) {
			RTLogger.warn(this, t);
		}
	}

	void change() {
		if (existing == null) {
			create();
			return;
		}
		try {
			long targetTick = steps.getTick();
			ShortMessage targetMsg = build();
			MidiEvent targetEvent = new MidiEvent(targetMsg, targetTick);
			Edit mod = new Edit(Type.MOD, existing.e(), targetEvent);

			if (automation.isSelected()) {
				mod.setDestination(new Prototype(val2.get(), steps2.getTick()));
			}

			track.getEditor().push(mod);
			track.getEditor().publish(this, Collections.singletonList(targetEvent));
		} catch (InvalidMidiDataException ie) {
			RTLogger.warn(this, ie);
		}
	}

	void delete() {
		if (undoCurve != null && !undoCurve.getNotes().isEmpty()) {
			Edit del = new Edit(Type.DEL, undoCurve.getNotes());
			track.getEditor().push(del);
			track.getEditor().publish(this, Collections.emptyList());
			return;
		}
		if (existing == null)
			return;

		Edit edit = new Edit(Type.DEL, existing.e());
		track.getEditor().push(edit);
		track.getEditor().publish(this, Collections.emptyList());
	}

	void exe() {
		try {
			track.send(build(), 0);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

	void publishSelection() {
		if (existing != null) {
			track.getEditor().publish(this, Collections.singletonList(existing.e()));
		}
	}

	@Override
	protected void updateSelection(Editor.Selection selection) {
		// if (selection.originId() == originId
		SwingUtilities.invokeLater(() -> {
			if (selection == null || selection.events().isEmpty()) {
				init(track != null ? track.getLeft() : 0);
				return;
			}

			CCData firstCC = null;
			for (MidiEvent event : selection.events()) {
				if (event.getMessage() instanceof ShortMessage sm && Midi.isCC(sm)) {
					ControlChange type = ControlChange.find(sm);
					if (type != null) {
						firstCC = new CCData(event, type);
						break;
					}
				}
			}

			if (firstCC != null) {
				edit(firstCC);
			} else {
				init(track != null ? track.getLeft() : 0);
			}
		});
	}

	private ShortMessage build() throws InvalidMidiDataException {
		return new ShortMessage(Midi.CONTROL_CHANGE, track.getCh(), ((ControlChange)cc.getSelectedItem()).data1, val.get());
	}

	@Override protected void pad1() {
		create();
	}

	@Override protected void pad2() {
		delete();
	}

	@Override protected boolean doKnob(int idx, int value) {
		switch (idx) {
		case 0: // cc/msgs val/ beat step
			cc.setSelectedIndex(Constants.ratio(value, ControlChange.ORDERED.length - 1));
			break;
		case 1:
			val.knob(value);
			break;
		case 2:
			steps.beatKnob(value);
			break;
		case 3:
			steps.stepKnob(value);
			break;
		case 4:
			messages.setText("" + Constants.ratio(value, 33));
			automation.setSelected(true);
			break;
		case 5:
			val2.knob(value);
			automation.setSelected(true);
			break;
		case 6:
			steps2.beatKnob(value);
			automation.setSelected(true);
			break;
		case 7:
			steps2.stepKnob(value);
			automation.setSelected(true);
			break;
		default: return false;
		}
		return true;
	}

	protected class Val extends JPanel {

		private JLabel display = new JLabel("  0");
		private final JSlider data2 = new JSlider(0, 127);
		private final JComboBox<ControlChange> sourceCC;

		Val(boolean automation) {
			this(automation, null);
		}

		Val(boolean automation, JComboBox<ControlChange> ccCombo) {
			super(new FlowLayout(FlowLayout.CENTER, 3, 3));
			this.sourceCC = ccCombo;
			Gui.resize(data2, WIDE);
			add(new JLabel(automation ? "End Value: " : "Value: "));
			add(data2);
			add(display);
			setEnabled(!automation);
			data2.addChangeListener(l->text(data2.getValue()));
			data2.setValue(MidiConstants.CUTOFF + 1);
		}

		public void knob(int value) {
			set(Constants.ratio(value, 127));
		}

		@Override public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			setBackground(enabled ? ENABLED: DISABLED);
		}

		void set(int val) {
			data2.setValue(val);
			text(val);
		}
		int get() {
			return data2.getValue();
		}
		public void text(int val) {
			if (sourceCC != null && ((ControlChange)sourceCC.getSelectedItem()).toggle)
				display.setText(val > MidiConstants.CUTOFF ? "  On " : " Off ");
			else {
				String out = " ";
				if (val < 100) out += " ";
				if (val < 10) out += " ";
				out += val;
				display.setText(out);
			}
		}
	}
}