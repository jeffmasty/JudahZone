package net.judah.sampler;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.util.Constants;
import judahzone.widgets.CenteredCombo;
import net.judah.gui.Size;
import net.judah.gui.knobs.Knobs;

/**Shepard–Risset glissando -- Continuous Risset Scale with concurrent voices.
 * 4-segment envelope: attack, sustain, release, silence. Repeats cyclically.
 * https://en.wikipedia.org/wiki/Shepard_tone#Shepard%E2%80%93Risset_glissando*/
public class SirenSynth extends Knobs {

	public static enum Mode { SIREN, RINGMOD, SUM };
	private static enum EnvelopeStage { ATTACK, SUSTAIN, RELEASE, SILENCE };

	private static final int DURATION_MS = 450;
	private static final int WAVETABLE_size = 512;
	private static final float[] WAVETABLE = new float[WAVETABLE_size];

	private boolean isStopping = false;
	private int stopFadeCounter = 0;
	private static final int STOP_FADE_SAMPLES = (int)(0.05f * Constants.sampleRate()); // 50ms fade

	static {
	    double twopi = 2.0 * Math.PI;
	    for (int i = 0; i < WAVETABLE_size; i++) {
	        double phase = twopi * i / (WAVETABLE_size - 1);
	        float sample = 0f;
	        float amplitude = 1f;
	        for (int h = 0; h < 8; h++) {
	            sample += amplitude * (float)Math.sin(phase);
	            amplitude *= 0.5f;
	            phase *= 2.0;
	        }
	        WAVETABLE[i] = sample / 8f;
	    }
	}

	private JToggleButton playButton;
	private JComboBox<Mode> modeCombo;
	private JSlider rateSlider;
	private JSlider ampSlider;
	private JLabel rateLabel;
	private JLabel ampLabel;

	private final ShepardRisset[] voices;
	private final int SR;
	private float rateScalar = 1f;
	private int mode = 0;
	private boolean isPlaying = false;

	public SirenSynth(int sampleRate) {
	    this.SR = sampleRate;
	    this.voices = new ShepardRisset[] {
	        new ShepardRisset(0.0f, 1.0f),
	        new ShepardRisset(0.1f, 1.5f),
	        new ShepardRisset(0.23f, 1.0f),
	        new ShepardRisset(0.37f, 1.5f),
	        new ShepardRisset(0.53f, 1.0f),
	        new ShepardRisset(0.73f, 2.0f)
	    };
	    SwingUtilities.invokeLater(this::buildUI);
	}

	private void buildUI() {
	    playButton = new JToggleButton("Alarm");
	    playButton.addActionListener(e -> toggle());
	    modeCombo = new CenteredCombo<>(Mode.values());
	    Gui.resize(modeCombo, Size.MEDIUM);
	    modeCombo.setSelectedIndex(0);

	    int rate = 28;
	    rateLabel = new Gui.Bold("0%");
	    rateSlider = new JSlider(-100, 100, rate);
	    rateSlider.setOrientation(JSlider.VERTICAL);
	    rateSlider.setMajorTickSpacing(25);
	    rateSlider.setMinorTickSpacing(20);
	    rateSlider.setPaintTicks(true);
	    rateSlider.setPaintLabels(true);
	    rateSlider.setValue(rate);
	    rateSlider.addChangeListener(e -> setRate(rateSlider.getValue()));
	    setRate(rate);

	    int amp = 23;
	    ampSlider = new JSlider(0, 100, amp);
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
	    ratePanel.add(rateSlider);
	    ratePanel.add(rateLabel);

	    Box volPanel = new Box(BoxLayout.Y_AXIS);
	    volPanel.add(new Gui.Bold(" Amp "));
	    volPanel.add(ampSlider);
	    volPanel.add(ampLabel);

	    setLayout(new BorderLayout(8, 6));
	    add(volPanel, BorderLayout.WEST);
	    add(playButton, BorderLayout.CENTER);
	    add(ratePanel, BorderLayout.EAST);
	    setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
	}

	private void setRate(int rateValue) {
	    rateLabel.setText(rateValue + "%");
	    rateScalar = 1f + (float)Math.pow(rateValue / 100f, 3.0) * 10f / SR;
	    for (ShepardRisset v : voices)
	        v.updateRateScaling(rateValue / 100f);
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

	public void start(int ratePercentage, int modeIdx) {
	    this.mode = Math.max(0, Math.min(2, modeIdx));
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
	    this.stopFadeCounter = 0;
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
            start(rateSlider.getValue(), modeCombo.getSelectedIndex());
	}

	public void process(float[] left, float[] right) {
	    if (!isPlaying && !isStopping)
	        return;

	    for (int i = 0; i < Constants.bufSize(); i++) {
	        float sample = 0f;
	        for (ShepardRisset v : voices) {
	            v.updateRate(rateScalar);
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

	        if (mode == 0) {
	            left[i] += sample;
	            right[i] += sample;
	        } else if (mode == 1) {
	            left[i] *= sample;
	            right[i] *= sample;
	        } else {
	            left[i] = left[i] * 0.5f + sample * 0.5f;
	            right[i] = right[i] * 0.5f + sample * 0.5f;
	        }
	    }
	}

	/**Glissando voice with 4-segment envelope: attack, sustain, release, silence. */
	public static class ShepardRisset {
		private static final float RATE_UPPER = 2f;
		private static final float RATE_LOWER = 0.5f;
		private static final float DEFAULT_ATTACK_SECONDS = 0.6f;
		private static final int WINDOW = (int)(DEFAULT_ATTACK_SECONDS * Constants.sampleRate());

		private float phase;
		private float rate;
		private float amplitude;
		private float envLevel;

		private int resetPos;
		private int resetTotal;
		private int halfWindowSamples;
		private float preResetLevel;

		private final float resetPhaseOffset;
		private float fundamentalRatio;
		private float windowScaling = 1f;
		private float windowShape = 0.4f;

		private EnvelopeStage stage = EnvelopeStage.ATTACK;
		private int stageSamples;
		private int stageCounter;
		private final int attackSamples;
		private final int releaseSamples;
		private final int silenceSamples;

		ShepardRisset(float phaseOffset, float fundamentalRatio) {
		    this.resetPhaseOffset = phaseOffset;
		    this.fundamentalRatio = fundamentalRatio;
		    this.amplitude = 0.1f;
		    this.envLevel = this.amplitude;
		    this.resetPos = 0;
		    this.resetTotal = 0;
		    this.halfWindowSamples = Math.max(1, (int)((DURATION_MS / 1000f) * Constants.sampleRate()));
		    int samples = (int)((DURATION_MS / 1000f) * Constants.sampleRate());
		    attackSamples = releaseSamples = silenceSamples = samples;
		    reset();
		}

		void setWindowShape(float shape) {
		    if (shape <= 0f) shape = 0.01f;
		    this.windowShape = shape;
		}

		void updateRateScaling(float normalizedRate) {
		    float adjustedRate = normalizedRate + (resetPhaseOffset * 0.3f);
		    adjustedRate = Math.max(-1f, Math.min(1f, adjustedRate));
		    float distanceFromCenter = Math.abs(adjustedRate);
		    windowScaling = 2f - (1.5f * distanceFromCenter);
		    windowScaling = Math.max(0.5f, Math.min(2.5f, windowScaling));
		}

		void reset() {
		    this.phase = 0f;
		    this.rate = 1f * fundamentalRatio;

		    int baseHalfWindow = (int)((DURATION_MS / 1000f) * Constants.sampleRate());
		    this.halfWindowSamples = Math.max(1, (int)(baseHalfWindow * windowScaling));

		    int requestedTotal = this.halfWindowSamples * 2;
		    if (requestedTotal > WINDOW && WINDOW > 0)
		        requestedTotal = WINDOW;
		    this.halfWindowSamples = Math.max(1, requestedTotal / 2);

		    this.resetTotal = this.halfWindowSamples * 2;
		    this.resetPos = 0;
		    this.preResetLevel = this.envLevel;

		    this.stage = EnvelopeStage.ATTACK;
		    this.stageCounter = -1;
		    this.stageSamples = attackSamples;
		}

		void setAmplitude(float amp) {
		    this.amplitude = amp;
		    if (resetPos == 0)
		        this.envLevel = amplitude;
		}

		void updateRate(float rateScalar) {
		    rate *= rateScalar;
		}

		float nextSample() {
		    // Update envelope stage
		    updateEnvelope();

		    // Process oscillator (existing logic)
		    if (resetPos < resetTotal && resetTotal > 0) {
		        if (resetPos < halfWindowSamples) {
		            float t = (float)resetPos / (float)halfWindowSamples;
		            float shaped = (float)Math.pow(t, windowShape);
		            envLevel = preResetLevel * (1f - shaped);
		        } else if (resetPos == halfWindowSamples) {
		            envLevel = 0f;
		        } else {
		            float t = (float)(resetPos - halfWindowSamples) / (float)halfWindowSamples;
		            float shaped = (float)Math.pow(t, windowShape);
		            envLevel = amplitude * shaped;
		        }
		        resetPos++;
		        if (resetPos >= resetTotal) {
		            envLevel = amplitude;
		            resetPos = 0;
		            resetTotal = 0;
		        }
		    }

		    float ampFade = envLevel;
		    float adjustedUpper = RATE_UPPER * (1f + 0.15f * resetPhaseOffset);
		    float adjustedLower = RATE_LOWER * (1f - 0.15f * resetPhaseOffset);

		    if (rate > adjustedUpper * 0.5f) {
		        ampFade = envLevel * (adjustedUpper - rate) / (adjustedUpper * 0.5f);
		        ampFade = Math.max(0f, ampFade);
		    }

		    if (rate < adjustedLower * 2f)
		        ampFade *= rate / (adjustedLower * 2f);

		    if (rate > adjustedUpper) {
		        rate *= 0.125f;
		        phase *= 0.125f;
		    } else if (rate < adjustedLower) {
		        rate *= 8f;
		        phase *= 8f;
		        if (phase > WAVETABLE_size)
		            phase -= WAVETABLE_size;
		    }

		    phase += rate;
		    if (phase >= WAVETABLE_size)
		        phase -= WAVETABLE_size;

		    int idx1 = (int)phase;
		    int idx2 = (idx1 + 1) % WAVETABLE_size;
		    float frac = phase - idx1;
		    float osample = (1f - frac) * WAVETABLE[idx1] + frac * WAVETABLE[idx2];

		    // Apply stage envelope scaling
		    float stageScalar = getStageEnvelope();
		    return ampFade * osample * stageScalar;
		}

		private void updateEnvelope() {
		    stageCounter++;
		    if (stageCounter >= stageSamples) {
		        stageCounter = 0;
		        switch (stage) {
		            case ATTACK:
		                stage = EnvelopeStage.SUSTAIN;
		                stageSamples = Integer.MAX_VALUE; // sustain indefinitely
		                break;
		            case SUSTAIN:
		                stage = EnvelopeStage.RELEASE;
		                stageSamples = releaseSamples;
		                break;
		            case RELEASE:
		                stage = EnvelopeStage.SILENCE;
		                stageSamples = silenceSamples;
		                break;
		            case SILENCE:
		                stage = EnvelopeStage.ATTACK;
		                stageSamples = attackSamples;
		                break;
		        }
		    }
		}

		private float getStageEnvelope() {
		    switch (stage) {
		        case ATTACK:
		            return (float)stageCounter / attackSamples;
		        case SUSTAIN:
		            return 1f;
		        case RELEASE:
		            return 1f - ((float)stageCounter / releaseSamples);
		        case SILENCE:
		            return 0f;
		        default:
		            return 0f;
		    }
		}
	}


}
