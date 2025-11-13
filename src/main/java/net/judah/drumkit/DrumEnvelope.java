package net.judah.drumkit;

public class DrumEnvelope {

	private final AtkDec ad;
	private int attack, decay;
	private int pad;

	public DrumEnvelope(AtkDec ad, int idx) {
		this.ad = ad;
		this.pad = idx;
	}

	public void reset() {
		attack = 0;
		decay = ad.getDk(pad);
	}

	public float calcEnv() {
		if (attack < ad.getAtk(pad)) {
			return ++attack / (float)ad.getAtk(pad);
		}
		else {
			if (decay > ad.getDk(pad))
				decay = ad.getDk(pad);

			if (decay > 0)
				return --decay / (float)ad.getDk(pad);
			else
				return 0f;
		}
	}

}
