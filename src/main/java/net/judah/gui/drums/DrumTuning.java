// File: `net/judah/gui/drums/OscTuning.java`
package net.judah.gui.drums;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.drums.Drum;
import judahzone.drums.DrumUpdate;
import judahzone.gui.Gui;
import judahzone.util.Frequency;
import net.judah.gui.Size;
import net.judah.gui.widgets.Slider;

public class DrumTuning extends JPanel {

	public static final int FIRST_MIDI = 12;
	public static final int LAST_MIDI = 111;

	private final Drum drum;

	private final Slider midiSlider = new Slider(FIRST_MIDI, LAST_MIDI, null);
	private static final Dimension S = new Dimension(100, 48);

	private final JLabel hz = new JLabel("440", JLabel.CENTER);
	private final JLabel note = new JLabel("C4", JLabel.CENTER);

	public DrumTuning(Drum drum) {
		this.drum = drum;

		midiSlider.setToolTipText("Tuning (logarithmic)");
		midiSlider.setPaintTicks(true);
		midiSlider.setMajorTickSpacing(25);
		Gui.resize(midiSlider, S);

		Gui.resize(hz, Size.MICRO);
		Gui.resize(note, Size.MICRO);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(note);
		add(midiSlider);
		add(hz);
		midiSlider.addChangeListener(l->drum.getEdit().set(
				new DrumUpdate(drum, DrumUpdate.Mode.BASE, 4, midiSlider.getValue())));

	}

	public void update() {
		if (drum == null)
			return;
		float freq = drum.getFundamental();
		int val = Frequency.hzToMidi(freq);
		if (midiSlider.getValue() != val)
			midiSlider.setValue(val);
		hz.setText("" + (int)freq);
		note.setText(Frequency.toNote(freq).toString());
	}

}
