package net.judah.midi;

import java.util.Vector;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Midi;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.Instrument;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.synth.ZoneMidi;

/** base function for an external synth, subclasses handle midi-channel awareness*/
@Getter
public class MidiInstrument extends Instrument implements ZoneMidi {

	public static enum Type { SYNTH, DRUM, BOTH }

	protected ChannelCC cc = new ChannelCC(this);
	@Setter protected JackPort midiPort;
	protected String[] patches = new String[] {};
	protected final Vector<PianoTrack> tracks = new Vector<>();

	// Stereo
	public MidiInstrument(String channelName, String sourceLeft, String sourceRight,
			JackPort left, JackPort right, String icon, JackPort midi) {
		super(channelName, sourceLeft, sourceRight, icon);
		leftPort = left;
		rightPort = right;
		init(midi);
	}

	/** mono-synth */
	public MidiInstrument(String name, String sourcePort, JackPort mono, String icon, JackPort midi) {
		super(name, sourcePort, mono, icon, 45, 11000);
		init(midi);
	}

	private void init(JackPort midi) {
		midiPort = midi;
		Services.add(this);
	}

	@Override public final void send(MidiMessage midi, long timeStamp) {
		if (midi instanceof MetaMessage)
			return; // TODO
		ShortMessage msg = (ShortMessage)midi;
		if (cc.process(msg))
			return; // channel filtered

		if (Midi.isProgChange(midi))  // Should have been filtered by MidiTrack
			return;

		ShortMessage shrt = (ShortMessage)midi;
		PianoTrack out = null;
		for (PianoTrack t : getTracks())
			if (t.getCh() == msg.getChannel()) {
				out = t;
				break;
			}
		if (out == null)
			out = tracks.getFirst();

		if (out.getActives().receive(shrt)) // Pedal handling
			write(Midi.format(msg, out.getCh(), out.getAmp()));
	}

	void write(ShortMessage msg) {
		try {
			JackMidi.eventWrite(midiPort, JudahMidi.ticker(),
					msg.getMessage(), msg.getLength());
		} catch (Exception e) {RTLogger.warn(this, e);}
	}
	@Override public void close() {
		Panic.panicNow(midiPort, 0);
	}

//	/** no-op, subclass override */
	@Override public String progChange(int data2, int ch) { return null; }

	public final MidiTrack trackByName(String name) {
		for (MidiTrack t : tracks)
			if (t.getName().equals(name))
				return t;
		return null;
	}

	@Override public PianoTrack getTrack() {
		if (tracks.isEmpty()) return null;
		return tracks.getFirst();
	}

}
