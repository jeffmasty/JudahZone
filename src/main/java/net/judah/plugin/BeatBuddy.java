package net.judah.plugin;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Midi;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.midi.MidiClock;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** listens to an incoming Midi Clock stream and knows how to communicate to a Beat Buddy drum machine */
@Log4j
public class BeatBuddy implements MidiClock {
	
	// investigate beat buddy sent: 240.127.127/0  176.102.2/0 
	// Song select is prog change
	public static final int FOLDER_MSB = 0;
	public static final int FOLDER_LSB = 32;
	public static final int DATA_UP = 96;
	public static final int DATA_DOWN = 97;
	public static final int REG_DATA_1 = 98; // assign data up/down to tempo, data2 = 106 
	public static final int REG_DATA_2 = 99; // assign data up/down to temp, data2 = 107
	
	public static final int NEXT_PART = 102;
	public static final int TEMPO_MSB = 106;
	public static final int TEMPO_LSB = 107;
	public static final int VOLUME = 108;
	public static final int CYMBOL = 110;
	public static final int CONTINUE = 111;
	public static final int DRUM_FILL = 112;
	public static final int TRANSITION = 113; // data2 = song part number
	public static final int START = 114;
	public static final int OUTRO = 115;
	public static final int DRUMSET = 116; // data2 = set number
	public static final int TAPT_MODE = 117; // enter tap tempo mode
	
	private static final int NORMAL_VOL = 100;
	public static final Midi CYMBOL_HIT = _createCC(CYMBOL, 9, NORMAL_VOL);
	public static final Midi PLAY_MIDI = _createCC(START, 0, NORMAL_VOL);
	public static final Midi PAUSE_MIDI = _createCC(CONTINUE, 0, NORMAL_VOL);

	static Midi _createCC(int dat1, int channel,int dat2) {
		Midi temp = null;
		try {
			temp = new Midi(ShortMessage.CONTROL_CHANGE, channel, dat1, dat2);
		} catch (Throwable t) {log.error(t.getMessage(), t);}
		return temp;
	}
	
	@Setter JackPort out;
	ArrayList<TimeListener> listeners = new ArrayList<>();

	@Getter float tempo;
	@Getter int beat;
	@Getter boolean play;
	
	int pulse;
	long ticker;
	long delta;
	@Getter @Setter Midi queue;
	
	public void play(boolean play) {
		this.play = play;
		if (play) 
			queue = PLAY_MIDI;
		else 
			queue = PAUSE_MIDI;
	}
	
	public void play() {
		if (play == false) {
			play = true;
			queue = PLAY_MIDI;
			return;
		}
		queue = PAUSE_MIDI;
	}

	@Override public void addListener(TimeListener l) {
		if (!listeners.contains(l)) listeners.add(l); }

	@Override public void removeListener(TimeListener l) {
		listeners.remove(l); }

	/** no-op, tempo is set by midi process() */
	@Override public boolean setTempo(float tempo) { return false; }

	/** no-op */
	@Override public int getMeasure() { return 0; }

	/** no-op */
	@Override public void setMeasure(int bpb) { }

	@Override public long getLastPulse() { 
		return ticker; }
	
	public void setVolume(int vol) {
		queue = Midi.create(Midi.CONTROL_CHANGE, 0, 108, vol);
	}
	
	public void send(int command, int value) {
		queue = Midi.create(Midi.CONTROL_CHANGE, 0, command, value);
		log.info("buddy queue'd " + queue);
	}
	
	
	@Override
	public void processTime(byte[] midi) {
		int stat;
		if (midi.length == 1)
			stat = midi[0] & 0xFF;
		else if (midi.length == 2) {
			stat = midi[1] & 0xFF;
			stat = (stat << 8) | (midi[0] & 0xFF);
		}
		else {
			stat = 0;
			RTLogger.log(this, midi.length + " " + new Midi(midi));
		}
		
		if (ShortMessage.TIMING_CLOCK == stat) {
			pulse++;
			if (pulse == 1) {
				ticker = System.currentTimeMillis();
			}
			if (pulse == 25) {
				listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
			}
			if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
				listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
				delta = System.currentTimeMillis() - ticker;
				float temptempo = Constants.toBPM(delta, 2);
				assert temptempo > 0 : temptempo;
				// RTLogger.log(this, "tempo: " + temptempo);
				if (Math.round(temptempo) != tempo) {
					tempo = Math.round(temptempo);
					listeners.forEach(l -> {l.update(Property.TEMPO, 
							tempo); });
					// RTLogger.log(this, "TEMPO! : " + tempo);
				}
				pulse = 0;
			}
		}

		else if (ShortMessage.START == stat) { 
			RTLogger.log(this, "MIDI START!");
			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
					JackTransportState.JackTransportStarting); });
			beat = 0;
			pulse = 0;
		}
		
		else if (ShortMessage.STOP == stat) {
			RTLogger.log(this, "MIDI STOP");
			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
					JackTransportState.JackTransportStopped); });
		}
		
		else if (ShortMessage.CONTINUE == stat) {
			RTLogger.log(this, "MIDI CONTINUE");
			listeners.forEach(l -> {l.update(Property.TRANSPORT, 
					JackTransportState.JackTransportRolling); });

		}
		else 
			RTLogger.log(this, "unknown beat buddy " + new Midi(midi));
	}

	public void song(int num) {
		if (num < 0 || num > 127) return;
		queue = Midi.create(Midi.PROGRAM_CHANGE, 0, num, 0);
		
	}

}

/*
Tempo control
Since the BeatBuddy’s tempo ranges from 40BPM - 300BPM, we can’t
use just one CC command to cover the whole range because a CC
command can only have 128 values. There are two ways of changing the
tempo. With INC/DEC (increase/decrease) messages which adjust the
tempo up or down by 1 BPM, or by skipping directly to a specific tempo
BPM using the MSB/LSB system, like in the Song Selection system
(Program Change). However, it’s a bit more complicated because unlike
the Song Selection system where you have dedicated CC commands to
respresent the MSB (CC-0) and LSB (CC-32), there is no dedicated CC
commands for Tempo. So we use the “NRPN Register” (Non-Registered
Parameter Number) which is a general purpose MSB (CC-99) and LSB
(CC-98) that can be used to control tempo or any other parameter, or
multiple parameters at once. Currently we’re only using it to control
tempo, but we follow the MIDI Standard protocol to leave room for
further control in the future. Because of this capability for multiple
parameter control, the steps below are followed.
INC/DEC​
 (see. https://www.midi.org/specifications) ​
 ​
Here are the common steps to do to control the BeatBuddy’s tempo. It
follows the Data INC/DEC specification of the MIDI protocol:
Step to increment tempo
Step
 Message
 Details
s
1*
 CC–99 / 106
 Set the NRPN MSB register to Tempo MSB
2*
 CC–98 / 107
 Set the NRPN LSB register to Tempo LSB
3
 CC–96 / 1
 Increment the tempo by one
4*
 CC–99 / 127
 Clear the NRPN MSB register
 5*
 CC–98 / 127
 Clears the NRPN LSB register
Step to decrement tempo
Step
 Message
 Details
s
1*
 CC–99 / 106
 Set the NRPN MSB register to Tempo MSB
2*
 CC–98 /107
 Set the NRPN LSB register to Tempo LSB
3
 CC–97 / 1
 Decrement the tempo by one
4*
 CC–99 / 127
 Clear the NRPN MSB register
5*
 CC–98 / 127
 Clears the NRPN LSB register
Steps with a * are optional if the only value control by Inc/Dec is the
Tempo. By default, the Beatbuddy will increment / decrement the tempo
when receiving a INC/DEC message.
Tempo MSB & Tempo LSB
To directly set the tempo to a specific BPM, we need to use the Tempo
MSB and Tempo LSB. The Beatbuddy will update its current tempo only
when receiving the LSB message. So the order of the message should
be:
1. MSB value
2. LSB value
Don’t forget, the value of the Tempo can only be set with both MSB
(CC-106) and LSB (CC-107).
Here are a few examples of Midi message combination.
MSB
 LSB
 Tempo
(CC-106)
 (CC-107)
0
 25
 40
0
 40
 40
0
 127
 127
1
 0
 128
1
 25
 153
1
 50
 178
2
 0
 256
2
 44
 300
2
 45
 300
MIDI Commands
Program Change (CC) summary
CC
 Value
 Action
Numbe
r
CC-0
 [0-127]
 Bank (Song folder) Select MSB
CC-32
 [0-127]
 Bank (Song folder) Select LSB
CC-96
 [1-127]
 Data increment (+1) – INC
CC-97
 [1-127]
 Data decrement (-1) – DEC

 
 */
