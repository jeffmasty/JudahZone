package net.judah.midi;

import java.util.ArrayList;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.mixer.Instrument;
import net.judah.util.Constants;

/** base function for an external synth */
@Getter
public class MidiInstrument extends Instrument implements MidiReceiver {

	protected final ArrayList<Integer> actives = new ArrayList<>();
	protected String[] patches = new String[] {};
	@Setter protected MidiPort midiPort;
	boolean mono;
	
	public MidiInstrument(String channelName, String sourceLeft, String sourceRight, JackPort left, JackPort right, String icon) {
		super(channelName, sourceLeft, sourceRight, icon);
		leftPort = left;
		rightPort = right;
		JudahZone.getServices().add(this);
	}
	
	/** mono-synth */
	public MidiInstrument(String name, String sourcePort, JackPort mono, JackPort midi, String icon) {
		super(name, sourcePort, mono, icon);
		JudahZone.getServices().add(this);
		setMidiPort(new MidiPort(midi));
	}

	@Override
	public final void send(MidiMessage message, long timeStamp) {
		ShortMessage msg = (ShortMessage)message;
		if (Midi.isNoteOn(msg)) {
			actives.add(msg.getData1());
		}
		else if (Midi.isNote(msg))
			actives.remove((Integer)msg.getData1());
		
		midiPort.send(msg, (int)timeStamp);
	}

	@Override
	public void close() {
		Constants.execute(new Panic(midiPort, 0 /*monosynth*/));
	}

	/** no-op, subclass override */
	@Override public void progChange(String preset, int ch) {	}
	/** no-op, subclass override */
	@Override public void progChange(String preset) { }

	@Override
	public String getProg(int ch) {
		return "none";
	}

	public void setMono() {
		mono = true;
	}

}
