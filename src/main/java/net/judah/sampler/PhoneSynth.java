package net.judah.sampler;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import judahzone.api.Asset;
import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.WavConstants;


/**DTMF tone generator with button press and visual feedback.
 * Ensures each trigger is exactly DURATION_MS long and includes an
 * attack and release window. Overlapping triggers queue the next tone
 * and wait for the current release to complete before starting the queued tone. */
public class PhoneSynth extends JPanel implements Updateable, Consumer<Asset> {

	private static final float[] LOW_FREQS = {697f, 770f, 852f, 941f};
	private static final float[] HIGH_FREQS = {1209f, 1336f, 1477f, 1633f};
	private static final String[] KEYPAD = {"123", "456", "789", "*0#"};
	private static final int[] MY_DIGITS = {0, 1, 3, 4, 7, 8, 9};
	private static final int DURATION_MS = 333;
	private static final float RAMP_TIME_MS = 20f;
	private static final Color PLAYING_COLOR = new Color(144, 238, 144); // light green

	private static final File RING = new File(Folders.getSamples(), "Phone.wav");

	private final int SR;
	private final Sampler sampler;
	private final int SAMPLES; // computed from SR

	private float amplitude = 0.1f;
	private int sampleCount = 0;
	private volatile float lowFreq;
	private volatile float highFreq;
	private float lowPhase;
	private float highPhase;
	private volatile boolean isPlaying;
	private volatile boolean isReleasing = false;
	private int releaseSampleCount = 0;
	private volatile float queuedLowFreq;
	private volatile float queuedHighFreq;
	private volatile boolean hasPendingTone = false;

	private volatile char currentKey = '\0';
	private volatile char queuedKey = '\0';

	private final Map<Character, JButton> buttonMap = new HashMap<>();

	private BasicPlayer ringtone; // ONE_SHOT: Phone.wav (ringtone)
	private Asset asset;


	public PhoneSynth(int sampleRate, Sampler sampler) {
		this.SR = sampleRate;
		this.sampler = sampler;
		this.SAMPLES = (int)((DURATION_MS / 1000f) * SR);

		SwingUtilities.invokeLater(()->gui());
		// Preload Phone.wav (ringtone)
		SampleDB.loadAsync(RING, WavConstants.RUN_LEVEL, this);
	}

	private void gui() {
		JButton ringring = new JButton("   ringtone   ");
		ringring.setFont(Gui.BOLD18);
		ringring.addActionListener(e -> ringtone());

		JPanel content = new JPanel(new GridLayout(4, 3, 5, 4));
		for (int r = 0; r < KEYPAD.length; r++) {
			for (int c = 0; c < KEYPAD[r].length(); c++) {
				char key = KEYPAD[r].charAt(c);
				JButton btn = new JButton(String.valueOf(key));
				btn.setFont(Gui.BOLD18);
				buttonMap.put(key, btn);
				btn.addMouseListener(new MouseAdapter() {
					@Override public void mousePressed(MouseEvent e) {  trigger(key); }
				});
				content.add(btn);
			}
		}

		Box content2 = Box.createVerticalBox();
		content2.add(Box.createVerticalStrut(10));
		content2.add(content);
		content2.add(Box.createVerticalStrut(8));
		content2.add(Gui.box(ringring));
		content2.add(Box.createVerticalStrut(8));

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		int strut = 50;
		add(Box.createHorizontalStrut(strut));
		add(content2);
		add(Box.createHorizontalStrut(strut));
	}

	/** Dial a digit: play the tone (exactly DURATION_MS). */
	public void dial(int i) {
		if (i < 0 || i >= MY_DIGITS.length) {
			ringtone();
			return;
		}
		char key = Character.forDigit(MY_DIGITS[i], 10);
		trigger(key);
	}

	public void setAmplitude(float amp) {
		this.amplitude = Math.max(0f, Math.min(1f, amp));
	}

	public void ringtone() {
		if (ringtone == null) {
			try {
				if (asset == null)
					throw new IllegalStateException("ringtone not set: " + RING.getAbsolutePath());
				ringtone = new BasicPlayer();
				ringtone.setRecording(asset);
			} catch (Exception e) { RTLogger.warn(this, e); }
		}
		sampler.add(ringtone).play(true);
	}

	@Override
	public void accept(Asset t) {
		this.asset = t;
	}

	@Override
	public void update() {
		for (Map.Entry<Character, JButton> entry : buttonMap.entrySet()) {
			char key = entry.getKey();
			JButton btn = entry.getValue();

			boolean isPlayingThisTone = isPlaying && !isReleasing && currentKey == key;
			btn.setBackground(isPlayingThisTone ? PLAYING_COLOR : null);
			btn.setOpaque(isPlayingThisTone);
		}
	}

	private void trigger(char key) {
		int row = -1, col = -1;
		for (int r = 0; r < KEYPAD.length; r++)
			if (KEYPAD[r].indexOf(key) >= 0) {
				row = r;
				col = KEYPAD[r].indexOf(key);
				break;
			}

		if (row < 0 || col < 0)
			return;

		float requestedLow = LOW_FREQS[row];
		float requestedHigh = HIGH_FREQS[col];

		// If nothing is playing, start immediately.
		if (!isPlaying) {
			startTone(requestedLow, requestedHigh, key);
			return;
		}

		// If playing and not already releasing, initiate release and queue the new tone.
		if (isPlaying && !isReleasing) {
			isReleasing = true;
			releaseSampleCount = 0;
			queuedLowFreq = requestedLow;
			queuedHighFreq = requestedHigh;
			queuedKey = key;
			hasPendingTone = true;
			return;
		}

		// If currently releasing, update the queued tone (do not start it yet).
		if (isReleasing) {
			queuedLowFreq = requestedLow;
			queuedHighFreq = requestedHigh;
			queuedKey = key;
			hasPendingTone = true;
		}
	}

	private void startTone(float low, float high, char key) {
		lowFreq = low;
		highFreq = high;
		sampleCount = 0;
		lowPhase = 0f;
		highPhase = 0f;
		isPlaying = true;
		isReleasing = false;
		hasPendingTone = false;
		releaseSampleCount = 0;
		currentKey = key;
		queuedKey = '\0';
	}

	@SuppressWarnings("unused")
	private void initiateRelease() {
		if (isPlaying && !isReleasing) {
			isReleasing = true;
			releaseSampleCount = 0;
			// keep any queued tone already set
		}
	}

	protected void process(float[] sumLeft, float[] sumRight) {
		if (!isPlaying)
			return;

		float rampSamples = (RAMP_TIME_MS / 1000f) * SR;
		if (rampSamples < 1f) rampSamples = 1f; // avoid divide by zero

		float lowCyclesPerSample = lowFreq / SR;
		float highCyclesPerSample = highFreq / SR;

		for (int i = 0; i < Constants.bufSize(); i++) {
			float env;

			// Recompute cycles per sample in case frequencies were changed by a queued start this sample.
			lowCyclesPerSample = lowFreq / SR;
			highCyclesPerSample = highFreq / SR;

			if (isReleasing) {
				env = 1f - (releaseSampleCount / rampSamples);
				if (env < 0f) env = 0f;
				releaseSampleCount++;

				// Release finished: either start queued tone or stop.
				if (env <= 0f) {
					isReleasing = false;
					releaseSampleCount = 0;
					isPlaying = false;
					currentKey = '\0';

					if (hasPendingTone) {
						// Start queued tone immediately (attack begins next sample).
						lowFreq = queuedLowFreq;
						highFreq = queuedHighFreq;
						sampleCount = 0;
						lowPhase = 0f;
						highPhase = 0f;
						isPlaying = true;
						hasPendingTone = false;
						currentKey = queuedKey;
						queuedKey = '\0';

						// Recompute cycles for new tone and ensure first sample's env is 0 (attack starts).
						lowCyclesPerSample = lowFreq / SR;
						highCyclesPerSample = highFreq / SR;
						env = 0f;
					} else {
						// no queued tone, stop producing.
						return;
					}
				}
			} else {
				// Normal (non-releasing) envelope: attack, sustain, release tail inside SAMPLES.
				if (sampleCount < rampSamples)
					env = sampleCount / rampSamples;
				else if (sampleCount >= SAMPLES - rampSamples)
					env = Math.max(0f, (SAMPLES - sampleCount) / rampSamples);
				else
					env = 1f;
			}

			float low = (float)Math.sin(2f * Math.PI * lowPhase);
			float high = (float)Math.sin(2f * Math.PI * highPhase);
			float sample = env * (low + high) * amplitude;
			sumLeft[i] += sample;
			sumRight[i] += sample;

			lowPhase = (lowPhase + lowCyclesPerSample) % 1f;
			highPhase = (highPhase + highCyclesPerSample) % 1f;

			// Advance sampleCount only when not releasing (release has its own counter).
			if (!isReleasing)
				sampleCount++;

			// If we reached the configured duration, stop or start queued tone on next sample.
			if (!isReleasing && sampleCount >= SAMPLES) {
				isPlaying = false;
				currentKey = '\0';
				if (hasPendingTone) {
					lowFreq = queuedLowFreq;
					highFreq = queuedHighFreq;
					// prepare the queued tone to start on the next sample (attack from 0)
					sampleCount = 0;
					lowPhase = 0f;
					highPhase = 0f;
					isPlaying = true;
					hasPendingTone = false;
					currentKey = queuedKey;
					queuedKey = '\0';
					// next loop iteration will use the new lowFreq/highFreq and env starts at 0
				} else {
					return;
				}
			}
		}
	}

	public void doKnob(int idx, int value) {
		// volume?
	}

}