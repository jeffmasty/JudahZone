package net.judah.synth;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**Performs an envelope's life cycle per Jack frame based on supplied Adsr settings.
 * 
 * inspired by: https://github.com/michelesr/jack-oscillator/blob/master/src/lib/synth.c*/
@RequiredArgsConstructor
public class JackEnvelope {
	
	/** 0 to 1 will decrease over-all volume, above 1 would amplify */
	@Getter @Setter private float dampen = 0.25f; // wave shapes generated non-duty
	private int attack, decay, release;
	private float result; // class-level for peaceful transition from sustain to release
	
	public void reset() {
		attack = 0; 
		decay = Integer.MAX_VALUE;
		release = Integer.MAX_VALUE; 
	}

	/** sensitive to adsr, velocity, and dampen */
	public float calcEnv(Polyphony notes, int idx, Adsr adsr) {
		ShortMessage voice = notes.getNotes()[idx];
		if (voice == null) 
			return 0f;
		if (voice.getCommand() == ShortMessage.NOTE_ON) { 
			// note is pressed, perform either of A, D or S
			if (attack < adsr.attackTime) {
				result = ++attack / (float)adsr.attackTime;
			}
			else {
				if (decay > adsr.decayTime) 
					decay = adsr.decayTime;
				if (decay > 0) {
					result = --decay / (float)adsr.decayTime * (1 - adsr.sustainGain) + adsr.sustainGain; 
				} else 
					result = adsr.sustainGain;
			}
		} 
		else { // perform Release
			if (adsr.releaseTime == 0) // changed during life-cycle
				result = 0;
			else {
				if (release > adsr.releaseTime)
					release = adsr.releaseTime; // refresh from model
				if (release > 0) {
					result = --release / (float)adsr.releaseTime * result;
				}
				else 
					result = 0; // note complete
			}
		}
		if (result <= 0)
			return silence(notes, voice, idx);
		return result * dampen * (voice.getData2() * 0.007874f); // data2/127 velocity
	}

	private float silence(Polyphony notes, ShortMessage voice, int idx) {
		if (voice.getCommand() == ShortMessage.NOTE_OFF)
			notes.getNotes()[idx] = null; 
		// else, user is fiddling with sustain to 0, while note pressed
		return 0f;
	}

}
