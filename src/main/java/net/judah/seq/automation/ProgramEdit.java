package net.judah.seq.automation;

import java.awt.Dimension;
import java.util.Collections;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import net.judah.gui.Size;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.Edit;
import net.judah.seq.track.Editor;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Edit.Type;

class ProgramEdit extends AutoBox implements MidiConstants {

	private MidiEvent existing;

	private final Btn create = new Btn("Create", e->create());
	private final Btn delete = new Btn("Delete", e->delete());
	private final Btn select = new Btn("Select", e->publishSelection());
	private final Btn exe = new Btn("Exe", e->exe());
	private final JList<String> list = new JList<String>();
	private final Tick tick;


	protected ProgramEdit(MidiTrack owner) {
		super(BoxLayout.PAGE_AXIS, owner);
		tick = new Tick(track);
		Box btns = new Box(BoxLayout.LINE_AXIS);
		btns.add(Box.createHorizontalStrut(25));
		btns.add(new JLabel("Program Change"));
		btns.add(Box.createHorizontalStrut(25));
		btns.add(create);
		btns.add(delete);
		btns.add(select);
		btns.add(exe);

		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		Box mid = new Box(BoxLayout.LINE_AXIS);
		mid.add(Box.createHorizontalStrut(30));
		mid.add(Gui.resize(new JScrollPane(list), new Dimension(Size.WIDTH_KNOBS - 100, 140)));
		mid.add(Box.createHorizontalStrut(30));

		Box inner = new Box(BoxLayout.PAGE_AXIS);
		inner.add(Box.createVerticalStrut(8));
		inner.add(btns);
		inner.add(Box.createVerticalStrut(8));
		inner.add(Gui.wrap(mid));
		inner.add(Box.createVerticalStrut(1));
		inner.add(tick);
		inner.add(Box.createVerticalStrut(1));
		add(Gui.wrap(inner));

		init(0L);

		String[] source = track.getPatches();
		int length = source.length;
		DefaultListModel<String> model = new DefaultListModel<>();
		for (int i = 0; i < 128; i++)
			model.add(i, i + ": " + (i < length ? source[i] : ""));
		list.setModel(model);

	}

	protected ProgramEdit edit(MidiEvent e) {
		existing = e;
		if (e != null && e.getMessage() instanceof ShortMessage msg) {
			list.setSelectedIndex(msg.getData1());
			tick.quantizable(e.getTick());
			delete.setEnabled(true);
			select.setEnabled(true);
		} else {
			init(track != null ? track.getLeft() : 0);
		}
		return this;
	}

	@Override protected ProgramEdit init(long time) {
		existing = null;
		list.setSelectedIndex(0);
		tick.quantizable(time);
		delete.setEnabled(false);
		select.setEnabled(false);
		return this;
	}

	private void create() {
		try {
			MidiEvent target = new MidiEvent(build(), tick.getTick());
			// If an event already exists at this tick, it's a modification.
			if (existing != null && existing.getTick() == target.getTick()) {
				Edit mod = new Edit(Type.MOD, existing, target);
				track.getEditor().push(mod);
			} else { // Otherwise, it's a new event.
				Edit create = new Edit(Type.NEW, target);
				track.getEditor().push(create);
			}
			existing = target;
			track.getEditor().publish(this, Collections.singletonList(target));
			delete.setEnabled(true);
			select.setEnabled(true);

		} catch (InvalidMidiDataException me) {
			RTLogger.warn(this, me);
		}
	}

	private void exe() {
		try {
			track.send(build(), 0);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

	private void delete() {
		if (existing == null) return;
		Edit edit = new Edit(Type.DEL, existing);
		track.getEditor().push(edit);
		track.getEditor().publish(this, Collections.emptyList());
	}

	void publishSelection() {
		if (existing != null) {
			track.getEditor().publish(this, Collections.singletonList(existing));
		}
	}

	@Override
	protected void updateSelection(Editor.Selection selection) {
		SwingUtilities.invokeLater(() -> {
			if (selection == null || selection.events().isEmpty()) {
				init(track != null ? track.getLeft() : 0);
				return;
			}

			MidiEvent firstProgChange = null;
			for (MidiEvent event : selection.events()) {
				if (event.getMessage() instanceof ShortMessage sm && Midi.isProgChange(sm)) {
					firstProgChange = event;
					break;
				}
			}

			if (firstProgChange != null) {
				edit(firstProgChange);
			} else {
				init(track != null ? track.getLeft() : 0);
			}
		});
	}

	private ShortMessage build() throws InvalidMidiDataException {
		return new ShortMessage(Midi.PROGRAM_CHANGE, track.getCh(), list.getSelectedIndex(), 0);
	}

	@Override protected void pad1() {
		create();
	}

	@Override protected void pad2() {
		if (delete.isEnabled())
			delete();
	}

	@Override protected boolean doKnob(int idx, int value) {
		switch(idx) {
		case 0:
			list.setSelectedIndex(Constants.ratio(value, 127));
			break;
		case 1:
			tick.frameKnob(value);
			break;
		case 2:
			tick.beatKnob(value);
			break;
		case 3:
			tick.stepKnob(value);
			break;
		default:
			return false;
		}
		return true;
	}
}