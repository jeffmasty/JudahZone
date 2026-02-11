package net.judah.sampler.vocoder;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import judahzone.data.Frequency;
import judahzone.gui.Gui;
import judahzone.widgets.Knob;
import net.judah.gui.widgets.Slider;
import net.judah.sampler.Sampler;

/** GUI wrapper around StenzelCoder vocoder with parameter controls. */
public class VoiceBox extends JPanel {

	private final Sampler sampler;
	private final StenzelCoder stenzel = new StenzelCoder();

	private final JToggleButton active;
	private final Knob wetDry = new Knob();
	private final Knob preamp = new Knob();
	private final Knob gain = new Knob();
	private final Knob decay = new Knob();
	private final Slider pitch = new Slider(null);

	public VoiceBox(Sampler sampler) {
		this.sampler = sampler;

		active = new JToggleButton("Stenzel Vocoder", false);
		active.addActionListener(e -> {
			if (active.isSelected())
				on();
			else
				off();
		});

		pitch.setToolTipText("Pitch (MIDI note)");
		pitch.setMinimum(0);
		pitch.setMaximum(127);
		pitch.setValue(69); // A4
		pitch.addChangeListener(e -> stenzel.setHz(new Frequency(Frequency.midiToHz(pitch.getValue()))));

		wetDry.setToolTipText("Wet/Dry Mix");
		preamp.setToolTipText("Input Gain");
		gain.setToolTipText("Output Gain");
		decay.setToolTipText("Decay/Muffle");

		// Layout: Title + Toggle
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
		top.add(active);
		top.add(Box.createHorizontalGlue());

		// Layout: Knob grid (4 coders settings)
		JPanel knobs = new JPanel(new GridLayout(1, 4, 8, 8));
		knobs.add(Gui.duo(wetDry, new JLabel("Wet")));
		knobs.add(Gui.duo(preamp, new JLabel("Pre")));
		knobs.add(Gui.duo(gain, new JLabel("Out")));
		knobs.add(Gui.duo(decay, new JLabel("Decay")));

		// Layout: Pitch slider
		JPanel pitchPanel = new JPanel();
		pitchPanel.setLayout(new BoxLayout(pitchPanel, BoxLayout.X_AXIS));
		pitchPanel.add(new JLabel("Pitch: "));
		pitchPanel.add(Gui.resize(pitch, new Dimension(200, 30)));
		pitchPanel.add(Box.createHorizontalGlue());

		// Main layout
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(8));
		add(top);
		add(Box.createVerticalStrut(8));
		add(knobs);
		add(Box.createVerticalStrut(8));
		add(pitchPanel);
		add(Box.createVerticalStrut(8));

		update();
		listeners();
	}

	private void listeners() {
		wetDry.addListener(data2 -> stenzel.set(ZoneCoder.Settings.WET.ordinal(), data2));
		preamp.addListener(data2 -> stenzel.set(ZoneCoder.Settings.PREAMP.ordinal(), data2));
		gain.addListener(data2 -> stenzel.set(ZoneCoder.Settings.GAIN.ordinal(), data2));
		decay.addListener(data2 -> stenzel.set(ZoneCoder.Settings.DECAY.ordinal(), data2));
	}

	public void update() {
		wetDry.setValue(stenzel.get(ZoneCoder.Settings.WET.ordinal()));
		preamp.setValue(stenzel.get(ZoneCoder.Settings.PREAMP.ordinal()));
		gain.setValue(stenzel.get(ZoneCoder.Settings.GAIN.ordinal()));
		decay.setValue(stenzel.get(ZoneCoder.Settings.DECAY.ordinal()));
		pitch.setValue(Frequency.hzToMidi(stenzel.getHz()));
	}

	/** Map MIDI knob indices to ZoneCoder parameters: 0=Wet, 1=Preamp,
	    2=Gain, 3=Decay, 4=Pitch */
	public void doKnob(int idx, int value) {
		switch (idx) {
			case 0: // Wet/Dry
				SwingUtilities.invokeLater(() -> wetDry.setValue(value));
				break;
			case 1: // Preamp
				SwingUtilities.invokeLater(() -> preamp.setValue(value));
				break;
			case 2: // Gain
				SwingUtilities.invokeLater(() -> gain.setValue(value));
				break;
			case 3: // Decay
				SwingUtilities.invokeLater(() -> decay.setValue(value));
				break;
			case 4: // Pitch
				SwingUtilities.invokeLater(() -> pitch.setValue(value));
				break;
		}
	}

	void on() {
		sampler.setVoiceBox(stenzel);
		active.setSelected(true);
	}

	void off() {
		sampler.setVoiceBox(null);
		active.setSelected(false);
	}

}