package net.judah.midi;

import java.util.List;
import java.util.Vector;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.mixer.Instrument;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;

/** base function for an external synth */
public class MidiInstrument extends Instrument implements MidiReceiver {

	@Setter @Getter protected JackPort midiPort;
	@Getter protected String[] patches = new String[] {};
	@Getter protected Vector<MidiTrack> tracks = new Vector<>();
	
	public MidiInstrument(String channelName, String sourceLeft, String sourceRight, JackPort left, JackPort right, String icon, JackPort midi) {
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

	@Override
	public final void send(MidiMessage message, long timeStamp) {
//		midiPort.send(message, (int)timeStamp);
		JudahMidi.queue(message, midiPort);
//		channels.send(message, timeStamp, jackPort);
//		MainFrame.update();
//		channels.update((ShortMessage)message);

	}

	@Override
	public void close() {
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

//	public void setMono() {
//		mono = true;
//	}
	
	public static void offs(MidiReceiver out, int ch, List<Integer> actives) {
		Constants.execute(()->{
			for (int i = 0; i < actives.size(); i++) 
				out.send(Midi.create(ShortMessage.NOTE_OFF, ch, actives.get(i), 0), JudahMidi.ticker());
			actives.clear();
		});
	}

}
