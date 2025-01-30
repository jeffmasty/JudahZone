package net.judah.drumkit;

public class DrumEnvelope {

	private final AtkDec ad;
	private int attack, decay;

	public DrumEnvelope(AtkDec ad) {
		this.ad = ad;
	}

	public void reset() {
		attack = 0;
		decay = ad.getDecayTime();
	}

	public float calcEnv() {
		if (attack < ad.getAttackTime()) {
			return ++attack / (float)ad.getAttackTime();
		}
		else {
			if (decay > ad.getDecayTime())
				decay = ad.getDecayTime();

			if (decay > 0)
				return --decay / (float)ad.getDecayTime();
			else
				return 0f;
		}
	}

}
