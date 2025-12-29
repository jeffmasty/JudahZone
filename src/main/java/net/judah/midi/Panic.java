package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.RequiredArgsConstructor;
import net.judah.drumkit.DrumKit;
import net.judah.seq.track.NoteTrack;
import net.judah.synth.Engine;
import net.judah.synth.ZoneMidi;

@RequiredArgsConstructor
public class Panic implements Runnable  {

	private JackPort port;
	private Engine engine;

	private int channel = 0;

	public Panic(NoteTrack t) {
		if (t.getMidiOut() instanceof DrumKit)
			return;
		if (t.getMidiOut() instanceof Engine)
			new Panic((Engine)t.getMidiOut(), t.getCh());
		else
			new Panic(((MidiInstrument)t.getMidiOut()).getMidiPort(), t.getCh());
	}

	public Panic(ZoneMidi it) {
		if (it instanceof DrumKit)
			return;
		if (it instanceof Engine)
			new Panic((Engine)it, 0);
		else if (it instanceof MidiInstrument)
			new Panic( ((MidiInstrument)it).getMidiPort(), 0);
	}

	public Panic(Engine e, int ch) {
		engine = e;
		channel = ch;
		Threads.execute(this);
	}

	public Panic(JackPort midi, int ch) {
		port = midi;
		channel = ch;
		Threads.execute(this);
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
