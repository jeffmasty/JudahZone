package net.judah.mixer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.midi.MidiPort;
import net.judah.midi.Panic;

/** base function for an external mono-synth */
public class MidiInstrument extends Instrument implements MidiReceiver {

	@Getter protected final MidiPort midiPort;
	@Getter private String[] patches = new String[] {"none"};
	@Getter @Setter private int channel = 0;
	// boolean doesProgChange
	// boolean isMono
	
	public MidiInstrument(String channelName, String sourceLeft, String sourceRight, MidiPort midi, String icon) {
		super(channelName, sourceLeft, sourceRight, icon);
		this.midiPort = midi;
		JudahZone.getServices().add(this);
	}

	/** mono-synth */
	public MidiInstrument(String name, String sourcePort, MidiPort midi, String icon) {
		super(name, sourcePort, (JackPort)null, icon);
		this.midiPort = midi;
		JudahZone.getServices().add(this);
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		midiPort.send((ShortMessage)message, (int)timeStamp);
	}

	@Override
	public void close() {
		new Panic(midiPort, 0 /*monosynth*/).start();
	}

	@Override public void progChange(String preset, int ch) {	}
	@Override public void progChange(String preset) {
		// no-op, please override
	}

	@Override
	public int getProg(int ch) {
		return 0;
	}

}
