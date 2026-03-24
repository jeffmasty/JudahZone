package net.judah.synth;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.api.MidiOut.MidiPiano;
import judahzone.api.MidiUpdate;
import judahzone.synth.Synth;
import judahzone.util.Notes;
import lombok.Getter;
import net.judah.seq.track.NoteTrack;


/** midi out wrapper to an FX bus */
public class TrackOut implements MidiPiano {

	public final int ch; // the channel for whom this wrapper is created.
	@Getter public final Notes notes;;
	private final SynthEngine owner;

	@Getter private String program = "";

	public TrackOut(SynthEngine engine, int idx, MidiUpdate gui) {
		this.owner = engine;
		this.ch = idx;
		notes = new Notes(this, gui, Synth.POLYPHONY);
	}

	public NoteTrack getTrack() {
		return owner.getTracks().stream().filter(t -> t.getCh() == ch).findFirst()
				.orElseThrow(() -> new NullPointerException("No track for channel " + ch));
	}
	@Override public String[] getPatches() {
		return owner.getPatches();
	}
	@Override public String progChange(int data2) {
		return owner.progChange(data2,  ch);
	}
	@Override public int progChange(String preset) {
		return owner.progChange(preset, ch);
	}
	@Override
	public int[] actives() {
		return getTrack().getActiveSnapshot();
	}
	@Override
	public void off(int data1) {
		notes.off(data1);
	}
	@Override
	public void send(MidiMessage message, long timeStamp) { // TODO Notes integration?
		if (message instanceof ShortMessage shrt) {
			if (shrt.getChannel() == ch)
				owner.send(message, timeStamp); // write
			else
				owner.send(Midi.format(shrt, ch, 1f), timeStamp);
		}
	}

	@Override
	public void close() { // remove track?
	}
	@Override
	public boolean isPedal() {
		return notes.isPedal();
	}
	@Override
	public void setPedal(boolean hold) {
		notes.setPedal(hold);
	}
	@Override
	public void on(ShortMessage velocity) {
		notes.on(velocity);
	}

	@Override
	public int getProgChange() {
		String[] o = owner.getPatches();
		for (int i = 0; i < o.length; i++) {
			if (o[i].equals(program))
				return i;
		}
		return -1;
	}


}
