package net.judah.synth;

import lombok.Data;

@Data
public class Adsr {
	
	/** milliseconds */ int 	attackTime = 1;
	/** milliseconds */ int 	decayTime = 3;
	/** 0 < gain < 1 */ float 	sustainGain = 0.6f;
	/** milliseconds */ int 	releaseTime = 5;
	/** 0 < gain < 1 */ float 	attackGain = 0.7f; // dampen
	
}
