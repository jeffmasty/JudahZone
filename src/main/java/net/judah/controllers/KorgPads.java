package net.judah.controllers;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.ControlPanel;
import net.judah.JudahZone;
import net.judah.Looper;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.api.Midi;
import net.judah.controllers.MapEntry.TYPE;
import net.judah.effects.Chorus;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.Fader;
import net.judah.effects.LFO.Target;
import net.judah.effects.Overdrive;
import net.judah.looper.Loop;
import net.judah.looper.SyncWidget.SelectType;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahClock.Mode;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;

/** CC 1 - 16 on channel 13 */
public class KorgPads extends ArrayList<Runnable> implements Controller {

	public static final String NAME = "nanoPAD2";
	@Getter private final ArrayList<MapEntry> list = new ArrayList<MapEntry>(); 
	private final Looper looper;
	
	private long lastPress;
	private Loop lastLoop;
	
	private final int JOYSTICK_ON = 75;
	private final int JOYSTICK_X = 73;
	private final int JOYSTICK_Y = 74;
	private final int LEFT = 55;
	private final int RIGHT = 74;
	
	public KorgPads(KorgMixer mixer, Looper looper) {
		assert looper != null;
		this.looper = looper;
		for (int x = 1; x < 17; x++)
			list.add(new MapEntry(new String("" + x), TYPE.MOMENTARY, x));
	}
	
	/** on double tap */
	private boolean doErase(Loop ch) {
		if (lastLoop == ch && System.currentTimeMillis() - lastPress < Constants.DOUBLE_CLICK) {
			lastPress = 0;
			
			ch.erase();
			return true;
		}
		lastPress = System.currentTimeMillis();
		lastLoop = ch;
		return false;
	}

	// TODO tap tempo
	// TODO song pattern select
	@Override public boolean midiProcessed(Midi midi) {
		int data1 = midi.getData1();
		int data2 = midi.getData2();
		// only trigger commands on pad press, not on pad release, but stop processing release midi bytes
		if (data2 == 0) {
			if (JOYSTICK_ON == data1) 
				joystick(false);
			return true;
		}

		switch (data1) {
		// top row pads
		case 1: // toggle record loop A
			if (doErase(looper.getLoopA())) 
				return true;
			trigger(looper.getLoopA());
			return true;
		case 2: // toggle record loop B
			if (doErase(looper.getLoopB())) 
				return true;
			return record(looper.getLoopB());
		case 3: // toggle record loop C
			if (doErase(looper.getLoopC())) 
				return true;
			return record(looper.getLoopC());

		case 4: // toggle record drums loop
			if (doErase(looper.getDrumTrack())) 
				return true;
			return record(looper.getDrumTrack());

		case 5: // RESET / TODO Song Type
			if (looper.getLoopA().hasRecording())  
				looper.reset();
			return true; 
		case 6: 
			JudahClock.setOnDeck(SelectType.SYNC);
			return true; 
		case 7: // focus efx on Main or Circuit Trax channel 
			ControlPanel controls = ControlPanel.getInstance();
			if (controls.getCurrent() == null) 
				MainFrame.setFocus(JudahZone.getMasterTrack());
			else if (controls.getCurrent().getChannel() == JudahZone.getMasterTrack())
				MainFrame.setFocus(JudahZone.getChannels().getCircuit());
			else 
				MainFrame.setFocus(JudahZone.getMasterTrack());
			return true;
		case 8: // TODO Tuner/PlayStop Loop/FadeOut Main
			Channel ch = ControlPanel.getInstance().getChannel();
			if (ch == null) return true; // ? 
			if (ch instanceof LineIn && (ch == JudahZone.getChannels().getGuitar() ||
					ch == JudahZone.getChannels().getCrave())) {
				ControlPanel.getInstance().getTuner().setChannel(
						GuitarTuner.getChannel() == ch ? null : ch);
			} else {
				if (ch.isOnMute() || ch.getGain().getVol() < 5) {
					ch.setOnMute(false);
					Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, 0, 51));
				}
				else 
					Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
			}
			return true;
			
		// bottom row pads
		case 9: 
			Loop a = looper.getLoopA();
			if (a.hasRecording())  
				looper.reset();
			else {
				a.setArmed(!a.isArmed());
				MainFrame.update(a);
			}
            return true;
		
		case 10:  // latch B
			Loop b = looper.getLoopB();
			if (b.hasRecording())
				b.duplicate();
			else {
				b.setArmed(!b.isArmed());
				MainFrame.update(b);
			}
			return true;
		case 11:  // latch C
			Loop c = looper.getLoopC();
			if (c.hasRecording())
				c.duplicate(); // TODO set Tempo as per clock.length in bars and free loop C
			else { 
				c.setArmed(!c.isArmed());
				MainFrame.update(c);
			}
			return true;
		case 12: // latch Drums
			if (looper.getLoopA().hasRecording() == false) 
				looper.getDrumTrack().toggle();
			else if (looper.getDrumTrack().hasRecording() == false)
					JudahMidi.getClock().latch(looper.getLoopA());
			return true;
			
		case 13: // reverb
			new Events.LatchEfx();
			return true;
		case 14: // reset effects for current channel
			ControlPanel.getInstance().getChannel().reset();
			return true;
		case 15: // focus efx on Uno or Crave synth channels
			ControlPanel panel = ControlPanel.getInstance();
			if (panel.getCurrent() == null) 
				MainFrame.setFocus(JudahZone.getChannels().getCrave());
			else if (panel.getCurrent().getChannel() == JudahZone.getChannels().getCrave())
				MainFrame.setFocus(JudahZone.getChannels().getUno());
			else 
				MainFrame.setFocus(JudahZone.getChannels().getCrave());
			return true;
			
//			ControlPanel panel = ControlPanel.getInstance();
//			Channel synth = JudahZone.getChannels().getUno();
//			if (panel.getCurrent() == null || 
//				panel.getCurrent().getChannel() != synth) {
//				panel.setFocus(synth);
//			}
//			else { // Crave already selected
//				if (MPK.getMode() == KnobMode.Effects1)
//					MPK.setMode(KnobMode.Effects2); 
//				else 
//					MPK.setMode(KnobMode.Effects1);
//			}
//			return true;
		case 16: // play/stop/sync drum machine
			 JudahClock clock = JudahMidi.getClock();
			if (clock.isActive()) 
				clock.end();
			else if (looper.getLoopA().hasRecording())  
				clock.latch(looper.getLoopA());
			else 
				clock.begin();
			return true; 
			
			
		case JOYSTICK_ON: 
			joystick(true);
			return true;
		case JOYSTICK_X:
			Channel fx = ControlPanel.getInstance().getChannel();
			Delay delay = fx.getDelay();
			Chorus chorus = fx.getChorus();
			Overdrive od = fx.getOverdrive();
			boolean left = data2 < 50;
			boolean right = data2 > RIGHT;
			if (left) {
				if (delay.getDelay() < Delay.DEFAULT_TIME) 
					delay.setDelay(Delay.DEFAULT_TIME);
				// cc 50 to 0 = 0 to max delay
				delay.setFeedback(.02f * (50 - data2));
				delay.setActive(true);
			}
			else if (right) {
				od.setDrive(Constants.logarithmic((data2 - 77) * 2, 0, 1));
				chorus.setActive(true);
				od.setActive(true);
			}
			else { // dead zone
				delay.setActive(false);
				od.setActive(false);
				chorus.setActive(false);
			}
			return true;
		case JOYSTICK_Y: 
			CutFilter party = ControlPanel.getInstance().getChannel().getCutFilter();
			boolean down = data2 < LEFT - 10;
			boolean up = data2 > RIGHT + 10;
			if (up) {
				party.setFrequency(CutFilter.knobToFrequency(data2 - 60));
				party.setActive(true);
			}
			else if (down) {
				party.setFrequency(CutFilter.knobToFrequency(data2));
				party.setActive(true);
			}
			else {
				party.setActive(false);
			}
			return true;
		}
		return false;
	}

	private void joystick(boolean active) {
		if (active) {
			ControlPanel.getInstance().getChannel().reset();
			// TODO stash preset
		}
		else {
			ControlPanel.getInstance().getChannel().reset();
			// TODO restore preset
		}
	}
	

	public static boolean record(Loop target) {
		target.record(target.isRecording() != AudioMode.RUNNING);
		return true;
	}
	
	public static void trigger(Loop loop) {
		JudahClock clock = JudahMidi.getClock();
		if (loop.isRecording() == AudioMode.RUNNING) {
			loop.record(false);
			clock.removeListener(loop.getSync());
		}
		else if (!loop.hasRecording()) {
			loop.getSync().syncUp();
			
			if (!clock.isActive() && clock.getMode() == Mode.Internal)
				clock.begin();
		}
		else 
			record (loop);
	}
	

}

/*

        nanoPAD2   MIDI Implementation               Revision 1.01 (2011.10.19)

1.Transmitted Data -------------------------------------------------------------

1-1 Channel Messages            [H]:Hex,  [D]:Decimal
+--------+----------+----------+-----------------------------------------------+
| Status |  Second  |  Third   | Description         (Transmitted by )         |
|  [Hex] | [H]  [D] | [H]  [D] |                                               |
+--------+----------+----------+-----------------------------------------------+
|   8n   | kk  (kk) | 40  (64) | Note Off            (Trigger Pad, X-Y Pad)    |
|   9n   | kk  (kk) | VV  (VV) | Note On             (Trigger Pad, X-Y Pad)    |
|   Bn   | cc  (cc) | vv  (vv) | Control Change      (Trigger Pad, X-Y Pad)    |
|   Cn   | vv  (vv) | --   --  | Program Change      (Trigger Pad)             |
|   En   | bb  (bb) | bb  (bb) | Pitch Bend Change   (X-Y Pad)                 |
+--------+----------+----------+-----------------------------------------------+
 n  : MIDI Channel = 0~15
 kk : Note# 0~127
 VV : Velocity = 1~127
 cc : Control Change# = 0~127
 vv : Value = 0~127


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
|   12    | Software Project     ( Family ID   (LSB))     |
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

 Structure of nanoPAD2 System Exclusive Messages

 1st Byte = F0 : Exclusive Status
 2nd Byte = 42 : KORG
 3rd Byte = 4g : g : Global MIDI Channel
 4th Byte = 00 : Software Project (nanoPAD2: 000112H)
 5th Byte = 01 : 
 6th Byte = 12 : 
 7th Byte = 00 : Sub ID
 8th Byte = cd : 0dvmmmmm  d     (1: Controller->Host)
                           v     (0: 2 Bytes Data Format, 1: Variable)
                           mmmmm (Command Number)
 9th Byte = nn : 2 Bytes Format: Function ID, Variable: Num of Data
10th Byte = dd : Data
  :
 LastByte = F7 : End of Exclusive


+-----------------+---------------------------------------+
|9th Byte command#| Description/Command                   |
|   [Bin (Hex)]   |                                       |
+-----------------+---------------------------------------+
|  010 00000 (40) | Native mode In/Out                    |
|  010 11111 (5F) | Packet Communication               *1 |
|  011 11111 (7F) | Data Dump                          *1 |
+-----------------+---------------------------------------+

 *1 :Function ID Code List
+-------------+-----------------------------------+-----+
| Function ID | Description/Function              |     |
|    [Hex]    |                                   |     |
+-------------+-----------------------------------+-----+
|     40      | Current Scene Data Dump           |  R  |
|     51      | Global Data Dump                  |  R  |
|     4F      | Scene Change                      | R,C |
|     23      | Data Load Completed (ACK)         |  E  |
|     24      | Data Load Error (NAK)             |  E  |
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
|   01    | Reply                                          |
|   0g    | g: MIDI Global Channel  ( Device ID )          |
|   dd    | Echo Back ID                                   |
|   12    | nanoPAD2 ID          ( Family ID   (LSB))      |
|   01    | nanoPAD2 ID          ( Family ID   (MSB))      |
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




2.Recognized Receive Data --------------------------------------------------

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
 Structure of nanoPAD2 System Exclusive Messages

 1st Byte = F0 : Exclusive Status
 2nd Byte = 42 : KORG
 3rd Byte = 4g : g : Global MIDI Channel
 4th Byte = 00 : Software Project (nanoPAD2: 000112H)
 5th Byte = 01 : 
 6th Byte = 12 : 
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
|  000 11111 (1F) | Data Dump Request                  *2 |
|  011 11111 (7F) | Data Dump                          *2 |
+-----------------+---------------------------------------+

 *2 :Function ID Code List
+-------------+-----------------------------------+
| Function ID | Description/Function              |
|    [Hex]    |                                   |
+-------------+-----------------------------------+
|     14      | Scene Change Request              |
|     10      | Current Scene Data Dump Request   |
|     0E      | Global Data Dump Request          |
|     40      | Current Scene Data Dump           |
|     51      | Global Data Dump                  |
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


3.MIDI Exclusive Format   (R:Receive, T:Transmit) --------------------------

3-1 Standard Messages

 (1) Current Scene Data Dump Request                                             R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0000 (10) | Current Scene Data Dump Request                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoPAD2 transmits a Func=40(Current Scene Data Dump)
 or a Func=24(NAK) message.

 (2) Global Data Dump Request                                                    R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0000 1110 (0E) | Global Data Dump Request                                         |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoPAD2 transmits a Func=51(Global Data Dump) or
 a Func=24(NAK) message.

 (3) Scene Write Request                                                         R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0001 (11) | Scene Write Request                                              |
| 0sss ssss (ss) | Destination Scene No.(0~3)                                       |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoPAD2 writes the current scene data to the
 internal memory and transmits Func=4F(Scene Change) and Func=21(Write Complete)
 messages or a Func=22(Write Error) message.

 (4) Scene Change Request                                                        R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0100 (14) | Scene Change Request                                             |
| 0sss ssss (ss) | Destination Scene No.(0~3)                                       |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoPAD2 loads the specified scene data to the
 current scene from the internal memory, and transmits Func=4F(Scene Change) and
 Func=23(ACK) messages or a Func=24(NAK) message.

 (5) Native mode In/Out Request                                                  R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0000 0000 (00) | Native mode In/Out Request (Host->Controller, 2Bytes Format)     |
| 0000 0000 (00) |                                                                  |
| 0qqq qqqq (qq) | qq = 00:Out Req, 01:In Req                                       |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive this message, and transmits Func=40 message.

 (6) Mode Request                                                                R,-
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0001 1111 (1F) | Data Dump Command  (Host->Controller, 2Bytes Format)             |
| 0001 0010 (12) | Mode Request                                                     |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive this message, and transmits Func=42 message.

 (7) Current Scene Data Dump                                                     R,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0111 1111 (7F) | Data Dump Command  (Host<->Controller, Variable Format)          |
| 0111 0000 (70) | Num of Data (1+111 Bytes : 1110000)                              |
| 0100 0000 (40) | Current Scene Data Dump                                          |
| 0ddd dddd (dd) | Data                                                    (NOTE 1) |
|     :          |  :                                                               |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When these message and data were received, the nanoPAD2 saves the data to the current
 scene, and transmits a Func=23(ACK) or a Func=24(NAK) message.
 When a Func=10(Current Scene Data Dump Request) message was received, the nanoPAD2
 transmits this message with the current scene data.

 (8) Global Data Dump                                                            R,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0111 1111 (7F) | Data Dump Command  (Host<->Controller, Variable Format)          |
| 0011 0111 (37) | Num of Data (1+54 Bytes : 110111)                                |
| 0101 0001 (51) | Global Data Dump                                                 |
| 0ddd dddd (dd) | Data                                                    (NOTE 2) |
|     :          |  :                                                               |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When this message was received, the nanoPAD2 saves them to the internal memory,
 and transmits a Func=23(ACK) or a Func=24(NAK) message.
 When a Func=0E(Global Data Dump Request) message was received, the nanoPAD2 transmits
 this message with the global data on the internal memory.

 (9) Data Load Completed (ACK)                                                   -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0011 (23) | Data Load Completed                                              |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the operation was completed, the nanoPAD2 transmits this message.

 (10) Data Load Error (NAK)                                                      -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0100 (24) | Data Load Error                                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the operation was not completed, the nanoPAD2 transmits this message.

 (11) Write Completed                                                            -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0001 (21) | Write Completed                                                  |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the writing operation was completed, the nanoPAD2 transmits this message.

 (12) Write Error                                                                -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | nanoPAD2 Exclusive Header  g;Global Channel  [Hex]               |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0010 0010 (22) | Write Error                                                      |
| 0000 0000 (00) |                                                                  |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the writing operation was not completed, the nanoPAD2 transmits this message.

 (13) Scene Change                                                               -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0100 1111 (4F) | Scene Change                                                     |
| 0sss ssss (ss) | Destination Scene No.(0~3)                                       |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
 When the nanoPAD2 completed to change the scene, it transmits this message.

 (14) Native mode In/Out                                                         -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0100 0000 (40) | Native mode In/Out  (Host<-Controller, 2Bytes Format)            |
| 0000 0000 (00) |                                                                  |
| 0rrr rrrr (rr) | rr = 02:Out,03:In                                                |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive Command=00 message, and transmits this message.

 (15) Mode Data                                                                  -,T
+----------------+------------------------------------------------------------------+
|     Byte       |                  Description                                     |
+----------------+------------------------------------------------------------------+
| F0,42,4g,      | Exclusive Header  g;Global Channel  [Hex]                        |
| 00,01,12,00,   | Software Project (nanoPAD2: 000112H, Sub ID: 00)                 |
| 0101 1111 (5F) | Data Dump Command  (Host<-Controller, 2Bytes Format)             |
| 0100 0010 (42) | Mode Data                                                        |
| 0mmm mmmm (rr) | mm = 00:Normal mode, 01:Native mode                              |
| 1111 0111 (F7) | End of Exclusive (EOX)                                           |
+----------------+------------------------------------------------------------------+
Receive Func=12 message, and transmits this message.

 NOTE 1: Current Scene Data Dump Format
         97 bytes = 7*13+6 -> 8*13+(6+1) => 111 bytes
         (TABLE 1)

 NOTE 2: Global Data Dump Format
         47 bytes = 7*6+5  -> 8*6+(5+1)  => 54 bytes
         (TABLE 2)

 NOTE 3: The Dump Data Conversion

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
+----------+-------------------------+---------------------------------------+
|          |     PARAMETER           |                VALUE                  |
+----------+-------------------------+---------------------------------------+
| Trigger Pad 1 parameters                                                   |
+----+-----+-------------------------+---------------------------------------+
| 0  |B7~5 | Assign type             | 0~3 = No Assign/Control Change/		 |
|    |     |                         |       Note/Program Change  			 |
|    +-----+-------------------------+---------------------------------------+
|    |B4~3 | Gate Arp Enable         | 0/1=Disable/Enable                    |
|    +-----+-------------------------+---------------------------------------+
|    |B2~1 | Pad Behavior            | 0/1=Momentary/Toggle                  |
|    +-----+-------------------------+---------------------------------------+
|    |B0   | TouchScale Gate Arp     | 0/1=Disable/Enable					 |
|    |     | Enable 				 |										 |
+----+-----+-------------------------+---------------------------------------+
| 1        | Note# 1 / CC# 1 /       | 0~127								 |
|          | Program Number          |                                       |
|          +-------------------------+---------------------------------------+
| 2        | Note# 2 / CC# 2         | 0~127/128~=0~127/No Assign            |
|          +-------------------------+---------------------------------------+
| 3        | Note# 3 / CC# 3         | 0~127/128~=0~127/No Assign            |
|          +-------------------------+---------------------------------------+
| 4        | Note# 4 / CC# 4         | 0~127/128~=0~127/No Assign            |
|          +-------------------------+---------------------------------------+
| 5        | MIDI Channel            | 0~16=0~15/Global MIDI Ch.             |
+----------+-------------------------+---------------------------------------+
|  6~11    | Trigger Pad 2 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 12~17    | Trigger Pad 3 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 18~23    | Trigger Pad 4 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 24~29    | Trigger Pad 5 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 30~35    | Trigger Pad 6 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 36~41    | Trigger Pad 7 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 42~47    | Trigger Pad 8 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 48~53    | Trigger Pad 9 assign    | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 54~59    | Trigger Pad 10 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 60~65    | Trigger Pad 11 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 66~71    | Trigger Pad 12 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 72~77    | Trigger Pad 13 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 78~83    | Trigger Pad 14 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 84~89    | Trigger Pad 15 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
| 90~95    | Trigger Pad 16 assign   | ( Same as the Trigger Pad 1 format )  |
+----------+-------------------------+---------------------------------------+
|    96    | Reserved                | 0x00                                  |
+----------+-------------------------+---------------------------------------+

 Total 97 bytes


 TABLE 2 : Global Parameter
+--------+-------------------------+---------------------------------------+
|        |     PARAMETER           |                VALUE                  |
+--------+-------------------------+---------------------------------------+
| Common                                                                   |
+--------+-------------------------+---------------------------------------+
| 0      | Global MIDI Ch.         | 0~15                                  |
|        +-------------------------+---------------------------------------+
| 1      | Velocity curve          | 0~3=Curve1~3/Const                    |
|        +-------------------------+---------------------------------------+
| 2      | Constant Velocity Value | 1~127                                 |
|        +-------------------------+---------------------------------------+
| 3~4    | BPM                     | 200~3000=BPM 20.0~300.0               |
|        +-------------------------+---------------------------------------+
| 5      | MIDI Clock              | 0~2=Auto/Internal/External            |
|        +-------------------------+---------------------------------------+
| 6      | Reserved                | 0x00                                  |
+--------+-------------------------+---------------------------------------+
| X-Y Pad                                                                  |
+--------+-------------------------+---------------------------------------+
|  (CC Mode)                                                               |
+--------+-------------------------+---------------------------------------+
| 7      | X-axis Assign Type      | 0~2=No Assign/CC/Pitch Bend           |
|        +-------------------------+---------------------------------------+
| 8      | X-axis CC Number        | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 9      | X-axis Polarity         | 0/1=Normal/Reverse                    |
|        +-------------------------+---------------------------------------+
| 10 	 | Reserved                | 0xFF                                  |
|        +-------------------------+---------------------------------------+
| 11     | Y-axis Assign Type      | 0~2=No Assign/CC/Pitch Bend           |
|        +-------------------------+---------------------------------------+
| 12     | Y-axis CC Number        | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 13     | Y-axis Polarity         | 0/1=Normal/Reverse                    |
|        +-------------------------+---------------------------------------+
| 14 	 | MIDI Channel            | 0~16=0~15/Global MIDI Ch.             |
|        +-------------------------+---------------------------------------+
| 15     | Touch Enable            | 0/1=Disable/Enable                    |
|        +-------------------------+---------------------------------------+
| 16     | Touch CC Number         | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 17     | Touch Off Value         | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 18     | Touch On Value          | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 19~20  | Reserved                | 0xFF                                  |
+--------+-------------------------+---------------------------------------+
|  (Touch Scale)                                                           |
+--------+-------------------------+---------------------------------------+
| 21     | Note On Velocity        | 1~127                                 |
|        +-------------------------+---------------------------------------+
| 22     | Y-axis CC Enable        | 0/1=Disable/Enable                    |
|        +-------------------------+---------------------------------------+
| 23     | Y-axis CC Number        | 0~127                                 |
|        +-------------------------+---------------------------------------+
| 24     | Y-axis Polarity         | 0/1=Normal/Reverse                    |
|        +-------------------------+---------------------------------------+
| 25     | MIDI Channel            | 0~16=0~15/Global MIDI Ch.             |
|        +-------------------------+---------------------------------------+
| 26     | Reserved                | 0xFF                                  |
|        +-------------------------+---------------------------------------+
| 27     | Gate Speed              | 0~10 *T-1                             |
|        +-------------------------+---------------------------------------+
| 28~29  | Reserved                | 0xFF                                  |
+--------+-------------------------+---------------------------------------+
| User Scale                                                               |
+--------+-------------------------+---------------------------------------+
| 30     | Length                  | 0~12                                  |
|        +-------------------------+---------------------------------------+
| 31     | Note Offset 1           | 0~+12                                 |
|        +-------------------------+---------------------------------------+
| 32~42  | Note Offset 2~12        | Same as Note Offset 1                 |
|        +-------------------------+---------------------------------------+
| 43~44  | Reserved                | 0xFF                                  |
+--------+-------------------------+---------------------------------------+
| 45~46  | Reserved                | 0xFF                                  |
+--------+-------------------------+---------------------------------------+

Total 47 bytes


 *T-1 Gate Speed List
 
   0: 0.062
   1: 0.125
   2: 0.25
   3: 0.333
   4: 0.5
   5: 0.666
   6: 0.75
   7: 1.0
   8: 1.333
   9: 1.5
  10: 2.0


4.Native KORG Mode Messages ----------------------------------------------------

(1) Native KORG mode Display LEDs
+--------+------------------+--------------------------------------+
| Status | Second   | Third |   Description                        |
| [Hex]  | [Hex]    | [Hex] |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  79~7C   |  ss   |  Scene 1~4 LED                       |
|        |          |       |  ss= Off:00~40 / On:41~7F            |
+--------+----------+-------+--------------------------------------+

(2) Native KORG Mode Trigger Pad Output
+--------+------------------+--------------------------------------+
| Status | Second   | Third |  Description                         |
| [Hex]  | [Hex]    | [Hex] |                                      |
+--------+----------+-------+--------------------------------------+
|   91   |  40~4F   |  vv   |  Trigger Pad 1~16 Note On            |
|        |          |       |  vv: velocity (01~7F)                |
+--------+----------+-------+--------------------------------------+
|   81   |  40~4F   |  40   |  Trigger Pad 1~16 Note Off           |
+--------+----------+-------+--------------------------------------+

(3) Native KORG Mode X-Y Pad Output
+--------+------------------+--------------------------------------+
| Status | Second   | Third |  Description                         |
| [Hex]  | [Hex]    | [Hex] |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  09      |  ss   |  X-axis                              |
|   BF   |  0A      |  ss   |  Y-axis                              |
+--------+----------+-------+--------------------------------------+
|   BF   |  0B      |  ss   |  Pad touch (ss= On:7F / Off:00)      |
+--------+----------+-------+--------------------------------------+
|   92   |  kk      |  7F   |  Touch Scale Note On                 |
|        |          |       |  kk: Note# (00~7F)                   |
+--------+----------+-------+--------------------------------------+
|   82   |  kk      |  40   |  Touch Scale Note Off                |
|        |          |       |  kk: Note# (00~7F)                   |
+--------+----------+-------+--------------------------------------+
|   B2   |  02      |  ss   |  Touch Scale Y-axis                  |
+--------+----------+-------+--------------------------------------+

(4) Native KORG mode Button Output
+--------+------------------+--------------------------------------+
| Status | Second   | Third |   Description                        |
| [Hex]  | [Hex]    | [Hex] |                                      |
+--------+----------+-------+--------------------------------------+
|   BF   |  39      |  ss   |  Scene Button                        |
|        |          |       |  ss: on/off (on = 7F Off = 00)       |
+--------+----------+-------+--------------------------------------+


*/

