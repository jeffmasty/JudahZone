package net.judah.synth.taco;

import javax.sound.midi.ShortMessage;

import judahzone.util.Constants;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**Performs an envelope's life cycle based on supplied Adsr settings.
 *
 * inspired by: https://github.com/michelesr/jack-oscillator/blob/master/src/lib/synth.c
 * Under the GNU License. Copyright (C) 2014-2015 Michele Sorcinelli */
@RequiredArgsConstructor
public class Envelope {
	private final int SAMPLE_RATE = Constants.sampleRate();

	@Getter private final Adsr adsr;
	/** reverse-amplification factor to dampen (or, I guess, amplify) all output */
	@Getter @Setter private float dampen = 0.5f;
	private float result; // class-level for peaceful transition from sustain to release

	private float attack, decay, sustain, release;

	public void reset() {
		attack = adsr.attackTime > 0 ?
				0 : adsr.attackGain;
		decay = adsr.decayTime > 0 ?
				adsr.attackGain : adsr.sustainGain;
		sustain = adsr.sustainGain;
		release = adsr.releaseTime > 0 ?
				adsr.sustainGain : -1f;
	}

	public float calcEnv(boolean pressed) {
		if (pressed) { // note is down, perform either of A/D or S
			if (attack < adsr.attackGain && adsr.attackTime > 0) {
				attack += (adsr.attackGain / (SAMPLE_RATE * adsr.attackTime / 1000));
				result = attack;
			}
			else if ((decay > sustain) && (sustain <= adsr.attackGain) && adsr.decayTime > 0) {
				decay -= (adsr.attackGain - adsr.sustainGain) / (SAMPLE_RATE * adsr.decayTime / 1000);
				result = decay;
			}
			else {
				result = adsr.sustainGain;
			}
		}
		else { // perform Release
			if (adsr.releaseTime == 0) // changed during life-cycle
				return 0f;
			if (release > result)
				release = result;
			if (release > 0) {
				release -= ( (adsr.sustainGain) / (SAMPLE_RATE * adsr.releaseTime / 1000));
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
			if (attack < adsr.attackGain && adsr.attackTime > 0) {
				attack += (adsr.attackGain / (SAMPLE_RATE * adsr.attackTime / 1000));
				result = attack;
			}
			else if ((decay > sustain) && (sustain <= adsr.attackGain) && adsr.decayTime > 0) {
				decay -= (adsr.attackGain - adsr.sustainGain) / (SAMPLE_RATE * adsr.decayTime / 1000);
				result = decay;
			}
			else {
				result = adsr.sustainGain;
			}
		}
		else { // perform Release
			if (adsr.releaseTime == 0) // changed during life-cycle
				result = 0;
			else {
				if (release > result) // transition from note on to note off
					release = result;
				if (release > 0) {
					release -= ( (adsr.sustainGain) / (SAMPLE_RATE * adsr.releaseTime / 1000));
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
