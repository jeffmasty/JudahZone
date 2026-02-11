package net.judah.drums.gui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.sound.midi.ShortMessage;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import judahzone.api.Key;
import judahzone.fx.EQ.EqBand;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.widgets.Knob;
import judahzone.widgets.Knob.KnobListener;
import lombok.Getter;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumType;
import net.judah.drums.synth.Bongo;
import net.judah.drums.synth.DrumOsc;
import net.judah.drums.synth.DrumParams;
import net.judah.drums.synth.DrumParams.FilterParam;
import net.judah.drums.synth.DrumSynth;
import net.judah.drums.synth.Snare;
import net.judah.midi.Actives;

/** GUI pad for a single DrumOsc in DrumSynth. */
public class DrumOscPad extends Gui.Opaque implements KnobListener {

	private static final Color borderColor = Pastels.PURPLE;
	private final DrumKit kit;
	@Getter private final DrumOsc drum;
	private final Knob knob;
	private final JLabel data = new JLabel("", JLabel.CENTER);
	private final JLabel label = new JLabel("", JLabel.CENTER);
	private final JPanel top = new Gui.Opaque();
	private JCheckBox choke; // OHats only
	private int currentTab = 0;
	private int currentParam = 0;

	public DrumOscPad(DrumOsc drum, DrumSynth synth) {
		this.kit = synth;
		this.drum = drum;
		knob = new Knob(this);
		knob.setKnobColor(Pastels.RED);

		// OHat-specific choke checkbox
		if (drum.getType() == DrumType.OHat) {
			choke = new JCheckBox();
			choke.setToolTipText("Stop Open Hat when Closed Hat plays");
			top.add(choke);
			choke.addItemListener(e -> {
				kit.setChoked(choke.isSelected());
			});
		}

		addMouseListener(new NotePad(kit, (byte)drum.getType().getData1(), synth.getChannel(), 100,
				()->  OneDrumFrame.load(drum, synth)));
		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(3, 1));
		top.add(new JLabel(drum.getType().name()));

		add(top);

		add(Gui.wrap(knob));

		JPanel bottom = new JPanel(new GridLayout(2, 1));
		bottom.add(data);
		bottom.add(label);
		add(bottom);

		updateMode(0, 0);
	}

	@Override
	public void knobChanged(int value) {
	    // Forward local pad knob moves to the kit's ZoneDrums doKnob handler
	    // so the change becomes the single source-of-truth update (DrumParams.set).
	    kit.getKnobs().doKnob(drum.getType().ordinal(), value);
	}

	/** Update pad background to reflect active play state. */
	public void update(Actives actives) {
		ShortMessage msg = actives.find(drum.getType().getData1());
		top.setBackground(msg == null ? null : Pastels.GREEN);
		if (choke != null && choke.isSelected() != kit.isChoked())
			choke.setSelected(kit.isChoked());
	}

	public void update() {
		// knobs stale i.e., progChange
		int update = DrumParams.get(drum, currentTab, currentParam);
		update(update);
	}

	public void update(int value) {
		if (knob.getValue() != value)
			knob.setValue(value);
		data(value);
	}

	private void data(int value) {
		data.setForeground(Color.BLACK);
		if (currentTab == 0)
			data.setText(value + "");

		else if (currentTab == 1) {
			if (currentParam == 0) // cutoff freq
				hzDuo(value, EqBand.Bass);
			else if (currentParam == 1) // cutoff freq
				hzDuo(value, EqBand.High);

			else if (currentParam == 2 || currentParam == 3)  // resonance
				data.setText(String.format("%.1f", value * 0.25f));
			else
				data.setText(value + " %");
		}
		else if (currentTab == 2) {
			if (currentParam > 2)  // pitch
				hzDuo(value, EqBand.Mid);
			else {
				if (currentParam == 0 && drum instanceof Snare snare) // snare noise type
					data.setText(snare.getNoiseGen().getColour().name());
				else if (currentParam == 2 && drum instanceof Bongo bongo) // bongo skin
					data.setText(bongo.getMembrane().name());
				else
					data.setText(value + "");
			}
		}
	}

	private void hzDuo(int data1, EqBand type) {
		Key key = Key.key(data1);
		int octave = data1 / 12;
		float freq = Key.toFrequency(key, octave);
		data.setText("" + (int)freq);
		label.setText(key.toString() + octave);

		switch(type) {
			case Bass -> {
			if (freq > drum.getHz(EqBand.High))
				data.setForeground(Color.RED);
			}
			case Mid -> {
				if (freq < drum.getHz(EqBand.Bass) || freq > drum.getHz(EqBand.High))
					data.setForeground(Color.RED);
			}
			case High ->{
				if (freq < drum.getHz(EqBand.Bass))
					data.setForeground(Color.RED);
			}
		}
	}

	public void updateMode(int tabIdx, int paramIdx) {
		currentTab = tabIdx;
		currentParam = paramIdx;
		label();
		// Sync knob to current parameter value
		// Called when tab/param selection changes
		update(DrumParams.get(drum, tabIdx, paramIdx));
	}

	private void label() {
		// Update label() to show current param name
		if (currentTab == 0) {
			label.setText("%");
		}
		else if (currentTab == 1) {
			label.setText(FilterParam.values()[currentParam].name());
		}
		else if (currentTab == 2) {
			if (currentParam > 2)
				label.setText(""); // Midi1 data
			else
				label.setText(drum.getSettings().names()[currentParam]);
		}
	}

}