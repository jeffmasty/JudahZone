package net.judah.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Custom;
import judahzone.api.Midi;
import judahzone.api.MidiOut.MidiPiano;
import judahzone.api.MidiUpdate;
import judahzone.util.Notes;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.Instrument;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.PianoTrack;

/** base function for a synth with effects
 *
 *  Contains a MidiOut and a PianoTrack */
@Getter
public final class MidiInstrument extends Instrument implements MidiPiano {

	public static enum Type { SYNTH, DRUM, BOTH }

	@Setter protected JackPort midiPort;
	private final Notes notes;
	private final PianoTrack track;
	private final ChannelCC cc = new ChannelCC(this);
	private String[] patches = new String[0];

	public MidiInstrument(Custom user, MidiUpdate gui, Chords chords) {
		super(user);

		String type = user.engine(); // wav/pluck/fm/?fluid?bass?
		RTLogger.log(this, "Creating MidiInstrument with type: " + type);

		notes = new Notes(this, gui, 8); // TODO monophonic (crave)
		track = new PianoTrack(user.name(), 0, this, this, chords);
		track.setPermanent(true);
	}

	@Override public void off(int data1) {
		try {
			ShortMessage msg = new ShortMessage(ShortMessage.NOTE_OFF, track.getCh(), data1, 0);
			write(msg);
			notes.off(data1);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	@Override public void on(ShortMessage velocity) {
		send(velocity, JudahMidi.ticker());
	}

	@Override public final void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage msg = (ShortMessage)midi;
		if (cc.process(msg))
			return; // channel filtered

		if (Midi.isProgChange(midi))  // Should have been filtered by MidiTrack?
			return;

		if (Midi.isNoteOn(msg))
			notes.on(msg);
		else if (Midi.isNoteOff(msg)) {
			notes.off(msg.getData1());
			if (notes.isPedal())
				return;
		} else {
			RTLogger.warn(this, "Unhandled MIDI message: " + Midi.toString(msg));
			return;
		}
		write(Midi.format(msg, track.getCh(), track.getAmp()));
	}

	void write(ShortMessage msg) {
		try {
			JackMidi.eventWrite(midiPort, JudahMidi.ticker(),
					msg.getMessage(), msg.getLength());
		} catch (Exception e) {RTLogger.warn(this, e);}
	}



	@Override public void close() {
		panic();
	}

    @Override
	public void panic() {
        try {
        	int channel = track.getCh();
            // Preferred: minimal CC panic messages
            ShortMessage reset = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 121, 0);
            ShortMessage allSoundOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 120, 0);
            ShortMessage allNotesOff = new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, 123, 0);

            if (midiPort != null) {
                JudahMidi.queue(reset, midiPort);
                JudahMidi.queue(allSoundOff, midiPort);
                JudahMidi.queue(allNotesOff, midiPort);
            }

        } catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
    }


	@Override public int getProgChange() { return 0; }
	@Override public String getProgram() { return ""; }
	@Override public String progChange(int data2) { return ""; }
	@Override public int progChange(String preset) { return 0; }
	@Override public int[] actives() { return notes.actives(); }
	@Override public boolean isPedal() { return notes.isPedal(); }
	@Override public void setPedal(boolean hold) { notes.setPedal(hold); }

}
