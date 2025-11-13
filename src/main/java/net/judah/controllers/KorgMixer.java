package net.judah.controllers;

import static net.judah.JudahZone.*;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.seq.Seq;
import net.judah.seq.Trax;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.Scene;

/** Korg nanoKONTROL2 midi controller custom codes */
public class KorgMixer implements Controller {
	public static final String NAME = "nanoKONTROL2";

	public static enum TYPE {
		MOMENTARY, TOGGLE, KNOB, NOTE_ON, NOTE_OFF, PROGCHAN, OTHER
	}

	@RequiredArgsConstructor
	static class MapEntry {
		@Getter private final String name;
		@Getter private final TYPE type;
		@Getter private final int val;
	}

	private static final int soff = 32;
	private static final int moff = 48;
	private static final int roff = 64;
	private static final int knoboff = 16;

	private static final MapEntry PREV = new MapEntry(new String("PREV"), TYPE.MOMENTARY, 58);
 	private static final MapEntry NEXT = new MapEntry(new String("NEXT"), TYPE.MOMENTARY, 59);
 	private static final MapEntry CYCLE = new MapEntry(new String("LOOP"), TYPE.TOGGLE, 46);
	private static final MapEntry SET = new MapEntry(new String("SET"), TYPE.MOMENTARY, 60);
	private static final MapEntry PREV2 = new MapEntry("PREV2", TYPE.MOMENTARY, 61);
	private static final MapEntry NEXT2 = new MapEntry("NEXT2", TYPE.MOMENTARY, 62);
	private static final MapEntry RWND = new MapEntry("RWND", TYPE.MOMENTARY, 43);
	private static final MapEntry FWRD = new MapEntry("FWRD", TYPE.MOMENTARY, 44);
	private static final MapEntry STOP = new MapEntry("STOP", TYPE.MOMENTARY, 42);
	private static final MapEntry PLAY = new MapEntry("PLAY", TYPE.TOGGLE, 41);
	private static final MapEntry RECORD = new MapEntry("REC", TYPE.TOGGLE, 45);


	@Override
	public boolean midiProcessed(Midi midi) {
		int data1 = midi.getData1();
		int data2 = (int)Math.floor(midi.getData2() / 1.27f);
		Seq seq = getSeq();

		if (data1 >= 0 && data1 < 8) { // Main Faders
			Channel ch = fader(data1);
			ch.getGain().set(Gain.VOLUME, data2);
			MainFrame.update(ch);
		}

		// knobs = drumkit or synths gain
		else if (data1 >= knoboff && data1 < knoboff + 4) {
			getDrumMachine().getTracks().get(data1 - knoboff).setAmp(data2 * .01f);
		}
		else if (data1 >= knoboff + 4 && data1 < knoboff + 8) {
			ampKnob(data1 - (knoboff + 4), data2);
		}
		else if (data2 > 0 && data1 >= soff && data1 < soff + 8) { // play/stop sequencer tracks
			if (data1 < soff + 4)
				seq.get(data1 - soff).trigger();
			else if (data1 == soff + 4)
				seq.byName(Trax.TK2.toString()).trigger();
			else
				getFluid().getTracks().get(data1 - soff - 5).trigger();
		}
		else if (data2 > 0 && data1 >= moff && data1 < moff + 8) { // MUTE/RECORD
			Channel ch = mutes(data1 - moff);
			if (ch == getMic())
				((LineIn)ch).setMuteRecord(!((LineIn)ch).isMuteRecord());
			else if (ch == getGuitar()) // swap
				getBass().setOnMute(!getBass().isOnMute());
			else
				ch.setOnMute(!ch.isOnMute());
		}

		else if (data2 > 0 && data1 >= roff && data1 < roff + 8) { // LAUNCH SCENE
			launchScene(data1 - roff);
		}

		else if (data1 == SET.getVal() && data2 != 0) { // Run SettableCombo or hide modal dialog
			// TODO override = select track's Frame on KorgPads
		}
		else if (data1 == CYCLE.getVal()) {
			getLooper().verseChorus();
		}
		else if (data1 == PREV.getVal() && data2 != 0) {
			seq.getTracks().next(false);
		}
		else if (data1 == NEXT.getVal() && data2 != 0) {
			seq.getTracks().next(true);
		}

		else if (data1 == PREV2.getVal() && data2 != 0) { // change pattern
			seq.getCurrent().next(false);
		}
		else if (data1 == NEXT2.getVal() && data2 != 0) { // change pattern
			seq.getCurrent().next(true);
		}
		else if (data1 == RWND.getVal() && data2 != 0) { // prev Tab
			TabZone.instance.changeTab(false);
		}
		else if (data1 == FWRD.getVal() && data2 != 0) { // next Tab
			TabZone.instance.changeTab(true);
		}
		if (data1 == STOP.getVal() && data2 != 0 && seq.getCurrent() != null) { // Track Active/Inactive
			seq.getCurrent().toggle();
		}
		else if (data1 == PLAY.getVal() && data2 > 0) { // MPK mode
			if (seq.getCurrent() instanceof PianoTrack synth)
				synth.toggle(Arp.MPK);
//			else
//				seq.getCurrent().toggle();
		}
		else if (data1 == RECORD.getVal())
			seq.getCurrent().setCapture(!seq.getCurrent().isCapture());

		return true;
	}

	private void launchScene(int idx) {
		if (idx == 7) {
			getSeq().byName(Trax.B.toString()).toggle();
			return;
		}
		List<Scene> scenes = getOverview().getSong().getScenes();
		getOverview().setOnDeck(scenes.size() > idx ? scenes.get(idx) : scenes.getLast());

	}

	private void ampKnob(int idx, int data2) {
		MidiTrack track =  switch (idx) {
			case 1 -> getFluid().getTracks().get(0);
			case 2 -> getFluid().getTracks().get(1);
			case 3 -> getFluid().getTracks().get(2);
			default -> getTacos().tk2.getTrack();
		};
		track.setAmp(data2 * .01f);
	}

	private Channel fader(int idx) {
		if (idx < 4)
			return getLooper().get(idx);
		if (idx == 5)
			return getFluid();
		if (idx == 6)
			return getDrumMachine();
		if (idx == 7) {
			return getMains();
		}
		return getTacos().taco;
	}

	private Channel mutes(int idx) {
		if (idx < 4)
			return getLooper().get(idx);
		if (idx == 5)
			return getBass();
		if (idx == 6)
			return getDrumMachine();
		if (idx == 7) {
			return getMains();
		}
		return getMic();
	}

}

/**

        nanoKONTROL2 MIDI Implementation             Revision 1.00 (2010.12.14)

1.Transmitted Data -------------------------------------------------------------

1-1 Channel Messages            [H]:Hex,  [D]:Decimal
+--------+----------+----------+-----------------------------------------------+
| Status |  Second  |  Third   | Description         (Transmitted by )         |
|  [Hex] | [H]  [D] | [H]  [D] |                                               |
+--------+----------+----------+-----------------------------------------------+
|   8n   | kk  (kk) | 40  (64) | Note Off            (Button)                  |
|   9n   | kk  (kk) | 00  (00) | Note Off            (Button) *m               |
|   9n   | kk  (kk) | VV  (VV) | Note On             (Button)                  |
|   Bn   | cc  (cc) | vv  (vv) | Control Change      (Panel Controls)          |
|   En   | vv  (vv) | vv  (vv) | Pitch Bend          (Slider) *m               |
+--------+----------+----------+-----------------------------------------------+
 n  : MIDI Channel = 0~15
 kk : Note# 0~127
 VV : Velocity = 1~127
 cc : Control Change# = 0~127
 vv : Value = 0~127
 *m Only DAW1/DAW2/DAW3/DAW5 Mode


1-2 Universal System Exclusive Message ( Non Realtime )

 Device Inquiry Reply
+---------+-----------------------------------------------+
| Byte[H] |                Description                    |
+---------+-----------------------------------------------+
|   F0    | Exclusive Status                              |
|   7E    | Non Realtime Message                          |
|   0g    | Global MIDI Channel  ( Device ID )            |
|   06    | General Information                           |
|   02    | Identity Reply                                |
|   42    | KORG ID              ( Manufacturers ID )     |
|   13    | Software Project     ( Family ID   (LSB))     |
|   01    |                      ( Family ID   (MSB))     |
|   00    |                      ( Member ID   (LSB))     |
|   00    |                      ( Member ID   (MSB))     |
|   xx    |                      ( Minor Ver.  (LSB))     |
|   xx    |                      ( Minor Ver.  (MSB))     |
|   xx    |                      ( Major Ver.  (LSB))     |
|   xx    |                      ( Major Ver.  (MSB))     |
|   F7    | End Of Exclusive                              |
+---------+-----------------------------------------------+

  This message is transmitted whenever an INQUIRY MESSAGE REQUEST is received.



1-3 System Exclusive Message Transmitted Command List

 Structure of nanoKONTROL2 System Exclusive Messages

 1st Byte = F0 : Exclusive Status
 2nd Byte = 42 : KORG
 3rd Byte = 4g : g : Global MIDI Channel
 4th Byte = 00 : Software Project (nanoKONTROL2: 000113H)
 5th Byte = 01 :
 6th Byte = 13 :
 7th Byte = 00 : Sub ID
 8th Byte = cd : 0dvmmmmm  d     (1: Controller->Host)
                           v     (0: 2 Bytes Data Format, 1: Variable)
                           mmmmm (Command Number)
 9th Byte = nn : 2 Bytes Format: Function ID, Variable: Num of Data
10th Byte = dd : Data
  :
 LastByte = F7 : End of Exclusive

+-----------------+---------------------------------------+
|8th Byte command#| Description/Command                   |
|   [Bin (Hex)]   |                                       |
+-----------------+---------------------------------------+
|  010 00000 (40) | Native mode In/Out                    |
|  010 11111 (5F) | Packet Communication               *2 |
|  011 11111 (7F) | Data Dump                          *2 |
+-----------------+---------------------------------------+

 *2 :Function ID Code List
+-------------+-----------------------------------+-----+
| Function ID | Description/Function              |     |
|    [Hex]    |                                   |     |
+-------------+-----------------------------------+-----+
|     40      | Current Scene Data Dump           |  R  |
|     23      | Data Load Completed               |  E  |
|     24      | Data Load Error                   |  E  |
|     21      | Write Completed                   |  E  |
|     22      | Write Error                       |  E  |
|     42      | Mode Data                         |  R  |
+-------------+-----------------------------------+-----+
Transmitted when
      R  : Request Message is received.
      C  : Change Scene.
      E  : Exclusive Message is received.


1-4 Search Device Message

 Search Device Reply
+---------+------------------------------------------------+
| Byte[H] |                Description                     |
+---------+------------------------------------------------+
|   F0    | Exclusive Status                               |
|   42    | KORG ID              ( Manufacturers ID )      |
|   50    | Search Device                                  |
|   01    | Request                                        |
|   0g    | g: MIDI Global Channel  ( Device ID )          |
|   dd    | Echo Back ID                                   |
|   13    | nanoKONTROL2 ID      ( Family ID   (LSB))      |
|   01    | nanoKONTROL2 ID      ( Family ID   (MSB))      |
|   00    |                      ( Member ID   (LSB))      |
|   00    |                      ( Member ID   (MSB))      |
|   xx    |                      ( Minor Ver.  (LSB))      |
|   xx    |                      ( Minor Ver.  (MSB))      |
|   xx    |                      ( Major Ver.  (LSB))      |
|   xx    |                      ( Major Ver.  (MSB))      |
|   F7    | End Of Exclusive                               |
+---------+------------------------------------------------+

  g  : MIDI Global Channel = 0 ~ F
  dd : Echo Back ID = copy from SEARCH DEVICE REQUEST message.

  This message is transmitted whenever a SEARCH DEVICE REQUEST is received.


2.Recognized Receive Data ------------------------------------------------------

2-1 Universal System Exclusive Message (Non Realtime)

 Inquiry Message Request
+---------+-----------------------------------------------+
| Byte[H] |                Description                    |
+---------+-----------------------------------------------+
|   F0    | Exclusive Status                              |
|   7E    | Non Realtime Message                          |
|   gg    | Global MIDI Channel                           |
|   06    | General Information                           |
|   01    | Identity Request                              |
|   F7    | End Of Exclusive                              |
+---------+-----------------------------------------------+
 gg = 00~0F :Received if Global Channel
      7F    :Received on any Channel


2-2 System Exclusive Message Received Command List
 Structure of nanoKONTROL2 System Exclusive Messages

 1st Byte = F0 : Exclusive Status
 2nd Byte = 42 : KORG
 3rd Byte = 4g : g : Global MIDI Channel
 4th Byte = 00 : Software Project (nanoKONTROL2: 000113H)
 5th Byte = 01 :
 6th Byte = 13 :
 7th Byte = 00 : Sub ID
 8th Byte = cd : 0dvmmmmm  d     (0: Host->Controller)
                           v     (0: 2 Bytes Data Format, 1: Variable)
                           mmmmm (Command Number)
 9th Byte = nn : 2 Bytes Format: Function ID, Variable: Num of Data
10th Byte = dd : Data
  :
 LastByte = F7 : End of Exclusive

+-----------------+---------------------------------------+
|8th Byte command#| Description/Command                   |
|   [Bin (Hex)]   |                                       |
+-----------------+---------------------------------------+
|  000 00000 (00) | Native mode In/Out Request            |
|  000 11111 (1F) | Data Dump Request                  *3 |
|  011 11111 (7F) | Data Dump                          *3 |
+-----------------+---------------------------------------+

 *3 :Function ID Code List
+-------------+-----------------------------------+
| Function ID | Description/Function              |
|    [Hex]    |                                   |
+-------------+-----------------------------------+
|     10      | Current Scene Data Dump Request   |
|     40      | Current Scene Data Dump           |
|     11      | Scene Write Request               |
|     12      | Mode Request                      |
+-------------+-----------------------------------+


2-3 Search Device Message

 Search Device Request
+---------+------------------------------------------------+
| Byte[H] |                Description                     |
+---------+------------------------------------------------+
|   F0    | Exclusive Status                               |
|   42    | KORG ID              ( Manufacturers ID )      |
|   50    | Search Device                                  |
|   00    | Request                                        |
|   dd    | Echo Back ID                                   |
|   F7    | END OF EXCLUSIVE                               |
+---------+------------------------------------------------+
 Receive this message, and transmits SEARCH DEVICE REPLY message
 including copied Echo Back ID.


3.MIDI Exclusive Format   (R:Receive, T:Transmit) ------------------------------

3-1 Standard Messages

 (1) Current Scene Data Dump Request                                             R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0000 (10) | Current Scene Data Dump Request                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoKONTROL2 transmits a Func=40(Current Scene Data Dump)
 or a Func=24(NAK) message.

 (2) Scene Write Request                                                         R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0001 (11) | Scene Write Request                                              |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoKONTROL2 writes the current scene data to the
 internal memory and transmits Func=21(Write Complete) messages or a Func=22(Write Error)
 message.

 (3) Native mode In/Out Request                                                  R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0000 0000 (00) | Native mode In/Out Request (Host->Controller, 2Bytes Format)     |
| 0000 0000 (00) |                                                                  |
| 0qqq qqqq (qq) | qq = 00:Out Req, 01:In Req                                       |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive this message, and transmits Command=40 message.

 (4) Mode Request                                                                R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0010 (12) | Mode Request                                                     |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive this message, and transmits Func=42 message.

 (5) Current Scene Data Dump                                                     R,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0111 1111 (7F) | Data Dump Command  (Host<->Controller, Variable Format)          |
| 0111 1111 (7F) | Over 0x7F Data                                                   |
| 0000 0010 (02) | 2Bytes structure                                                 |
| 0000 0011 (03) | Num of Data MSB (1+388 bytes : B'110000101)                      |
| 0000 0101 (05) | Num of Data LSB                                                  |
| 0100 0000 (40) | Current Scene Data Dump                                          |
| 0ddd dddd (dd) | Data                                                 (NOTE 1, 2) |
|     :          |  :                                                               |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When these message and data were received, the nanoKONTROL2 saves the data to the current
 scene, and transmits a Func=23(ACK) or a Func=24(NAK) message.
 When a Func=10(Current Scene Data Dump Request) message was received, the nanoKONTROL2
 transmits this message with the current scene data.

 (6) Data Load Completed (ACK)                                                   -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0011 (23) | Data Load Completed                                              |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the operation was completed, the nanoKONTROL2 transmits this message.

 (7) Data Load Error (NAK)                                                       -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0100 (24) | Data Load Error                                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the operation was not completed, the nanoKONTROL2 transmits this message.

 (8) Write Completed                                                             -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0001 (21) | Write Completed                                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the writing operation was completed, the nanoKONTROL2 transmits this message.

 (9) Write Error                                                                 -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0010 (22) | Write Error                                                      |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the writing operation was not completed, the nanoKONTROL2 transmits this message.

 (10) Native mode In/Out                                                         -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0100 0000 (40) | Native mode In/Out  (Host<-Controller, 2Bytes Format)            |
| 0000 0000 (00) |                                                                  |
| 0rrr rrrr (rr) | rr = 02:Out,03:In                                                |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive Command=00 message, and transmits this message.

 (11) Mode Data                                                                  -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,13,00,   | Software Project (nanoKONTROL2: 000113H, Sub ID: 00)             |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0100 0010 (42) | Mode Data                                                        |
| 0mmm mmmm (rr) | mm = 00:Normal mode, 01:Native mode                              |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive Func=12 message, and transmits this message.

NOTE 1: Current Scene Data Dump Format
         339 bytes = 7*48+3 -> 8*48+(3+1) => 388 bytes
         (TABLE 1)

NOTE 2: The Dump Data Conversion

   Data (1set = 8bit x 7Byte)
   b7     ~      b0   b7     ~      b0   b7   ~~    b0   b7     ~      b0
   +-+-+-+-+-+-+-+-+  +-+-+-+-+-+-+-+-+  +-+-+-~~-+-+-+  +-+-+-+-+-+-+-+-+
   | | | | | | | | |  | | | | | | | | |  | | |    | | |  | | | | | | | | |
   +-+-+-+-+-+-+-+-+  +-+-+-+-+-+-+-+-+  +-+-+-~~-+-+-+  +-+-+-+-+-+-+-+-+
         7n+0               7n+1          7n+2 ~~ 7n+5         7n+6

    MIDI Data (1set = 7bit x 8Byte)
      b7b7b7b7b7b7b7     b6    ~     b0     b6 ~~    b0     b6    ~     b0
   +-+-+-+-+-+-+-+-+  +-+-+-+-+-+-+-+-+  +-+-+-~~-+-+-+  +-+-+-+-+-+-+-+-+
   |0| | | | | | | |  |0| | | | | | | |  |0| |    | | |  |0| | | | | | | |
   +-+-+-+-+-+-+-+-+  +-+-+-+-+-+-+-+-+  +-+-+-~~-+-+-+  +-+-+-+-+-+-+-+-+
   7n+6,5,4,3,2,1,0         7n+0          7n+1 ~~ 7n+5         7n+6


 TABLE 1 : Scene Parameter
+--------+----------------------------+--------------------------------------------+
|        |     PARAMETER              |                VALUE                       |
+--------+----------------------------+--------------------------------------------+
| Common parameters                                                                |
+--------+----------------------------+------------------------------------------ -+
| 0      | Global MIDI Ch.            | 0~15                                       |
|        +----------------------------+--------------------------------------------+
| 1      | Control Mode               | 0~5=CC Mode/Cubase/DP/                     |
|        |                            |     Live/ProTools/SONAR                    |
|        +----------------------------+--------------------------------------------+
| 2      | LED Mode                   | 0~1=Internal/External                      |
+--------+----------------------------+--------------------------------------------+
| Controller Group 1 parameters                                                    |
+--------+----------------------------+--------------------------------------------+
| 3      | Group MIDI Ch.             | 0~16=0~15/Global MIDI Ch.                  |
|        +----------------------------+--------------------------------------------+
| 4      | Slider assign type         | 0/1=Disable/Enable                         |
|        +----------------------------+--------------------------------------------+
| 5      | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 6      | Slider CC/Note Number      | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 7      | Slider min value           | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 8      | Slider max value           | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 9      | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 10     | Knob assign type           | 0/1=Disable/Enable                         |
|        +----------------------------+--------------------------------------------+
| 11     | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 12     | Knob CC/Note Number        | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 13     | Knob min value             | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 14     | Knob max value             | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 15     | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 16     | Solo Button assign type    | 0~2=No Assign/Control Change/              |
|        |                            |     Note                                   |
|        +----------------------------+--------------------------------------------+
| 17     | Solo Button Behavior       | 0/1=Momentary/Toggle                       |
|        +----------------------------+--------------------------------------------+
| 18     | Solo Button CC/Note Number | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 19     | Solo Button off value      | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 20     | Solo Button on value       | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 21     | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 22     | Mute Button assign type    | 0~2=No Assign/Control Change/Note          |
|        +----------------------------+--------------------------------------------+
| 23     | Mute Button Behavior       | 0/1=Momentary/Toggle                       |
|        +----------------------------+--------------------------------------------+
| 24     | Mute Button CC/Note Number | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 25     | Mute Button off value      | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 26     | Mute Button on value       | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 27     | Reserved                   | 0                                          |
|        +----------------------------+--------------------------------------------+
| 28     | Rec Button assign type     | 0~2=No Assign/Control Change/Note          |
|        +----------------------------+--------------------------------------------+
| 29     | Rec Button Behavior        | 0/1=Momentary/Toggle                       |
|        +----------------------------+--------------------------------------------+
| 30     | Rec Button CC/Note Number  | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 31     | Rec Button off value       | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 32     | Rec Button on value        | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 33     | Reserved                   | 0                                          |
+--------+----------------------------+--------------------------------------------+
| 34~64  | Group 2 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
| 65~95  | Group 3 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
| 96~126 | Group 4 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
|127~157 | Group 5 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
|158~188 | Group 6 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
|189~219 | Group 7 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
|220~250 | Group 8 parameters         | ( Same as the Group 1 format )             |
+--------+----------------------------+--------------------------------------------+
| Transport Button parameters                                                      |
+--------+----------------------------+--------------------------------------------+
| 251    | Transport Button MIDI Ch.  | 0~16=0~15/Global MIDI Ch.                  |
+--------+-------------------------- -+--------------------------------------------+
| Transport Button Prev Track parameters                                           |
+--------+----------------------------+--------------------------------------------+
| 252    | Assign type                | 0~2=No Assign/Control Change/Note          |
|        +----------------------------+--------------------------------------------+
| 253    | Button Behavior            | 0/1=Momentary/Toggle                       |
|        +----------------------------+--------------------------------------------+
| 254    | CC/Note Number             | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 255    | Off value                  | 0~127                                      |
|        +----------------------------+--------------------------------------------+
| 256    | On value                   | 0~127                                      |
|        +----------------------------+ -------------------------------------------+
| 257    | Reserved                   | 0                                          |
+--------+----------------------------+--------------------------------------------+
|258~263 | Next Track parameters      | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|264~269 | Cycle parameters           | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|270~275 | Marker Set  parameters     | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|276~281 | Prev Marker parameters     | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|282~287 | Next Marker parameters     | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|288~293 | REW parameters             | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|294~299 | FF parameters              | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|300~305 | STOP parameters            | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|306~311 | PLAY parameters            | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|312~317 | REC parameters             | (Same as the Transport Prev Track format ) |
+--------+----------------------------+--------------------------------------------+
|318~322 | Custom DAW Assign          | 0,41,42,46,47,48,49,50,127,255             |
+--------+----------------------------+--------------------------------------------+
|323~338 | Reserved                   | 0                                          |
+--------+----------------------------+--------------------------------------------+

Total 339 bytes


4.Native KORG Mode Messages ----------------------------------------------------

(1) Native KORG mode Display LEDs
+--------+------------------+--------------------------------------+
| Status | Second   | Third |   Description                        |
| [Hex]  | [Hex]    | [Hex] |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  2E      |  ss   |   Cycle (ss= Off:00~40 On:41~7F)     |
|   BF   |  2B      |  ss   |   REW                                |
|   BF   |  2C      |  ss   |   FF                                 |
|   BF   |  2A      |  ss   |   STOP                               |
|   BF   |  29      |  ss   |   PLAY                               |
|   BF   |  2D      |  ss   |   REC                                |
+--------+----------+-------+--------------------------------------+
|   BF   |  20      |  ss   |   Group 1 Solo                       |
|   BF   |  21 ~ 27 |  ss   |   Group 2 ~ 8 Solo                   |
|   BF   |  30      |  ss   |   Group 1 Mute                       |
|   BF   |  31 ~ 37 |  ss   |   Group 2 ~ 8 Mute                   |
|   BF   |  40      |  ss   |   Group 1 Rec                        |
|   BF   |  41 ~ 47 |  ss   |   Group 2 ~ 8 Rec                    |
+--------+----------+-------+--------------------------------------+

(2) Native KORG Mode Button Output
+--------+------------------+--------------------------------------+
| Status | Second   | Third |  Description                         |
| [Hex]  | [Hex]    |       |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  2E      |  ss   |  Cycle ss;on/off (on = 127 Off = 00) |
|   BF   |  2B      |  ss   |  REW                                 |
|   BF   |  2C      |  ss   |  FF                                  |
|   BF   |  2A      |  ss   |  STOP                                |
|   BF   |  29      |  ss   |  PLAY                                |
|   BF   |  2D      |  ss   |  REC                                 |
|   BF   |  3A      |  ss   |  << Track                            |
|   BF   |  3B      |  ss   |  Track >>                            |
|   BF   |  3C      |  ss   |  Marker Set                          |
|   BF   |  3D      |  ss   |  << Marker                           |
|   BF   |  3E      |  ss   |  Marker >>                           |
+--------+----------+-------+--------------------------------------+
|   BF   |  20      |  ss   |  Group 1 Solo                        |
|   BF   |  21 ~ 27 |  ss   |  Group 2 ~ 8 Solo                    |
|   BF   |  30      |  ss   |  Group 1 Mute                        |
|   BF   |  31 ~ 37 |  ss   |  Group 2 ~ 8 Mute                    |
|   BF   |  40      |  ss   |  Group 1 Rec                         |
|   BF   |  41 ~ 47 |  ss   |  Group 2 ~ 8 Rec                     |
+--------+----------+-------+--------------------------------------+

(3) Native KORG Mode Knob/Slider Output
+--------+------------------+--------------------------------------+
| Status | Second   | Third |  Description                         |
| [Hex]  | [Hex]    |       |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  10      |  vv   |  Group 1 Knob vv=value(0 ~ 127)      |
|   BF   |  11 ~ 17 |  vv   |  Group 2 ~ 8 Knob                    |
+--------+----------+-------+--------------------------------------+
|   BF   |  00      |  vv   |  Group 1 Slider vv=value(0~127)      |
|   BF   |  01 ~ 07 |  vv   |  Group 2 ~ 8 Slider                  |
+--------+----------+-------+--------------------------------------+

*/
