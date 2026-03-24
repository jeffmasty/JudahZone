package net.judah.synth;

import java.util.Vector;

import javax.sound.midi.InvalidMidiDataException;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.MidiOut.MidiBus;
import judahzone.api.MidiUpdate;
import judahzone.util.Constants;
import lombok.Getter;
import net.judah.channel.LineIn;
import net.judah.midi.ChannelCC;
import net.judah.seq.chords.Chords;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;

/**
 * Audio FX bus responds to multi-channel Midi *
 * ZoneMidi plumbing: ports, patches and track list.
 *
 * • Subclasses (FluidSynth, TacoTruck, DrumMachine, etc. )
 */
public abstract class SynthEngine extends LineIn implements MidiBus {

	@Getter protected boolean external;
	protected String commandLine;

	// optional JACK ports / names used by external engines
	protected final String leftPortName;
	protected final String rightPortName;
	@Getter protected final JackPort leftPort;
	@Getter protected final JackPort rightPort;
	protected final JackPort midiPort;
	protected ChannelCC cc = new ChannelCC(this);

	public SynthEngine(String name, int channels) {
		super(name, channels == Constants.STEREO);
		leftPortName = null;
		rightPortName = null;
		leftPort = null;
		rightPort = null;
		midiPort = null;
	}

	public SynthEngine(String name, boolean isStereo) {
		super(name, isStereo);
		leftPortName = null;
		rightPortName = null;
		leftPort = null;
		rightPort = null;
		midiPort = null;
	}

	/**
	 * Extended constructor for external engines that expose JACK ports / a MIDI
	 * input port. Subclasses that previously depended on MidiInstrument-style
	 * construction can call this.
	 */
	public SynthEngine(String name, String leftPortName, String rightPortName, JackPort leftPort, JackPort rightPort,
			String image, JackPort midiPort, boolean isStereo) {
		super(name, isStereo);
		this.leftPortName = leftPortName;
		this.rightPortName = rightPortName;
		this.leftPort = leftPort;
		this.rightPort = rightPort;
		this.midiPort = midiPort;
	}

	/** Create a track and add it to the managed track list */
	public abstract PianoTrack addTrack(String name, MidiUpdate gui, Chords c) throws InvalidMidiDataException;

	public abstract Vector<? extends NoteTrack> getTracks();

	@Override
	public int[] actives() { // union?
		return getTracks().isEmpty() ? new int[0] : getTrack().getActiveSnapshot();
	}

	@Override
	public void off(int data1) {
		// TODO
	}

	public NoteTrack getTrack() {
		Vector<? extends NoteTrack> tracks = getTracks();
		if (tracks == null || tracks.isEmpty())
			return null;
		return tracks.get(0);
	}

	@Override public String[] getPatches() { return new String[0]; }

	/**@param preset name of prog change
	 * @return name of prog change idx on success or -1 */
	@Override public final int progChange(String preset) { return progChange(preset, 0); }

	@Override public final int getProgChange() { return getProgChange(0); }

	@Override public final String getProgram() {  return getProgram(0); }

	@Override public final String progChange(int data2) { return progChange(data2, 0); }

//	/** Default progChange behavior: map index to patch name and send program change
//	 * to the engine's MIDI input port when available. Subclasses may override. */
//	@Override
//	public String progChange(int data1, int ch) {
//		if (data1 < 0 || data1 > 127)
//			return null;
//		for (int i = 0; i < tracks.size(); i++) {
//			NoteTrack t = tracks.get(i);
//			if (t.getCh() == ch) {
//				return t.progChange(data1);
//			}
//		}
//		return null;
//	}

///**
// * Helper to send a program change into the engine's MIDI port and update track UIs. */
//protected void sendProg(int idx, int ch, String name) {
//    try {
//        if (midiPort != null) {
//            JudahMidi.queue(Midi.create(ShortMessage.PROGRAM_CHANGE, ch, idx, 0), midiPort);
//        }
//        // Update any tracks bound to this channel
//        for (NoteTrack p : tracks)
//            if (p.getCh() == ch)
//                MainFrame.updateTrack(Update.PROGRAM, p);
//    } catch (Exception ignored) {
//        // keep failures non-fatal for engine plumbing
//    }
//}


}