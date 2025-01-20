package net.judah.midi;

import java.util.Vector;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.mixer.Instrument;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.taco.Polyphony;
import net.judah.util.RTLogger;

/** base function for an external synth */
@Getter
public class MidiInstrument extends Instrument implements ZoneMidi {

	@Setter protected JackPort midiPort;
	protected String[] patches = new String[] {};
	protected Vector<PianoTrack> tracks = new Vector<>();
	private Polyphony notes;

	public MidiInstrument(String channelName, String sourceLeft, String sourceRight,
			JackPort left, JackPort right, String icon, JackPort midi) {
		super(channelName, sourceLeft, sourceRight, icon);
		leftPort = left;
		rightPort = right;
		JudahZone.getServices().add(this);
		midiPort = midi;
	}

	/** mono-synth */
	public MidiInstrument(String name, String sourcePort, JackPort mono, String icon, JackPort midi) {
		super(name, sourcePort, mono, icon);
		JudahZone.getServices().add(this);
		midiPort = midi;
	}

	@Override public final void send(MidiMessage message, long timeStamp) {
		try {
			JackMidi.eventWrite(midiPort, (int)timeStamp,
					message.getMessage(), message.getLength());
			if (message instanceof ShortMessage)
				for (MidiTrack t : getTracks())
					if (t.getCh() == ((ShortMessage)message).getChannel())
						t.getActives().receive((ShortMessage)message);
		} catch (Exception e) {RTLogger.warn(this, e);}
	}

	@Override public void close() {
		new Panic(midiPort, 0); // ?
	}

	/** no-op, subclass override */
	@Override public boolean progChange(String preset, int ch) { return false; }
	/** no-op, subclass override */
	@Override public boolean progChange(String preset) { return false; }

	@Override
	public String getProg(int ch) {
		return "none";
	}

	public final MidiTrack trackByName(String name) {
		for (MidiTrack t : tracks)
			if (t.getName().equals(name))
				return t;
		return null;
	}

}
