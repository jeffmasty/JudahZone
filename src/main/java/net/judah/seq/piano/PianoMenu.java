package net.judah.seq.piano;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import judahzone.gui.Actionable;
import judahzone.gui.Floating;
import judahzone.gui.Gui;
import judahzone.widgets.Integers;
import judahzone.widgets.RangeSlider;
import net.judah.gui.Size;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.widgets.GateCombo;
import net.judah.seq.Seq;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.Duration;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackMenu;
import net.judah.seq.track.Transpose;

public class PianoMenu extends TrackMenu implements Floating {

//	private static final String[] VERT = { "Top", "Bottom"};
//	private static final String[] HORZ = { "Left", "Right" };
//	private final String a;
//	private final String b;
//	a = track.isDrums() ? HORZ[0] : VERT[0];
//	b = track.isDrums() ? HORZ[1] : VERT[1];

	private final PianoView view;
	private final Seq seq;
	private final PianoTrack t;

	private final GateCombo gate;
	private final ModeCombo mode;

	private final JComboBox<Integer> range;//  = new JComboBox<Integer>(Integers.generate(0, 88));
	private final RangeSlider viewport;; // half octaves based around 8 = MidiConstants.MIDDLE_C (60)

	public PianoMenu(PianoView view, Piano grid, Seq seq) {
		super(grid);
		this.view = view;
		this.seq = seq;
		t = (PianoTrack) track;
		gate = new GateCombo(t);
		mode = new ModeCombo(t);
		range = new JComboBox<Integer>(Integers.generate(0, 88));
		range.setSelectedItem(t.getRange());
        menu.add(edit);
        menu.add(tools);
        viewport = new RangeSlider(0, 16);
        viewport.setValue(6);
        viewport.setExtent(4);
        viewport.addChangeListener(e -> {
            int centerNote = (viewport.getValue() + viewport.getExtent() / 2) * 6; // half-octave to semitones
            int rangeHalfOctaves = viewport.getExtent();
            int rangeSemitones = rangeHalfOctaves * 6;
            // Ensure minimum 1.5 octave (18 semitones = 3 half-octaves)
            if (rangeHalfOctaves < 3) {
                viewport.setExtent(3);
                return;
            }
            view.setRangeAndTonic(rangeSemitones, centerNote);
        });
	}

	/** PianoMenu specific: Range, octave navigation, gate, arpeggiator mode, duration, transpose */
	@Override protected void childMenus() {
		add(new JLabel(" Arp"));
		add(mode);
		add(new JLabel("Span"));
		add(Gui.resize(range, Size.MICRO));
		add(new JLabel("Octs"));
		add(viewport); // resize

		tools.add(new Actionable("Remap...", e->new Transpose(view.grid)));
		file.add(new Actionable("Rename", e->seq.rename(t)));
		file.add(new Actionable("Delete", e->seq.confirmDelete(t)));
		edit.add(new Actionable("Duration...", e->new Duration(grid)));
		add(Box.createHorizontalStrut(4));
		add(new JLabel("Amp "));
		add(new JLabel(" Gate"));
		add(Gui.resize(gate, Size.SMALLER));

		tools.add(new Actionable("Flip view", e->view.flip()));
	}

	@Override
	public void update(Update type) {
		if (type == Update.ARP)
			mode.update();
		else if (type == Update.GATE)
			gate.update();
		else if (type == Update.RANGE)
			range.setSelectedItem(((PianoTrack)track).getRange());
		else
			super.update(type);
	}

	@Override
	public void resized(int w, int h) {
		Gui.resize(this, new Dimension(w, h));
	}

	public void adjustViewport(boolean up) {
		// grow viewport (value + extent) on up, shrink (value + extent) on down, symetric around center
		int center = viewport.getValue() + viewport.getExtent() / 2;
		if (!up) {
			if (viewport.getExtent() < 16) { // max 8 octaves
				viewport.setExtent(viewport.getExtent() + 2); // grow by 1 octave (2 half-octaves)
				viewport.setValue(center - viewport.getExtent() / 2);
			}
		} else {
			if (viewport.getExtent() > 3) { // min 1.5 octaves
				viewport.setExtent(viewport.getExtent() - 2); // shrink by 1 octave (2 half-octaves)
				viewport.setValue(center - viewport.getExtent() / 2);
			}
		}

	}

}