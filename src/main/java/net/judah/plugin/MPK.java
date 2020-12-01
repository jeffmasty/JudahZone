package net.judah.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import lombok.extern.log4j.Log4j;
import net.judah.midi.Midi;

/* TODO upJoystick, downJoystick
cc codes 14-63,75-83, 85-90, 96-127
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
@Log4j
public class MPK {

	public static final String NAME = "MPKmini2"; // capture

	public static final int KNOB_COUNT = 8;
	public static final int PAD_COUNT = 8;
	public static final int KNOB_STYLES = 3;
	public static final int PAD_STYLES = 4;
	public static final int GREEN_BANK = 0;
	public static final int RED_BANK = 1;
	public static final int ABANK = 0;
	public static final int BBANK = 1;

	// Not MPK 
	private static final List<Integer> pedal = Arrays.asList(new Integer[] 
			{96, 97, 98, 99, 100, 101});
	
	private static final List<Integer> drumsA = Arrays.asList(new Integer[] 
		{48, 49, 50, 51, 44, 45, 46, 47});
	private static final List<Integer> drumsB = Arrays.asList(new Integer[]
			{36, 37, 38, 39, 32, 33, 34, 35});
	
	/** handles Prog Select 0 */
	public static String format(Midi midi) {
		if (midi == null) return "null";
		int val = midi.getData1();
		
		if (Midi.isNote(midi)) {
			if (drumsA.contains(val))
				return "Drums " + drumsA.indexOf(val) + quote(midi);
			else if (drumsB.contains(val))
				return "DrumB " + drumsB.indexOf(val) + quote(midi);
		}
		else if (Midi.isCC(midi)) {
			if (pedal.contains(val))
				return "Foot" + pedal.indexOf(val) + quote(midi);
			else if (knobs0.contains(val))
				return "Knob  " + knobs0.indexOf(val) + quote(midi);
			else if (knobs1.contains(val))
				return "Knob  " + knobs0.indexOf(val) + quote(midi) + " Bank1";
			else if (primaryCC.contains(val))
				return "CC Pad " + primaryCC.indexOf(val) + quote(midi);
		}
		int prog = whichProgPad(midi);
		if (prog >= 0) {
			return "P-Pad " + prog + quote(midi);
		}
		return midi.toString();
	}
	
	private static final List<Integer> knobs0 = Arrays.asList(new Integer[] 
			{48, 49, 50, 51, 44, 45, 46, 47});
	private static final List<Integer> knobs1 = Arrays.asList(new Integer[]
			{22, 23, 24, 25, 26, 27, 28, 29});
	private static final List<Integer> knobs2 = Arrays.asList(new Integer[]
			{75, 76, 77, 78, 79, 80, 81, 82});
	private static final List<Integer> knobs3 = Arrays.asList(new Integer[]
			{30, 83, 126, 7, 84, 94, 95,  8});
	
	@SuppressWarnings("unchecked")
	private static final List<Integer>[] knobbanks = new List[] {knobs0, knobs1, knobs2, knobs3};
	
	private static ArrayList<ShortMessage[]> knobs = genKnobs( knobbanks );
	
	public static ShortMessage knob(int bank, int knobNum) {
		return knobs.get(bank)[knobNum];
	}
	public static ShortMessage knob(int knobNum) {
		return knob(0, knobNum);
	}
	
	private static final List<Integer> primaryCC = Arrays.asList(new Integer[]{31, 32, 33, 34, 35, 36, 37, 38});
	
	private static int [][][] ccpads = {
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

	private static int [][][] progchanges = {
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
			log.error(e.getMessage(), e);
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
					if (progchanges[bank][progLvl][padNum] == data1)
						return padNum;
		return -1;
	}

	public static boolean isCCPad(Midi msg, int CCpad) {
		int data1 = msg.getData1();
		for (int ab = 0; ab < 2; ab++)
			for (int padLvl = 0; padLvl < PAD_STYLES; padLvl++)
				if (ccpads[ab][padLvl][CCpad] == data1)
					return Midi.isCC(msg);
		return false;
	}

	private static String quote(Midi midi) {
		return " (" + midi + ")";
	}


}
///** @return null if the msg was consumed (possibly firing off other events) or pass the midi message back as bytes*/
//public ShortMessage process(ShortMessage msg) throws InvalidMidiDataException {
//
//	// if (fluid != null) return fluidSynth(msg); // fluid = master.getFluidsynth();
//	final int dat1 = msg.getData1();
//	final int dat2 = msg.getData2();
//	if (msg.getStatus() == ShortMessage.NOTE_ON || msg.getStatus() == ShortMessage.NOTE_OFF)
//		return new Midi(msg.getCommand(), msg.getChannel(), dat1, dat2);
//
//	// int i = (byte & 0xFF);
//	// Use RTDebug System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));
//
//	return new ShortMessage(msg.getCommand(), msg.getChannel(), dat1, dat2);
//}
//	/** @return the knob index number (0 to 7 from top left) or -1 if not a knob */
//	public static int isKnob(ShortMessage msg) {
//		if (!isCC(msg)) return 0;
//		int data1 = msg.getData1();
//
//		for (int i = 0; i < KNOB_STYLES; i++)
//			for (int j = 0; j < KNOB_COUNT; j++)
//				if (data1 == knobs[i][j]) return j;
//		return -1;
//	}
//	public static boolean isBankUp(Midi msg) {
//		int progChange = whichProgPad(msg);
//		if (progChange < 0 || progChange > 3) return false;
//		return msg.getData1() == green[progChange - 1][BANK_UP];
//	}
//	public static boolean isBankDown(Midi msg) {
//		int progChange = whichProgPad(msg);
//		if (progChange < 0 || progChange > 3) return false;
//		return msg.getData1() == green[progChange - 1][BANK_DOWN];
//	}
//	public static boolean isInstrumentUp(Midi msg) {
//		int cmd = msg.getCommand();
//		if (cmd >= ShortMessage.PROGRAM_CHANGE && cmd <= 208) {
//			int data1 = msg.getData1();
//			for (int ab = 0; ab < 2; ab++) 
//				for (int i = 0; i < PAD_STYLES; i++) 
//					if (progchanges[ab][i][INSTRUMENT_UP] == data1)
//						return true;
//		}	
//		return false;
//	}
//	public static boolean isKnobType(Midi msg, int knobType) {
//		int data1 = msg.getData1();
//		for (int i = 0; i < KNOB_STYLES; i++)
//			if (knobs[i][knobType] == data1)
//				return isCC(msg);
//		return false;
//	}

//	public static boolean isInstrumentDown(Midi msg) {
//		int cmd = msg.getCommand();
//		if (cmd >= ShortMessage.PROGRAM_CHANGE && cmd <= 208) {
//			int data1 = msg.getData1();
//			for (int ab = 0; ab < 2; ab++) 
//				for (int i = 0; i < PAD_STYLES; i++) 
//					if (progchanges[ab][i][INSTRUMENT_DOWN] == data1)
//						return true;
//		}
//		return false;
//	}
//	private ShortMessage processDR5(ShortMessage msg) {
//		if (isCC(msg)) {
//			// System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));
//			if (dat1 == DR5_PADS.DR5_SWITCH) // toggle to dr-5/fluid
//			if (dat1 == DR5_PADS.DRUMS && dat2 > 0) {
//				log.info("Setting Drums track");
//				dr5_channel = DR5.DRUM_CHANNEL;
//				return null;
//			}
//			if (dat1 == DR5_PADS.CHORDS && dat2 > 0) {
//				log.info("Setting Track 1");
//				dr5_channel = DR5.CHORDS_CHANNEL;
//				return null;
//			}
//			if (dat1 == DR5_PADS.BASS && dat2 > 0) {
//				log.info("Setting Track 2");
//				dr5_channel = DR5.BASS_CHANNEL;
//				return null;
//			}
//			if (dat1 == DR5_PADS.AUX && dat2 > 0) {
//				log.info("Setting Track 3");
//				dr5_channel = DR5.AUX_CHANNEL;
//				return null;
//			}
//		}
//		if (isProgramChange(msg)) {
//			log.info("Program Change, yo");
//			int padNum = whichProgPad(msg);
//			if (padNum >= 0 && padNum < PAD_COUNT) {
//			}
//		}
//		return null;
//	}

//	// FLUID
//	static final int REVERB_KNOB = 0;
//	static final int ROOM_SIZE_KNOB = 1;
//	static final int DAMPNESS_KNOB = 2;
//	static final int VOLUME_KNOB = 3;
//	static final int CHORUS_DELAYLINES_KNOB = 4;
//	static final int CHORUS_LEVEL_KNOB = 5;
//	static final int CHORUS_SPEED_KNOB = 6;
//	static final int CHORUS_DEPTH_KNOB = 7;
//	static final int INSTRUMENT_UP = 3;
//	static final int INSTRUMENT_DOWN = 7;
//	static final int BANK_UP = 2;
//	static final int BANK_DOWN = 6;
//	static final int PRESET1 = 0;
//	static final int PRESET2 = 4;
//	static final int TOGGLE_REVERB = 0;
//	static final int TOGGLE_CHORUS = 4;
//	private Midi processFluid(Midi msg) {
//		// if (fluid != null) // fluid = master.getFluidsynth();
//		if (msg.getStatus() == ShortMessage.NOTE_ON || msg.getStatus() == ShortMessage.NOTE_OFF) return msg;
//		// int i = (int)(byte & 0xFF)
//		//System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));
////		if (isCC(msg)) {
////			int knob = isKnob(msg);
////			if (knob == VOLUME_KNOB) {
////				fluid.gain(msg.getData2() * 0.05f);
////				return null;
////			}
////        	if (isKnobType(msg, REVERB_KNOB)) {
////        		fluid.reverb(toFloat(msg.getData2()));
////        		return null;
////        	}
////        	if (isKnobType(msg, ROOM_SIZE_KNOB)) {
////        		fluid.roomSize(toFloat(msg.getData2()));
////        		return null;
////        	}
////        	if (isKnobType(msg, DAMPNESS_KNOB)) {
////        		fluid.dampness(toFloat(msg.getData2()));
////        		return null;
////        	}
////        	if (isKnobType(msg, CHORUS_DELAYLINES_KNOB)) {
////        		fluid.chorusDelayLines((int) (msg.getData2() * 0.99f));
////        		return null;
////        	}
////        	if (isKnobType(msg, CHORUS_LEVEL_KNOB)) {
////        		fluid.chorusLevel(msg.getData2() / 80f);
////        		return null;
////        	}
////        	if (isKnobType(msg, CHORUS_SPEED_KNOB)) {
////        		// 0.3 to 5
////        		fluid.chorusSpeed(((msg.getData2() + 0.61f) * 0.0496f) );
////        		return null;
////        	}
////        	if (isKnobType(msg, CHORUS_DEPTH_KNOB)) {
////        		fluid.chorusDepth((int)(msg.getData2() * 0.42f));
////        		return null;
////        	}
////        	if (isCCPad(msg, TOGGLE_REVERB)) 
////        		fluid.toggle(Constants.Toggles.REVERB, msg.getData2() > 0);
////        	if (isCCPad(msg, TOGGLE_CHORUS)) {
////        		fluid.toggle(Constants.Toggles.CHORUS, msg.getData2() > 0);
////		}
////		if (isProgramChange(msg)) {
////			int padNum = whichProgPad(msg);
////			if (padNum >= 0 && padNum < PAD_COUNT) {
////	        	if (isInstrumentUp(msg))
////	        		return fluid.instrumentUp();
////	        	if (isInstrumentDown(msg))
////	        		return fluid.instrumentDown();
////	        	if (padNum == PRESET1)
////	        		return fluid.preset(1);
////	        	if (padNum == PRESET2)
////	        		return fluid.preset(2);
////			}
////		}
////        	if (isBankUp(msg))
////        		return fluid.bankUp();
////        	if (isBankDown(msg))
////        		return fluid.bankDown();
//		return msg;
//	}
//	private float toFloat(int val) {
//		if (val > 100) val = 100;
//		return val / 100f;
//	}

