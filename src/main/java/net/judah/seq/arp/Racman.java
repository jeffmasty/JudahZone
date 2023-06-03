/* see: https://github.com/ybalcanci/Sequence-Player */
package net.judah.seq.arp;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.Setter;
import net.judah.seq.Poly;
import net.judah.seq.chords.Chord;

@Getter
public class Racman extends Algo implements Ignorant {

	private int idx;
	private int numElements = 128;
	private int[] sequence;
	private int[] noteSequence;
	@Setter private int modulus = 48;

	public Racman() {
		this(128, 72); // five octaves
	}
	
	public Racman(int numOfElements) {
		this(numOfElements, numOfElements);
	}
	
	/**@param numOfElementsToCalculate This is the number of elements to calculate in the sequence.
	 * @param modulusOfElementsConvertingToNote This is the modulus value for each element in the noteSequence will
	 *                                          be modulo of the corresponding value in sequnce.
	 * @param noteOffset This value will be added to each value in the sequence while converting
	 *                   them the notes.*/
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
		int[] sequence = getSequence();
		for(int i = 0; i < currentArraySize; i++){
			if(sequence[i] == num)
				return true;
		}
		return false;
	}
	
	@Override
	public void process(ShortMessage m, Chord chord, Poly result) {
		int note = noteSequence[idx] + m.getData1();
		if (++idx == numElements)
			idx = 0;
		while (note > range + m.getData1() || note > 127)
			note -= modulus;
		result.add(note);
	}

}
