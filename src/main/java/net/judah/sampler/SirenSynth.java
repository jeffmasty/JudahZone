package net.judah.sampler;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import judahzone.fx.op.ShepardRisset;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.util.Constants;
import net.judah.gui.Size;
import net.judah.gui.knobs.Knobs;
import net.judah.gui.widgets.Slider;

/**Shepard–Risset glissando -- Continuous Risset Scale with concurrent voices.
 * 4-segment envelope: attack, sustain, release, silence. Repeats cyclically.
 * https://en.wikipedia.org/wiki/Shepard_tone#Shepard%E2%80%93Risset_glissando*/
public class SirenSynth extends Knobs {

	public static enum Mode { SIREN, RINGMOD, SUM }; // future, see process summing

	private boolean isStopping = false;
	private int stopFadeCounter = 0;
	private static final int STOP_FADE_SAMPLES = (int)(0.05f * Constants.sampleRate()); // 50ms fade
	private static final Dimension PREFERRED = new Dimension(65, Size.HEIGHT_KNOBS);

	private JToggleButton playButton;
	private JSlider rateSlider = new JSlider();
	private JSlider ampSlider = new JSlider();
	private JLabel rateLabel = new JLabel();
	private JLabel ampLabel = new JLabel();

	private final ShepardRisset[] voices;
	private final int SR;
	private float rateScalar = 1f;
	private boolean isPlaying = false;

	public SirenSynth(int sampleRate) {
	    this.SR = sampleRate;
	    // Spread voices evenly across the sweep range (0..1) so they are always phase-staggered,
	    // independent of rate slider value. Voice i starts at fraction i/N of [RATE_LOWER..RATE_UPPER].
	    int n = 6;
	    this.voices = new ShepardRisset[] {
	        new ShepardRisset(0.0f,  1.0f, 0f / n),
	        new ShepardRisset(0.1f,  1.5f, 1f / n),
	        new ShepardRisset(0.23f, 1.0f, 2f / n),
	        new ShepardRisset(0.37f, 1.5f, 3f / n),
	        new ShepardRisset(0.53f, 1.0f, 4f / n),
	        new ShepardRisset(0.73f, 2.0f, 5f / n)
	    };
	    SwingUtilities.invokeLater(this::buildUI);
	}

	private void buildUI() {
	    playButton = new JToggleButton("Alarm");
	    playButton.addActionListener(e -> toggle());

	    int rate = 28;
	    rateLabel = new Gui.Bold("0%");
	    rateSlider = new Slider(-100, 100, null, "Glissando Rate");
	    rateSlider.setOrientation(JSlider.VERTICAL);
	    rateSlider.setMajorTickSpacing(25);
	    rateSlider.setMinorTickSpacing(20);
	    rateSlider.setPaintTicks(true);
	    rateSlider.setPaintLabels(true);
	    rateSlider.setValue(rate);
	    rateSlider.addChangeListener(e -> setRate(rateSlider.getValue()));
	    setRate(rate);

	    int amp = 23;
	    ampSlider = new Slider(0, 100, null);
	    ampLabel = new Gui.Bold(ampSlider.getValue() + "%");
	    ampSlider.setOrientation(JSlider.VERTICAL);
	    ampSlider.setMajorTickSpacing(20);
	    ampSlider.setMinorTickSpacing(10);
	    ampSlider.setPaintTicks(true);
	    ampSlider.setPaintLabels(true);
	    setAmp(amp);
	    ampSlider.addChangeListener(e -> setAmp(ampSlider.getValue()));

	    Box ratePanel = new Box(BoxLayout.Y_AXIS);
	    ratePanel.add(new Gui.Bold(" Rate "));
	    ratePanel.add(Gui.resize(rateSlider, PREFERRED));
	    ratePanel.add(rateLabel);

	    Box volPanel = new Box(BoxLayout.Y_AXIS);
	    volPanel.add(new Gui.Bold(" Amp "));
	    volPanel.add(Gui.resize(ampSlider, PREFERRED));
	    volPanel.add(ampLabel);

	    setLayout(new BorderLayout(8, 6));
	    add(volPanel, BorderLayout.WEST);
	    add(playButton, BorderLayout.CENTER);
	    add(ratePanel, BorderLayout.EAST);
	    setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
	}

	private void setRate(int rateValue) {
	    rateLabel.setText(rateValue + "%");
	    // Pivot at 0: sign-preserving square curve gives strong response across the mid-range.
	    // Clamp extreme values to prevent numerical instability over 4-second sweeps.
	    float norm = Math.max(-1f, Math.min(1f, rateValue / 100f));
	    float shaped = Math.signum(norm) * norm * norm; // sign-preserving x²

	    // Map slider to semitones-per-second around center pivot; choose a sensible max
	    // so the center produces near-zero movement and off-center produces steady glide.
	    final float MAX_SEMITONES_PER_SEC = 12f; // one octave per second at full deflection
	    float semitonesPerSecond = shaped * MAX_SEMITONES_PER_SEC;

	    // Compute multiplicative per-buffer factor: factor = 2^(semitones_per_buffer/12)
	    float semitonesPerBuffer = semitonesPerSecond * (Constants.bufSize() / (float) SR);
	    float perBufferFactor = (float)Math.pow(2.0, semitonesPerBuffer / 12.0);

	    // Defensive clamp to keep per-buffer multiplier in a safe range
	    perBufferFactor = Math.max(0.5f, Math.min(1.5f, perBufferFactor));

	    rateScalar = perBufferFactor;
	    for (ShepardRisset v : voices)
	        v.updateRateScaling(norm);
	}

	@Override
	public void doKnob(int idx, int value) {
	    if (idx == 1) {
	        int sliderValue = (value * 100 / 100) - 50;
	        if (sliderValue < -50) sliderValue = -50;
	        if (sliderValue > 50) sliderValue = 50;
	        final int sv = sliderValue * 2;
	        SwingUtilities.invokeLater(() -> rateSlider.setValue(sv));
	    } else if (idx == 0) {
	    	SwingUtilities.invokeLater(() -> ampSlider.setValue(value));
	    }
	}

	public void start(int ratePercentage) {
	    for (ShepardRisset v : voices)
	        v.reset();
	    setRate(ratePercentage);
	    this.isPlaying = true;
	    playButton.setBackground(Pastels.GREEN);
	    if (playButton.isSelected() == false)
	        playButton.setSelected(true);
	}


	public void stop() {
	    this.isStopping = true;
	    this.stopFadeCounter = 0; // TODO ramp
	}

	public void setAmp(int percent) {
        ampLabel.setText(percent + "%");
        float amp = percent * 0.01f;
	    for (ShepardRisset v : voices)
	        v.setAmplitude(Math.max(0f, Math.min(1f, amp)));
	}

	public void toggle() {
		if (isPlaying)
			stop();
		else
	            start(rateSlider.getValue());
	}

	public void process(float[] left, float[] right) {
	    if (!isPlaying && !isStopping)
	        return;

	    // Apply rate scalar once per buffer per voice, not per sample (destructive multiply)
	    for (ShepardRisset v : voices)
	        v.updateRate(rateScalar);

	    for (int i = 0; i < Constants.bufSize(); i++) {
	        float sample = 0f;
	        for (ShepardRisset v : voices) {
	            sample += v.nextSample();
	        }
	        sample /= voices.length;

	        // Apply stop fade
	        if (isStopping) {
	            float fadeScale = 1f - ((float)stopFadeCounter / STOP_FADE_SAMPLES);
	            sample *= Math.max(0f, fadeScale);
	            stopFadeCounter++;
	            if (stopFadeCounter >= STOP_FADE_SAMPLES) {
	                isPlaying = false;
	                isStopping = false;
	                if (playButton.isSelected())
	                    playButton.setSelected(false);
	                playButton.setBackground(null);
	                return;
	            }
	        }

			//    switch(mode) {
			//        case SIREN -> {
            left[i] += sample;
            right[i] += sample;
			//        case RINGMOD -> {
			//            left[i] *= sample;
			//            right[i] *= sample;
			//        case SUM -> {
			//        	left[i] = left[i] * 0.5f + sample * 0.5f;
			//            right[i] = right[i] * 0.5f + sample * 0.5f;
	    }
	}

}

