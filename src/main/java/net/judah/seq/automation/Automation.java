package net.judah.seq.automation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.widgets.TraxCombo;
import net.judah.midi.Midi;
import net.judah.seq.MidiConstants;
import net.judah.seq.Musician;
import net.judah.seq.track.MidiTrack;

public class Automation extends KnobPanel implements MidiConstants {
	public static enum AutoMode { CC, PC, Hz, All };

	private static final Dimension TRIX = new Dimension(60, Size.STD_HEIGHT);

	private static Automation instance;
	public static Automation getInstance() {
		if (instance == null)
			instance = new Automation();
		return instance;
	}

	@Getter private final KnobMode knobMode = KnobMode.Autom8;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 1));

	@Getter private MidiTrack track;

	private TraxCombo trax = new TraxCombo(this);
	private final ButtonGroup btns = new ButtonGroup();
	private final JToggleButton cc = new JToggleButton(AutoMode.CC.name());
	private final JToggleButton prog = new JToggleButton(AutoMode.PC.name());
	private final JToggleButton pitch = new JToggleButton(AutoMode.Hz.name()); // Key.SHARP + " " + Key.FLAT
	private final JToggleButton table = new JToggleButton(AutoMode.All.name());

	private Automation() {
		title.add(Gui.resize(trax, TRIX));
		title.add(cc);
		title.add(prog);
		title.add(pitch); // TODO
		title.add(table);

		btns.add(cc);
		btns.add(prog);
		btns.add(pitch);
		btns.add(table);

		cc.addChangeListener(l->change(cc));
		pitch.addChangeListener(l->change(pitch));
		prog.addChangeListener(l->change(prog));
		table.addChangeListener(l->change(table));
		cc.doClick();
		setTrack(JudahZone.getSeq().getCurrent());

	}

	private void change(JToggleButton btn) {
		if (track == null)
			setTrack(JudahZone.getSeq().getTracks().getCurrent());
		if (!btn.isSelected())
			return;
		if (btn == cc)
			install(CCEdit.getInstance());
		else if (btn == pitch)
			install(PitchEdit.getInstance());
		else if (btn == prog)
			install(ProgramEdit.getInstance());
		else
			install(AllView.getInstance());
	}

	public void setTrack(MidiTrack midi) {
		track = midi;
		if (trax.getSelectedItem() != track)
			trax.setSelectedItem(track);
		CCEdit.getInstance().setTrack(track);
		ProgramEdit.getInstance().setTrack(track);
		PitchEdit.getInstance().setTrack(track);
		AllView.getInstance().setTrack(track);
	}

	public void init(MidiTrack t, long tick, AutoMode mode) {
		setTrack(t);
		switch(mode) {
			case CC -> install(CCEdit.getInstance().init(tick));
			case Hz -> install(PitchEdit.getInstance().init(tick));
			case PC -> install(ProgramEdit.getInstance().init(tick));
			case All -> install(AllView.getInstance().init(tick));
		}
		updateBtns(mode);
 		MainFrame.setFocus(this);
	}

	public void edit(MidiTrack t, CCData dat) {
		setTrack(t);
		install(CCEdit.getInstance().edit(dat));
		updateBtns(AutoMode.CC);
		MainFrame.setFocus(this);
	}

	public void edit(MidiTrack t, MidiEvent e) {
		setTrack(t);
		if (e == null) { // ?
			install(AllView.getInstance().init(0l));
			MainFrame.setFocus(this);
			return;
		}
		ShortMessage in = (ShortMessage) e.getMessage();
		if (Midi.isCC(in)) {
			CC type = CC.find(in.getData1());
			if (type != null)
				install(CCEdit.getInstance().edit(new CCData(e, type)));
			updateBtns(AutoMode.CC);
		}
		else if (Midi.isPitchBend(in)) {
			install(PitchEdit.getInstance().edit(e));
			updateBtns(AutoMode.Hz);
		}

		else if (Midi.isProgChange(in)) {
			install(ProgramEdit.getInstance().edit(e));
			updateBtns(AutoMode.PC);
		}
 		MainFrame.setFocus(this);
	}

	private void updateBtns(AutoMode mode) {
		switch(mode) {
			case CC    ->   {if (!cc.isSelected()) 	 cc.setSelected(true);}
			case Hz -> 	{if (!pitch.isSelected()) pitch.setSelected(true);}
			case PC  -> 	{if (!prog.isSelected())	 prog.setSelected(true);}
			case All   -> 	{if (!table.isSelected())   table.setSelected(true);}
		}
	}

	AutoBox getView() {
		if (cc.isSelected())
			return CCEdit.getInstance();
		if (pitch.isSelected())
			return PitchEdit.getInstance();
		if (table.isSelected())
			return AllView.getInstance();
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
		public static Color DISABLED = Pastels.BLUE;
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

		protected Musician getMusician(MidiTrack t) {
			Musician result = TabZone.getMusician(t);
			if (result != null)
				return result;
			TabZone.edit(t);
			return TabZone.getMusician(t);
		}
	}

}
