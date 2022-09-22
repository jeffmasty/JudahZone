package net.judah.synth;

import lombok.Data;

@Data
public class Adsr {
	
	/** milliseconds   */ int 	attackTime = 1;
	/** milliseconds   */ int 	decayTime = 2;
	/** 0 <= gain <= 1 */ float sustainGain = 0.8f;
	/** milliseconds   */ int 	releaseTime = 2;
	/** 0 <= gain <= 1 */ float attackGain = 1; 
	
}
