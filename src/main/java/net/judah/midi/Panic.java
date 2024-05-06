package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import lombok.RequiredArgsConstructor;
import net.judah.api.Engine;
import net.judah.drumkit.DrumKit;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class Panic implements Runnable  {

	private JackPort port;
	private Engine engine;

	private int channel = 0;
	
	public Panic(MidiTrack t) {
		if (t.getMidiOut() instanceof DrumKit) 
			return;
		if (t.getMidiOut() instanceof Engine) 
			new Panic((Engine)t.getMidiOut(), t.getCh());
		else 
			new Panic(((MidiInstrument)t.getMidiOut()).getMidiPort(), t.getCh());
	}
	
	public Panic(Engine e, int ch) {
		engine = e;
		channel = ch;
		Constants.execute(this);
	}

	public Panic(JackPort midi, int ch) {
		port = midi;
		channel = ch;
		Constants.execute(this);
	}
	
	@Override public void run() {
		try {
			if (port != null)
				for (int i = 0; i < 128; i++) 
					JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), port);
			else 
				for (int i = 0; i < 128; i++) 
					engine.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), JudahMidi.ticker());
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
	}

}
