package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.RequiredArgsConstructor;
import net.judah.drums.DrumKit;
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

//	@Override public void run() {
//		try {
//			if (port != null)
//				for (int i = 0; i < 128; i++)
//					JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), port);
//			else
//				for (int i = 0; i < 128; i++)
//					engine.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), JudahMidi.ticker());
//
//		} catch (InvalidMidiDataException e) {
//			RTLogger.warn(this, e);
//		}
//	}

    @Override public void run() {
        try {
            sendPanicInternal(port, engine, channel);
        } catch (InvalidMidiDataException e) {
            RTLogger.warn(this, e);
        }
    }

	// new synchronous helper: send panic now on calling thread
    public static void panicNow(JackPort port, int channel) {
        try {
            sendPanicInternal(port, null, channel);
        } catch (Throwable t) {
            RTLogger.warn("Panic", t);
        }
    }

    public static void panicNow(Engine engine, int channel) {
        try {
            sendPanicInternal(null, engine, channel);
        } catch (Throwable t) {
            RTLogger.warn("Panic", t);
        }
    }

    private static void sendPanicInternal(JackPort port, Engine engine, int channel) throws InvalidMidiDataException {
        try {
            // Preferred: minimal CC panic messages
            ShortMessage reset = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 121, 0);
            ShortMessage allSoundOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 120, 0);
            ShortMessage allNotesOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 123, 0);

            if (port != null) {
                JudahMidi.queue(reset, port);
                JudahMidi.queue(allSoundOff, port);
                JudahMidi.queue(allNotesOff, port);
            } else if (engine != null) {
                engine.send(reset, JudahMidi.ticker());
                engine.send(allSoundOff, JudahMidi.ticker());
                engine.send(allNotesOff, JudahMidi.ticker());
            }
        } catch (InvalidMidiDataException e) {
            // fallback to 128 NOTE_OFFs
            if (port != null) {
                for (int i = 0; i < 128; i++)
                    JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), port);
            } else if (engine != null) {
                for (int i = 0; i < 128; i++)
                    engine.send(new ShortMessage(ShortMessage.NOTE_OFF, channel, i, 0), JudahMidi.ticker());
            }
        }
    }

}
