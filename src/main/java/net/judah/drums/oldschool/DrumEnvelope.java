package net.judah.drums.oldschool;

import judahzone.api.AtkDec;

public class DrumEnvelope {

	private final AtkDec ad;
	private int atk, dk;

	public DrumEnvelope(AtkDec ad) {
		this.ad = ad;
	}

	public void reset() {
		atk = 0;
		dk = ad.getDecay();
	}

	public float calcEnv() {
		if (atk < ad.getAttack()) {
			return ++atk / (float)ad.getAttack();
		}
		else {
			if (dk > ad.getDecay())
				dk = ad.getDecay();

			if (dk > 0) // TODO percentage of sample length
				return --dk / (float)ad.getDecay();
			else
				return 0f;
		}
	}

	/**
	 * @param cc
	 * @return true if the message is filtered by this envelope
	 */
/*	TODO from legacy KitSetup
 *
 * public boolean cc(ShortMessage cc) {
		if (!Midi.isCC(cc))
			return false;

		int data1 = cc.getData1();
		if (data1 == ControlChange.ATTACK.data1) {
			int val = (int) (cc.getData2() * Constants.TO_100);
			for (int i = 0; i < LENGTH; i++)
				atk[i] = val;
		}
		else if (data1 == ControlChange.DECAY.data1 || data1 == ControlChange.RELEASE.data1) { // DK2
			int val = (int) (cc.getData2() * Constants.TO_100);
			for (int i = 0; i < LENGTH; i++)
				dk[i] = val;
		} else
			return false;
		MainFrame.update(this);
		return true;
	} */


}
