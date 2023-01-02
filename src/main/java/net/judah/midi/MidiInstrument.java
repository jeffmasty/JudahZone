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

/** base function for an external synth */
@Getter
public class MidiInstrument extends Instrument implements MidiReceiver {

	protected final ArrayList<Integer> actives = new ArrayList<>();
	protected String[] patches = new String[] {"none"};
	@Setter protected MidiPort midiPort;
	@Setter protected int channel;
	@Setter protected float amplification = 0.9f;
	
	// boolean doesProgChange
	// boolean isMono
	
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
			msg = Midi.create(msg.getCommand(), msg.getChannel(), msg.getData1(), Math.round(msg.getData2() * amplification));
		}
		else if (Midi.isNote(msg))
			actives.remove((Integer)msg.getData1());
		
		midiPort.send(msg, (int)timeStamp);
	}

	@Override
	public void close() {
		new Panic(midiPort, 0 /*monosynth*/).start();
	}

	@Override public void progChange(String preset, int ch) {	}
	@Override public void progChange(String preset) {
		// no-op, subclass override
	}

	@Override
	public int getProg(int ch) {
		return 0;
	}

}
