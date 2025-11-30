package net.judah.synth.taco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class Adsr {

	/** milliseconds   */ public int    attackTime = 1;
	/** milliseconds   */ public int    decayTime = 2;
	/** 0 <= gain <= 1 */ public float  sustainGain = 0.8f;
	/** milliseconds   */ public int    releaseTime = 2;
	/** 0 <= gain <= 1 */ public float  attackGain = 1;

	public Adsr(Adsr copy) {
		set(copy);
	}

	public void set(Adsr env) {
		attackTime = env.attackTime;
		decayTime = env.decayTime;
		releaseTime = env.releaseTime;
		sustainGain = env.sustainGain;
	}

}
