package net.judah.plugin;

import java.util.ArrayList;

import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Midi;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.midi.MidiClock;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// cc 102 = Next Part

// investigate beat buddy sent: 240.127.127/0  176.102.2/0 

// 	implementation todo:
// song select
// cc   folder select lsb
// cc   folder select msb
// cc   data increment
// cc   data decrement
// tempo cc lsb msb
// volume
// player control
// play stop transition
//  ports:
// a2j:Komplete Audio 6 [??24??] (capture): Komplete Audio 6 MIDI 1  -->   JudahMidi:midiIn
public class BeatBuddy implements MidiClock {
	private static final int NORMAL_VOL = 100;
	
	public static final Midi CYMBOL_HIT = _createCC(110, 9, NORMAL_VOL);
	public static final Midi PLAY_MIDI = _createCC(114, 0, 100);
	public static final Midi PAUSE_MIDI = _createCC(111, 0, 100);

	static Midi _createCC(int dat1, int channel,int dat2) {
		Midi temp = null;
		try {
			temp = new Midi(ShortMessage.CONTROL_CHANGE, channel, dat1, dat2);
		} catch (Throwable t) {System.err.print(t.getMessage());}
		return temp;
	}

	
	@Setter JackPort out;
	ArrayList<TimeListener> listeners = new ArrayList<>();

	int tempo;
	@Getter int beat;
	@Getter boolean play;
	
	int pulse;
	long ticker;
	long delta;
	Midi queue;
	
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

	@Override
	// public void process(Midi midi) {
	public void process(byte[] midi) {
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
		
		if (queue != null) {
			try {
				JackMidi.eventWrite(out, 0, queue.getMessage(), queue.getLength());
			} catch (JackException e) { RTLogger.warn(this, e); }
			queue = null;
		}
		

	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}

	@Override
	public float getTempo() {
		return tempo;
	}

	/** no-op, tempo is set by midi process() */
	@Override
	public boolean setTempo(float tempo) {
		return false;
	}

	/** no-op */
	@Override
	public int getMeasure() {
		return 0;
	}

	/** no-op */
	@Override
	public void setMeasure(int bpb) {
	}

	@Override
	public long getLastPulse() {
		return ticker;
	}

}
