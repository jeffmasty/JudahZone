package net.judah.seq.automation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.seq.track.Editor;
import net.judah.seq.track.Editor.Delta;
import net.judah.seq.track.Editor.Selection;
import net.judah.seq.track.Editor.TrackListener;
import net.judah.seq.track.MidiTrack;
import net.judah.song.TraxCombo;

public class Automation extends KnobPanel implements MidiConstants, TrackListener {

	public static record CCData(MidiEvent e, ControlChange type) {}

	public static enum MidiMode { CC, Pitch, Program, Meta, All, NoteOn, ERROR };

	static final Object ORIGIN_ID = KnobMode.Autom8;
	@Getter private final KnobMode knobMode = KnobMode.Autom8;

	private static JPanel top;

	@Getter private final MidiTrack track;

	private final JTabbedPane content = new JTabbedPane();
	private final CCEdit cc;
	private final ProgramEdit program;
	private final PitchEdit pitch;
	private final MidiView all;

	public Automation(MidiTrack owner) {
		this.track = owner;

		cc = new CCEdit(owner);
		program = new ProgramEdit(owner);
		pitch = new PitchEdit(owner);
		all = new MidiView(this);

		content.addTab(MidiMode.All.name(), all);
		content.addTab(MidiMode.CC.name(), cc);
		content.addTab(MidiMode.Pitch.name(), pitch);
		content.addTab(MidiMode.Program.name(), program);
		Gui.resize(content, new Dimension(Size.WIDTH_KNOBS, Size.HEIGHT_KNOBS - Size.KNOB_TITLE.height));
		add(content);

		this.track.getEditor().addListener(this);

		Editor.Selection currentSelection = track.getEditor().getSelection();
		if (currentSelection != null) {
			selectionChanged(currentSelection);
		}
	}

	@Override
	public void selectionChanged(Selection selection) {
		AutoBox view = getView();
		if (view != null && selection != null) {
			if (selection.originId() != ORIGIN_ID) {
				view.updateSelection(selection);
			}
		}
	}

	public void init() {
		content.setSelectedComponent(all.init(0l));
 		MainFrame.setFocus(this);
	}

	public void init(long tick, MidiMode mode) {
		if (mode == MidiMode.CC)
			content.setSelectedComponent(cc.init(tick));
		else if (mode == MidiMode.Pitch)
			content.setSelectedComponent(pitch.init(tick));
		else if (mode == MidiMode.Program)
			content.setSelectedComponent(program.init(tick));
		else
			content.setSelectedComponent(all.init(tick));
		if (JudahZone.getInstance().getSeq().getCurrent() != track)
			JudahZone.getInstance().getSeq().getTracks().setCurrent(track);
 		MainFrame.setFocus(this);
	}

	public void edit(CCData dat) {
		content.setSelectedComponent(cc);
		cc.edit(dat);
		MainFrame.setFocus(this);
	}

	public void edit(MidiEvent e) {
		if (e == null) {
			content.setSelectedComponent(all.init(0l));
			MainFrame.setFocus(this);
			return;
		}
		if (e.getMessage() instanceof ShortMessage in) {
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
		} else {
			content.setSelectedComponent(all.init(e.getTick()));
		}
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

		protected final MidiTrack track;
		protected abstract AutoBox init(long tick);
		protected abstract void pad1();
		protected abstract void pad2();
		protected abstract boolean doKnob(int idx, int value);
		/** Updates the view's UI to reflect the given selection. */
		protected abstract void updateSelection(Selection selection);

		AutoBox(int layout, MidiTrack track) {
			super(layout);
			this.track = track;
		}
	}

	/** Data-change handling: ignore deltas whose origin is our shared origin; otherwise
	 *  treat like an EDIT and schedule update + crude updaters on the EDT. */
	@Override
	public void dataChanged(Delta delta) {
	    try {
	        SwingUtilities.invokeLater(() -> {
	        	all.refill();
	        });
	    } catch (Exception ex) {
	        RTLogger.warn(Automation.this, ex);
	    }
	}

	@Override
	public JComponent getTitle() {
		if (top == null) {
			TraxCombo trax = JudahZone.getInstance().getSeq().getTrax();
			if (trax.getSelectedItem() != track)
				trax.setSelectedItem(track);
			top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			top.add(Gui.resize(trax, Size.WIDE_SIZE));
		}
		return top;
	}
}