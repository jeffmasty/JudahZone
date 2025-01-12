package net.judah.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import net.judah.midi.Midi;
import net.judah.util.RTLogger;

/* cc codes 14-63,75-83, 85-90, 96-127
           31-63            90,96-127
 cc reserved
 0 - 13 except 3
 5 (Portamento Time), 7 (Volume), 8 (Balance) ,10 (pan), 11 (Expression), 12, 13
 64 sustain
 65 (Portamento On/Off Switch)
 66-69 foot switches
 70 - 74 amp effects 73 attack
 84 (Portamento CC Control)
 91 Reverb effects
 92 tremelo
 93 Chorus effects
 94 detuner
 95 phaser */
public interface MPKTools {

	public static final int thresholdLo = 1;
	public static final int thresholdHi = 98;

	public static final int KNOB_COUNT = 8;
	public static final int PAD_COUNT = 8;
	public static final int KNOB_STYLES = 3;
	public static final int PAD_STYLES = 4;
	public static final int GREEN_BANK = 0;
	public static final int RED_BANK = 1;
	public static final int ABANK = 0;
	public static final int BBANK = 1;
	public static final byte[] SUSTAIN_ON = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 127).getMessage();
	public static final byte[] SUSTAIN_OFF = Midi.create(Midi.CONTROL_CHANGE, 0, 64, 0).getMessage();
	public static final int JOYSTICK_L = 127;
	public  static final int JOYSTICK_R = 0;


	public static final List<Integer> DRUMS_A = Arrays.asList(new Integer[]
		{48, 49, 50, 51, 44, 45, 46, 47});
	public static final List<Integer> DRUMS_B = Arrays.asList(new Integer[]
			{36, 37, 38, 39, 32, 33, 34, 35});

	public static int drumBank(int data1) {
		for (int i = 0; i < DRUMS_B.size() ; i++)
			if (DRUMS_B.get(i) == data1)
				return 1;
		return 0;
	}

	public static int drumIndex(int data1) {
		for (int i = 0; i < DRUMS_A.size() ; i++)
			if (DRUMS_A.get(i) == data1)
				return i;
			else if (DRUMS_B.get(i) == data1)
				return i;
		return 0;
	}



	/** handles Prog Select 0 */
	public static String format(Midi midi) {
		if (midi == null) return "null";
		int val = midi.getData1();

		if (Midi.isNote(midi)) {
			if (DRUMS_A.contains(val))
				return "Drums " + DRUMS_A.indexOf(val) + quote(midi);
			else if (DRUMS_B.contains(val))
				return "DrumB " + DRUMS_B.indexOf(val) + quote(midi);
		}
		else if (Midi.isCC(midi)) {
				if (KNOBS.contains(val))
				return "Knob  " + KNOBS.indexOf(val) + quote(midi);
			else if (knobs1.contains(val))
				return "Knob  " + knobs1.indexOf(val) + quote(midi) + " Bank1";
			else if (PRIMARY_CC.contains(val))
				return "CC Pad " + PRIMARY_CC.indexOf(val) + quote(midi);
		}
		int prog = whichProgPad(midi);
		if (prog >= 0) {
			return "P-Pad " + prog + quote(midi);
		}
		return midi.toString();
	}

	public static final List<Integer> KNOBS = Arrays.asList(new Integer[]
			{14, 15, 16, 17, 18, 19, 20, 21});
	public static final List<Integer> knobs1 = Arrays.asList(new Integer[]
			{22, 23, 24, 25, 26, 27, 28, 29});
	public static final List<Integer> knobs2 = Arrays.asList(new Integer[]
			{75, 76, 77, 78, 79, 80, 81, 82});
	public static final List<Integer> knobs3 = Arrays.asList(new Integer[]
			{30, 83, 126, 7, 84, 94, 95,  8});

	@SuppressWarnings("unchecked")
	public static final List<Integer>[] KNOB_BANKS = new List[] {KNOBS, knobs1, knobs2, knobs3};

	public static ArrayList<ShortMessage[]> KNOBS_MIDI = genKnobs( KNOB_BANKS );

	public static ShortMessage knob(int bank, int knobNum) {
		return KNOBS_MIDI.get(bank)[knobNum];
	}
	public static ShortMessage knob(int knobNum) {
		return knob(0, knobNum);
	}


	public static int [][][] CC_PADS = {
			{ // green A bank
				{31, 32, 33, 34, 35, 36, 37, 38},
				{39, 40, 41, 42, 43, 44, 45, 46},
				{47, 48, 49, 50, 51, 52, 53, 54},
				{55, 56, 57, 58, 59, 60, 61, 62}
			},
			{ // red B bank
				{90, 96, 97, 98, 99, 100, 101, 102},
				{103, 104, 105, 106, 107, 108, 109, 110},
				{110, 111, 112, 113, 114, 115, 116, 117},
				{118, 119, 120, 121, 122, 123, 124, 125}
			}
	};
	public static final List<Integer> PRIMARY_CC = Arrays.asList(
	        new Integer[]{31, 32, 33, 34, 35, 36, 37, 38});

	public static final List<Integer> SAMPLES_CC = Arrays.asList(
			new Integer[] {90, 96, 99, 100, 97, 98, 101, 102});

	public static int [][][] PROG_CHANGES = {
			{ 									   // green
				{ 1,  2,  3,  4,  5,  6,  7,  8}, // lvl1
				{ 9, 10, 11, 12, 13, 14, 15, 16}, // lvl2
				{17, 18, 19, 20, 21, 22, 23, 24}, // lvl3
				{25, 26, 27, 28, 29, 30, 31, 32}  // lvl4
			},
			{ 									   // red
				{33, 34, 35, 36, 37, 38, 39, 40}, // lvl1
				{41, 42, 43, 44, 45, 46, 47, 48}, // lvl2
				{49, 50, 51, 52, 53, 54, 55, 56}, // lvl3
				{56, 58, 59, 60, 61, 62, 63, 64}  // lvl4
			}
	};
	public static final int[] PRIMARY_PROG = PROG_CHANGES[0][0];
	public static final int[] B_PROG = PROG_CHANGES[1][0];

	private static ArrayList<ShortMessage[]> genKnobs(List<Integer>[] in) {

		ArrayList<ShortMessage[]> result = new ArrayList<>();
		try {
			for (int i = 0; i < in.length; i++) {
				int length = in[i].size();
				ShortMessage[] program = new ShortMessage[length];
				for (int j = 0; j < length; j++) {
					program[j] = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, in[i].get(j), 0);
				}
				result.add(program);
			}
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(MPKTools.class, e);
		}
	return result;
}

	/** @return 0 to 7 pad of program change or -1 if MPK prog pads did not send this */
	public static int whichProgPad(ShortMessage msg) {
		if (!Midi.isProgChange(msg)) return -1;
		int data1 = msg.getData1();
		for (int bank = 0; bank < 2; bank++)
			for (int progLvl = 0; progLvl < PAD_STYLES; progLvl++)
				for (int padNum = 0; padNum < PAD_COUNT; padNum++)
					if (PROG_CHANGES[bank][progLvl][padNum] == data1)
						return padNum;
		return -1;
	}

	public static boolean isCCPad(Midi msg, int CCpad) {
		int data1 = msg.getData1();
		for (int ab = 0; ab < 2; ab++)
			for (int padLvl = 0; padLvl < PAD_STYLES; padLvl++)
				if (CC_PADS[ab][padLvl][CCpad] == data1)
					return Midi.isCC(msg);
		return false;
	}

	private static String quote(Midi midi) {
		return " (" + midi + ")";
	}

}

