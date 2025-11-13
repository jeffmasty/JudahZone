package net.judah.seq.track;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.midi.Midi;
import net.judah.seq.MidiConstants;
import net.judah.seq.track.CCHandler.CCData;

public class Automation extends KnobPanel implements MidiConstants {
	public static enum AutoMode { CC, Pitch, Prog };

	private static Automation instance;
	public static Automation getInstance() {
		if (instance == null)
			instance = new Automation();
		return instance;
	}

	@Getter private final KnobMode knobMode = KnobMode.Auto;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

	private final ButtonGroup btns = new ButtonGroup();
	private final JLabel track = new JLabel("");
	private final JToggleButton cc = new JToggleButton(" CC ");
	private final JToggleButton pitch = new JToggleButton("Pitch");
	private final JToggleButton prog = new JToggleButton("Prog");

	private Automation() {
		title.add(Gui.resize(track, Size.TINY));
		title.add(cc);
		// title.add(pitch); // TODO
		title.add(prog);
		btns.add(cc);
		btns.add(pitch);
		btns.add(prog);

		track.setFont(Gui.BOLD);
		cc.addChangeListener(l->change(cc));
		pitch.addChangeListener(l->change(pitch));
		prog.addChangeListener(l->change(prog));
		cc.doClick();
	}

	private void change(JToggleButton btn) {
		if (!btn.isSelected())
			return;
		if (btn == cc)
			install(CCEdit.getInstance());
		if (btn == pitch)
			install(PitchEdit.getInstance());
		if (btn == prog)
			install(ProgramEdit.getInstance());
	}

	public void init(MidiTrack t, long tick, AutoMode mode) {
		getInstance().track.setText(t.getName());
		switch(mode) {
			case CC -> install(CCEdit.getInstance().init(t, tick));
			case Pitch -> install(PitchEdit.getInstance().init(t, tick));
			case Prog -> install(ProgramEdit.getInstance().init(t, tick));
		}
		updateBtns(mode);
 		MainFrame.setFocus(this);
	}

	private void updateBtns(AutoMode mode) {
		switch(mode) {
			case CC -> {	if (!cc.isSelected()) 	 cc.setSelected(true);}
			case Pitch -> {	if (!pitch.isSelected()) pitch.setSelected(true);}
			case Prog -> {	if (!prog.isSelected())	 prog.setSelected(true);}
		}
	}

	public void edit(MidiTrack t, CCData dat) {
		getInstance().track.setText(t.getName());
		install(CCEdit.getInstance().edit(t, dat));
		updateBtns(AutoMode.CC);
		MainFrame.setFocus(this);
	}

	public void edit(MidiTrack t, MidiEvent e) {

		ShortMessage in = (ShortMessage) e.getMessage();
		if (Midi.isCC(in)) {
			CC type = CC.find(in.getData1());
			if (type != null)
				install(CCEdit.getInstance().edit(t, new CCData(e, type)));
			updateBtns(AutoMode.CC);
		}
		else if (Midi.isPitchBend(in)) {
			install(PitchEdit.getInstance().edit(t, e));
			updateBtns(AutoMode.Pitch);
		}

		else if (Midi.isProgChange(in)) {
			install(ProgramEdit.getInstance().edit(t, e));
			updateBtns(AutoMode.Prog);
		}
 		MainFrame.setFocus(this);
	}

	AutoBox getView() {
		if (cc.isSelected())
			return CCEdit.getInstance();
		if (pitch.isSelected())
			return PitchEdit.getInstance();
		return ProgramEdit.getInstance();
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
		public static Color ENABLED = null;
		public static Color DISABLED = Pastels.MY_GRAY;
		protected static Dimension WIDE = new Dimension(128, Size.STD_HEIGHT);
		AutoBox(int layout) {
			super(layout);
		}
		protected abstract AutoBox init(MidiTrack t, long tick);
		protected abstract void pad1();
		protected abstract void pad2();
		protected abstract boolean doKnob(int idx, int value);
	}

}
