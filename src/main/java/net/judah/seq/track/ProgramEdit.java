package net.judah.seq.track;

import java.awt.Dimension;

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

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiConstants;
import net.judah.seq.MidiPair;
import net.judah.seq.track.Automation.AutoBox;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

class ProgramEdit extends AutoBox implements MidiConstants {
	private static ProgramEdit instance;
	public static ProgramEdit getInstance() {
		if (instance == null)
			instance = new ProgramEdit();
		return instance;
	}
	private MidiTrack track;
	private MidiEvent existing;

	private final Btn update = new Btn("Publish", e->publish());
	private final Btn delete = new Btn("Delete", e->delete());
	private final Btn exe = new Btn("Exe", e->exe());
	private final JList<String> list = new JList<String>();
	private final Tick tick = new Tick();



	private ProgramEdit() {
		super(BoxLayout.PAGE_AXIS);

		Box btns = new Box(BoxLayout.LINE_AXIS);
		btns.add(Box.createHorizontalStrut(25));
		btns.add(new JLabel("Program Change"));
		btns.add(Box.createHorizontalStrut(25));
		btns.add(update);
		btns.add(delete);
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

	}

	protected ProgramEdit edit(MidiTrack t, MidiEvent e) {
		setTrack(t);
		existing = e;
		list.setSelectedIndex(((ShortMessage)e.getMessage()).getData1());
		tick.quantizable(e.getTick());
		delete.setEnabled(true);
		return this;
	}

	@Override
	protected ProgramEdit init(MidiTrack t, long time) {
		setTrack(t);
		existing = null;
		list.setSelectedIndex(0);
		tick.quantizable(time);
		delete.setEnabled(false);
		return this;
	}

	private void setTrack(MidiTrack t) {
		track = t;
		tick.setTrack(t);
		String[] source = t.getMidiOut().getPatches();
		int length = source.length;
		DefaultListModel<String> model = new DefaultListModel<>();
		for (int i = 0; i < 128; i++)
			model.add(i, i + ": " + (i < length ? source[i] : ""));
		list.setModel(model);
	}

	private void publish() {
		try {
			MidiEvent target;
			if (existing == null) {
				target = new MidiEvent(build(), tick.getTick());
				Edit create = new Edit(Type.NEW, new MidiPair(target, null));
				TabZone.getMusician(track).push(create);
				delete.setEnabled(true);
			} else {
				target = new MidiEvent(build(), tick.getTick());
				Edit mod = new Edit(Type.MOD, new MidiPair(existing, target));
				TabZone.getMusician(track).push(mod);
			}
			existing = target;

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
		Edit edit = new Edit(Type.DEL, new MidiPair(existing, null));
		TabZone.getMusician(track).push(edit);
		delete.setEnabled(false);
	}
	private ShortMessage build() throws InvalidMidiDataException {
		return new ShortMessage(Midi.PROGRAM_CHANGE, track.getCh(), list.getSelectedIndex(), 0);
	}

	@Override protected void pad1() {
		publish();
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
