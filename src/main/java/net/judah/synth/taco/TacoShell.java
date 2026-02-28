package net.judah.synth.taco;

import javax.sound.midi.ShortMessage;

import judahzone.data.Stamp;
import judahzone.util.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**Performs an envelope's life cycle based on supplied Adsr settings.
 *
 * inspired by: https://github.com/michelesr/jack-oscillator/blob/master/src/lib/synth.c
 * Under the GNU License. Copyright (C) 2014-2015 Michele Sorcinelli */
@RequiredArgsConstructor
public class TacoShell {
	private static final int SAMPLE_RATE = Constants.sampleRate();
	private static final float NISR = 1f / SAMPLE_RATE * 0.001f; // normalized inverse sample rate, for efficiency

	private static final float ATTACK_TARGET = 1f;

	@Getter private final Stamp adsr;
	/** reverse-amplification factor to dampen (or, I guess, amplify) all output */
	@Getter @Setter private float dampen = 0.5f;
	private float result; // class-level for peaceful transition from sustain to release

	private float attack, decay, sustain, release;

	public void reset() {
		attack = adsr.attack > 0 ?
				0 : ATTACK_TARGET;
		decay = adsr.decay > 0 ?
				ATTACK_TARGET : adsr.sustain;
		sustain = adsr.sustain;
		release = adsr.release > 0 ?
				adsr.sustain : -1f;
	}

	public float calcEnv(boolean pressed) {
		if (pressed) { // note is down, perform either of A/D or S
			if (attack < ATTACK_TARGET && adsr.attack > 0) {
				attack += (ATTACK_TARGET * NISR / adsr.attack);
				result = attack;
			}
			else if ((decay > sustain) && (sustain <= ATTACK_TARGET) && adsr.decay > 0) {
				decay -= (ATTACK_TARGET - adsr.sustain) * NISR / adsr.decay;
				result = decay;
			}
			else {
				result = adsr.sustain;
			}
		}
		else { // perform Release
			if (adsr.release == 0) // changed during life-cycle
				return 0f;
			if (release > result)
				release = result;
			if (release > 0) {
				release -= ( (adsr.sustain) * NISR / adsr.release);
				result = release;
			}
			else { // note complete
				return 0f;
			}
		}
		return result  * dampen;
	}

	public float calcEnv(Polyphony notes, int idx) {
		ShortMessage voice = notes.get(idx);
		if (voice != null && voice.getCommand() == ShortMessage.NOTE_ON) {
			// note is pressed, perform either of A/D or S
			if (attack < ATTACK_TARGET && adsr.attack > 0) {
				attack += (ATTACK_TARGET * NISR / adsr.attack);
				result = attack;
			}
			else if ((decay > sustain) && (sustain <= ATTACK_TARGET) && adsr.decay > 0) {
				decay -= (ATTACK_TARGET - adsr.sustain) * NISR / adsr.decay;
				result = decay;
			}
			else {
				result = adsr.sustain;
			}
		}
		else { // perform Release
			if (adsr.release == 0) // changed during life-cycle
				result = 0;
			else {
				if (release > result) // transition from note on to note off
					release = result;
				if (release > 0) {
					release -= ( (adsr.sustain) * NISR / adsr.release);
					result = release;
				}
				else
					result = 0; // note complete
			}
		}
		if (result <= 0)
			return silence(notes, idx);
		return result * dampen;
	}

	private float silence(Polyphony notes, int idx) {
		notes.removeIndex(idx);
		return 0f;
	}
}
