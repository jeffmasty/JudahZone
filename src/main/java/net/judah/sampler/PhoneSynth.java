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

import judahzone.data.Asset;
import judahzone.gui.Gui;
import judahzone.gui.Updateable;
import judahzone.jnajack.BasicPlayer;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;


/**DTMF tone generator with button press and visual feedback.
 * Ensures each trigger is exactly DURATION_MS long and includes an
 * attack and release window. Overlapping triggers queue the next tone
 * and wait for the current release to complete before starting the queued tone.
 * Also includes a "ringtone" button that plays a pre-recorded Phone.wav sample.*/
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
		SampleDB.loadAsync(RING, 0.4f, this);
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
		// volume? not implemented
	}

}

/*
Wikipedia: Dual-tone multi-frequency (DTMF) signaling is a telecommunication signaling system using the voice-frequency band over telephone lines between telephone equipment and other communications devices and switching centers.[1] DTMF was first developed in the Bell System in the United States,[2][3] and became known under the trademark Touch-Tone for use in push-button telephones, starting in 1963. The DTMF frequencies are standardized in ITU-T Recommendation Q.23.[4] The signaling system is also known as MF4 in the United Kingdom, as MFV in Germany, and Digitone in Canada.

Touch-tone dialing with a telephone keypad gradually replaced the use of rotary dials and has become the industry standard in telephony to control equipment and signal user intent.[5] The signaling on trunks in the telephone network uses a different type of multi-frequency signaling.

a mixture of two pure tone (pure sine wave) sounds.
The DTMF system uses two sets of four frequencies in the voice frequency range transmitted in pairs to represent sixteen signals, representing the ten digits and six additional signals identified as the letters A to D, and the symbols # and *. As the signals are audible tones, they can be transmitted through line repeaters and amplifiers, and over radio and microwave links.

The DTMF telephone keypad is laid out as a matrix of push buttons in which each row represents the low-frequency component and each column represents the high-frequency component of the DTMF signal. The commonly used keypad has four rows and three columns, but a fourth column is present for some applications. Pressing a key sends a combination of the row and column frequencies. For example, the 1 key produces a superimposition of a 697 Hz low tone and a 1209 Hz high tone. Initial pushbutton designs employed levers, enabling each button to activate one row and one column contact. The tones are decoded by the switching center to determine the keys pressed by the user.
*/