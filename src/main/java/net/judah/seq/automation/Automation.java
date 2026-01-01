package net.judah.seq.automation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.seq.MidiConstants;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.MidiTrack;
import net.judah.song.TraxCombo;

public class Automation extends KnobPanel implements MidiConstants {

	public static record CCData(MidiEvent e, ControlChange type) {}

	public static enum MidiMode { CC, Pitch, Program, Meta, All, NoteOn, NoteOff }; // TODO

//	private static Automation instance;
//	public static Automation getInstance(TraxCombo combo, MidiTrack current) {
//		if (instance == null)
//			instance = new Automation(combo, current);
//		return instance;
//	}

	@Getter private final KnobMode knobMode = KnobMode.Autom8;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
	@Getter private MidiTrack track;
	private final TraxCombo trax;

	private final JTabbedPane content = new JTabbedPane();
	private final CCEdit cc = new CCEdit();
	private final ProgramEdit program = new ProgramEdit();
	private final PitchEdit pitch = new PitchEdit();
	private final MidiView all = new MidiView(this);

	public Automation(TraxCombo combo, MidiTrack current) {
		trax = combo;
		title.add(Gui.resize(trax, Size.WIDE_SIZE));

		content.addTab(MidiMode.All.name(), all);
		content.addTab(MidiMode.CC.name(), cc);
		content.addTab(MidiMode.Pitch.name(), pitch);
		content.addTab(MidiMode.Program.name(), program);
		add(content);
		setTrack(current);
	}

	public void setTrack(MidiTrack midi) {
		track = midi;
		if (trax.getSelectedItem() != track)
			trax.setSelectedItem(track);

		cc.setTrack(track);
		program.setTrack(track);
		pitch.setTrack(track);
		all.setTrack(track);
	}

	public void init(MidiTrack t) {
		setTrack(t);
		content.setSelectedComponent(all.init(0l));
 		MainFrame.setFocus(this);
	}

	public void init(MidiTrack t, long tick, MidiMode mode) {
		setTrack(t);
		if (mode == MidiMode.CC)
			content.setSelectedComponent(cc.init(tick));
		else if (mode == MidiMode.Pitch)
			content.setSelectedComponent(pitch.init(tick));
		else if (mode == MidiMode.Program)
			content.setSelectedComponent(program.init(tick));
		else // TODO meta
			content.setSelectedComponent(all.init(tick));
 		MainFrame.setFocus(this);
	}

	public void edit(MidiTrack t, CCData dat) {
		setTrack(t);
		content.setSelectedComponent(cc);
		cc.edit(dat);
		MainFrame.setFocus(this);
	}

	public void edit(MidiTrack t, MidiEvent e) {
		setTrack(t);
		if (e == null) { // ?
			content.setSelectedComponent(all.init(0l));
			MainFrame.setFocus(this);
			return;
		}
		ShortMessage in = (ShortMessage) e.getMessage();
		if (Midi.isCC(in)) {
			ControlChange type = ControlChange.find(in.getData1());
			if (type != null)
				content.setSelectedComponent(cc.edit(new CCData(e,  type)));
		}
		else if (Midi.isPitchBend(in))
			content.setSelectedComponent(pitch.edit(e));

		else if (Midi.isProgChange(in))
			content.setSelectedComponent(program.edit(e));
		else
			content.setSelectedComponent(all.init(e.getTick()));
 		MainFrame.setFocus(this);
	}

	AutoBox getView() {
		return (AutoBox) content.getSelectedComponent();
	}

	@Override public void pad1() {
		getView().pad1();
	}

	@Override public void pad2() {
		getView().pad2();
	}

	@Override public boolean doKnob(int idx, int value) {
		return getView().doKnob(idx, value);
	}

	// Child views
	public static abstract class AutoBox extends Box {
		public static Color ENABLED = Pastels.BLUE;
		public static Color DISABLED = Pastels.SHADE;
		protected static Dimension WIDE = new Dimension(128, Size.STD_HEIGHT);
		AutoBox(int layout) {
			super(layout);
		}
		protected MidiTrack track;
		protected abstract void setTrack(MidiTrack t);
		protected abstract AutoBox init(long tick);
		protected abstract void pad1();
		protected abstract void pad2();
		protected abstract boolean doKnob(int idx, int value);
	}

	public void update(Update type, Computer c) {
		if (c != track)
			return;
		if (type == Update.EDIT || type == Update.FILE)
			all.setTrack(track); // refill

	}

}
