package net.judah.seq;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.gui.Gui;
import lombok.Setter;
import net.judah.drumkit.DrumType;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Integers;
import net.judah.gui.widgets.ModalDialog;
import net.judah.seq.Edit.Type;
import net.judah.seq.piano.Piano;
import net.judah.seq.track.MidiTrack;

/** Provides a ModalDialog to enter specific transposition amounts
 * @see net.judah.gui.widgets.ModalDialog*/
public class Transpose extends JPanel {

	private static final Dimension SIZE = new Dimension(220, 160);

	private final MidiTrack track;
	private final MusicBox view;
	private Integers steps;
	private final Integers tones = new Integers(-11, 12);
	private final Integers octaves = new Integers(-5, 5);
	private final JComboBox<DrumType> drum = new JComboBox<>(DrumType.values());
	@Setter static private int delta;

	private final Btn cancel = new Btn("Cancel", e->ModalDialog.getInstance().setVisible(false));

	public Transpose(Piano view) {
		super(new GridLayout(0, 2));
		this.view = view;
		this.track = view.getTrack();

		add(new JLabel("Octaves")); // +/- 5
		add(octaves);

		add(new JLabel("SemiTones")); // +/- 11
		add(tones);

		add(new Btn("Ok", e->remap()));
		add(cancel);
		setName("Remap");
		new ModalDialog(Gui.wrap(this), SIZE);

	}

	public Transpose(MidiTrack t, MusicBox view) {
		super(new GridLayout(0, 2));
		this.track = t;
		this.view = view;

		steps = new Integers(track.getClock().getSteps() * -1, track.getClock().getSteps());


		add(new JLabel("Steps"));
		add(steps);

		if (t.isSynth()) {
			add(new JLabel("Octaves")); // +/- 5
			add(octaves);
			add(new JLabel("SemiTones")); // +/- 11
			add(tones);
		}
		else {
			add(new JLabel("Drum "));
			add(drum);
		}

		add(new Btn("Ok", e->ok()));
		add(cancel);
		setName("Transpose");
		new ModalDialog(Gui.wrap(this), SIZE);
	}

	private void ok() { // TODO bug
		ModalDialog.getInstance().setVisible(false);
		Edit e = new Edit(Type.TRANS, view.selected);
		int data1 = 0;
		if (track.isDrums())
			data1 = ((DrumType)drum.getSelectedItem()).getData1() -
					((ShortMessage)view.selected.get(0).getMessage()).getData1();
		if (track.isSynth())
			data1 = (Integer)octaves.getSelectedItem() * 12 + (Integer)tones.getSelectedItem();
		e.setDestination(new Prototype(data1, (Integer)steps.getSelectedItem()));
		track.getEditor().push(e);
	}

	private void remap() {
		ModalDialog.getInstance().setVisible(false);
		Edit e = new Edit(Type.REMAP, new ArrayList<MidiEvent>());

		int data1 = (Integer)tones.getSelectedItem() + 12 * (Integer)octaves.getSelectedItem();
		e.setDestination(new Prototype(data1, -1));
		track.getEditor().push(e);
	}

}
