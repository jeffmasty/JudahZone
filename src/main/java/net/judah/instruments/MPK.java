package net.judah.instruments;

import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import net.judah.JudahZone;
import net.judah.Tab;
import net.judah.midi.Midi;

@SuppressWarnings({"serial", "unused"})
public class MPK extends Tab {

	public static final String NAME = "MPKmini2"; // capture

	public static final int KNOB_COUNT = 8;
	public static final int PAD_COUNT = 8;
	public static final int KNOB_STYLES = 3;
	public static final int PAD_STYLES = 4;
	public static final int GREEN_BANK = 0;
	public static final int RED_BANK = 1;
	public static final int ABANK = 0;
	public static final int BBANK = 1;


	// BOSS DR-5
	private int DR5_CC = 36;
	private static class DR5_PADS {
		static int DR5_SWITCH = 45;
		static int DRUMS = 50;
		static int CHORDS = 51;
		static int BASS = 46;
		static int AUX = 47;
	}
	// private int dr5_channel = DR5.AUX_CHANNEL;


	// FLUID
	static final int REVERB_KNOB = 0;
	static final int ROOM_SIZE_KNOB = 1;
	static final int DAMPNESS_KNOB = 2;
	static final int VOLUME_KNOB = 3;
	static final int CHORUS_DELAYLINES_KNOB = 4;
	static final int CHORUS_LEVEL_KNOB = 5;
	static final int CHORUS_SPEED_KNOB = 6;
	static final int CHORUS_DEPTH_KNOB = 7;

	static final int INSTRUMENT_UP = 3;
	static final int INSTRUMENT_DOWN = 7;
	static final int BANK_UP = 2;
	static final int BANK_DOWN = 6;
	static final int PRESET1 = 0;
	static final int PRESET2 = 4;

	static final int TOGGLE_REVERB = 0;
	static final int TOGGLE_CHORUS = 4;

	/*
		// TODO upJoystick, downJoystick

	channel 10?
	cc codes 14-63,75-83, 85-90, 96-127
	           31-63            90,96-127  */
/* cc reserved
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
	 95 phaser
*/

	private static int[][] knobs = {
			{14, 15, 16, 17, 18, 19, 20, 21},
			{22, 23, 24, 25, 26, 27, 28, 29},
			{75, 76, 77, 78, 79, 80, 81, 82},
			{30, 83, 126, 7, 84, 94, 95, 8}
	};

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


	private final JudahZone master;

//	private FluidSynth fluid;

	public MPK(JudahZone judahzone) {
		master = judahzone;
	}

	/** {@link ShortMessage#PROGRAM_CHANGE} allowing for 15 more channels */
	public static boolean isProgramChange(ShortMessage msg) {
		int cmd = msg.getCommand();
		return (cmd >= ShortMessage.PROGRAM_CHANGE && cmd <= 208);
	}

	/** @return 0 to 7 pad of program change or -1 if MPK prog pads did not send this */
	public static int whichProgPad(ShortMessage msg) {
		int data1 = msg.getData1();
		for (int bank = 0; bank < 2; bank++)
			for (int progLvl = 0; progLvl < PAD_STYLES; progLvl++)
				for (int padNum = 0; padNum < PAD_COUNT; padNum++)
					if (progchanges[bank][progLvl][padNum] == data1)
						return padNum;
		return -1;
	}

	public static boolean isCC(ShortMessage msg) {
		 return msg.getStatus() - msg.getChannel() == ShortMessage.CONTROL_CHANGE;
	}

	/** @return the knob index number (0 to 7 from top left) or -1 if not a knob */
	public static int isKnob(ShortMessage msg) {
		if (!isCC(msg)) return 0;
		int data1 = msg.getData1();

		for (int i = 0; i < KNOB_STYLES; i++)
			for (int j = 0; j < KNOB_COUNT; j++)
				if (data1 == knobs[i][j]) return j;
		return -1;
	}

//	public static boolean isBankUp(Midi msg) {
//		int progChange = whichProgPad(msg);
//		if (progChange < 0 || progChange > 3) return false;
//		return msg.getData1() == green[progChange - 1][BANK_UP];
//	}
//
//	public static boolean isBankDown(Midi msg) {
//		int progChange = whichProgPad(msg);
//		if (progChange < 0 || progChange > 3) return false;
//		return msg.getData1() == green[progChange - 1][BANK_DOWN];
//	}

	public static boolean isInstrumentUp(Midi msg) {
		int cmd = msg.getCommand();
		if (cmd >= ShortMessage.PROGRAM_CHANGE && cmd <= 208) {
			int data1 = msg.getData1();
			for (int ab = 0; ab < 2; ab++) {
				for (int i = 0; i < PAD_STYLES; i++) {
					if (progchanges[ab][i][INSTRUMENT_UP] == data1)
						return true;
				}
			}
		}
		return false;
	}

	public static boolean isCCPad(Midi msg, int CCpad) {
		int data1 = msg.getData1();
		for (int ab = 0; ab < 2; ab++)
			for (int padLvl = 0; padLvl < PAD_STYLES; padLvl++)
				if (ccpads[ab][padLvl][CCpad] == data1)
					return isCC(msg);
		return false;
	}

	public static boolean isKnobType(Midi msg, int knobType) {
		int data1 = msg.getData1();
		for (int i = 0; i < KNOB_STYLES; i++)
			if (knobs[i][knobType] == data1)
				return isCC(msg);
		return false;
	}

	public static boolean isInstrumentDown(Midi msg) {
		int cmd = msg.getCommand();
		if (cmd >= ShortMessage.PROGRAM_CHANGE && cmd <= 208) {
			int data1 = msg.getData1();
			for (int ab = 0; ab < 2; ab++) {
				for (int i = 0; i < PAD_STYLES; i++) {
					if (progchanges[ab][i][INSTRUMENT_DOWN] == data1)
						return true;
				}
			}
		}
		return false;
	}

	/** @return null if the msg was consumed (possibly firing off other events) or pass the midi message back as bytes*/
	public ShortMessage process(ShortMessage msg) throws InvalidMidiDataException {

		// if (fluid != null) return fluidSynth(msg); // fluid = master.getFluidsynth();
		final int dat1 = msg.getData1();
		final int dat2 = msg.getData2();
		if (msg.getStatus() == ShortMessage.NOTE_ON || msg.getStatus() == ShortMessage.NOTE_OFF)
			return new Midi(msg.getCommand(), msg.getChannel(), dat1, dat2);

		// int i = (byte & 0xFF);
		// Use RTDebug System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));

		return new ShortMessage(msg.getCommand(), msg.getChannel(), dat1, dat2);
	}


	private ShortMessage processDR5(ShortMessage msg) {
//		if (isCC(msg)) {
//
//			// System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));
//			if (dat1 == DR5_PADS.DR5_SWITCH) {
//				// TODO toggle to dr-5/fluid
//			}
//
//			if (dat1 == DR5_PADS.DRUMS && dat2 > 0) {
//
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
//
//		if (isProgramChange(msg)) {
//			log.info("Program Change, yo");
//			int padNum = whichProgPad(msg);
//			if (padNum >= 0 && padNum < PAD_COUNT) {
//			}
//		}
		return null;

	}

	private Midi processFluid(Midi msg) {
		// if (fluid != null) // fluid = master.getFluidsynth();
		if (msg.getStatus() == ShortMessage.NOTE_ON || msg.getStatus() == ShortMessage.NOTE_OFF) return msg;
		// int i = (int)(byte & 0xFF)
		//System.out.println(msg.getStatus() + " @ " + msg.getData1() + " @ " + msg.getData2() + " @ " + msg.getCommand() + " knob: " + isKnob(msg));

//		if (isCC(msg)) {
//			int knob = isKnob(msg);
//			if (knob == VOLUME_KNOB) {
//				fluid.gain(msg.getData2() * 0.05f);
//				return null;
//			}
//        	if (isKnobType(msg, REVERB_KNOB)) {
//        		fluid.reverb(toFloat(msg.getData2()));
//        		return null;
//        	}
//        	if (isKnobType(msg, ROOM_SIZE_KNOB)) {
//        		fluid.roomSize(toFloat(msg.getData2()));
//        		return null;
//        	}
//        	if (isKnobType(msg, DAMPNESS_KNOB)) {
//        		fluid.dampness(toFloat(msg.getData2()));
//        		return null;
//        	}
//        	if (isKnobType(msg, CHORUS_DELAYLINES_KNOB)) {
//        		fluid.chorusDelayLines((int) (msg.getData2() * 0.99f));
//        		return null;
//        	}
//        	if (isKnobType(msg, CHORUS_LEVEL_KNOB)) {
//        		fluid.chorusLevel(msg.getData2() / 80f);
//        		return null;
//        	}
//// TODO zero error   sendCommand: cho_set_speed 3.7006562 Too high depth. Setting it to max (2048).
//        	if (isKnobType(msg, CHORUS_SPEED_KNOB)) {
//        		// 0.3 to 5
//        		fluid.chorusSpeed(((msg.getData2() + 0.61f) * 0.0496f) );
//        		return null;
//        	}
//        	if (isKnobType(msg, CHORUS_DEPTH_KNOB)) {
//        		fluid.chorusDepth((int)(msg.getData2() * 0.42f));
//        		return null;
//        	}
//// TODO read data2
//        	if (isCCPad(msg, TOGGLE_REVERB)) {
//        		fluid.toggle(Constants.Toggles.REVERB, msg.getData2() > 0);
//        	}
//        	if (isCCPad(msg, TOGGLE_CHORUS)) {
//        		fluid.toggle(Constants.Toggles.CHORUS, msg.getData2() > 0);
//        	}
//		}
//
//		if (isProgramChange(msg)) {
//			int padNum = whichProgPad(msg);
//			if (padNum >= 0 && padNum < PAD_COUNT) {
//	        	if (isInstrumentUp(msg))
//	        		return fluid.instrumentUp();
//	        	if (isInstrumentDown(msg))
//	        		return fluid.instrumentDown();
//	        	if (padNum == PRESET1)
//	        		return fluid.preset(1);
//	        	if (padNum == PRESET2)
//	        		return fluid.preset(2);
//			}
//		}
// TODO
//        	if (isBankUp(msg))
//        		return fluid.bankUp();
//        	if (isBankDown(msg))
//        		return fluid.bankDown();
		return msg;
	}

	private float toFloat(int val) {
		if (val > 100) val = 100;
		return val / 100f;
	}

	@Override
	public String getTabName() {
		return "MPK";
	}


	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}


}
