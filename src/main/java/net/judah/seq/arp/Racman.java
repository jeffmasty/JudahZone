package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.Setter;
import net.judah.api.Algo;
import net.judah.api.Chord;
import net.judah.seq.Poly;

/** Original: https://github.com/ybalcanci/Sequence-Player */
public class Racman extends Algo implements Ignorant {

	private int idx;
	private int numElements = 128;
	private int[] sequence;
	private int[] noteSequence;
	@Setter private int modulus = 48;

	public Racman() {
		this(104, 60); 
	}
	
	public Racman(int numOfElements) {
		this(numOfElements, numOfElements);
	}
	
	/**@param numOfElementsToCalculate number of elements to calculate in the sequence.
	 * @param modulusOfElementsConvertingToNote modulus value for each element in the 
	 * noteSequence will be modulo of the corresponding value in sequnce.*/
	public Racman(int numOfElementsToCalculate, int modulusOfElementsConvertingToNote){
		this.numElements = numOfElementsToCalculate;
		this.modulus = modulusOfElementsConvertingToNote;
		calculateSequence();
	}

	private void calculateSequence() {
		sequence = new int[numElements];
		noteSequence = new int[numElements];

		sequence[0] = 0;
		for(int i = 1; i < numElements; i++){
			int currentNumberNeg = sequence[i - 1] - i;
			if(currentNumberNeg <= 0 || isANumberUsed(currentNumberNeg, i))
				sequence[i] = sequence[i - 1] + i;
			else
				sequence[i] = currentNumberNeg;
			noteSequence[i] = sequence[i] % modulus;
		}
	}

	private boolean isANumberUsed(int num, int currentArraySize){
		for (int i = 0; i < currentArraySize; i++)
			if (sequence[i] == num)
				return true;
		return false;
	}
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		int note = noteSequence[idx] + m.getData1();
		if (++idx == numElements)
			idx = 0;
		while (note > range + m.getData1() || note > 127)
			note -= modulus;
		while (note < m.getData1())
			note += 12;
		result.add(note);
	}

}
