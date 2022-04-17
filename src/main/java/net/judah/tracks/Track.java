package net.judah.tracks;

import java.io.File;

import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.MainFrame;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;

/** a midi track. 
 * Ch#,  fileType: step/midi, file, midiOut/(AudioIn)  */
@Data @EqualsAndHashCode
public abstract class Track implements TimeListener {

	public static enum Type {
		MIDI_POLY, MIDI_MONO, MIDI_DRUM, STEP_DRUM, DRUM_KIT //, STEP_MONO, STEP_POLY, ARP, CONTROLLER, AUDIO
	}
	
	protected final String name;
	protected final JudahClock clock;

	protected Type type;
	protected File file;
	protected final File folder;
	protected JackPort midiOut;
	protected final OUT initial;
	protected int ch;
	
	protected boolean active = false;
	protected float gain = 1;

	protected JPanel feedback = new JPanel();
	
	public Track(JudahClock clock, String name, Type type, int ch, final OUT port, File folder) {
		this.ch = ch;
		this.name = name;
		this.type = type;
		this.initial = port;
		this.clock = clock;
		this.folder = folder;
	}
	
	public Track(JudahClock clock, String name, Type type, OUT port, File folder) {
		this(clock, name, type, type == Type.MIDI_DRUM || type == Type.STEP_DRUM ? 9 : 0, port, folder);
	}

	public boolean isDrums() {
		return ch == 9;
	}
	
	public boolean isMono() {
		return type == Type.MIDI_MONO;
	}
	
	public boolean isMidi() {
		return type == Type.MIDI_DRUM || type == Type.MIDI_MONO || type == Type.MIDI_POLY;
	}
	
	public void setActive(boolean active) {
		this.active = active;
		if (active) 
			clock.addListener(this);
		else 
			clock.removeListener(this);
		if (!JudahClock.getInstance().isActive()) JudahClock.getInstance().begin();
		MainFrame.update(this);
	}
	
	
	public JackPort getMidiOut() {
		if (midiOut != null) return midiOut;
		midiOut = JudahMidi.getByName(initial.getPort());
		return midiOut;
	}

	public abstract void setFile(File file);

	public void close() {
		clock.removeListener(this);
	}

	public abstract boolean process(int knob, int data2);
		
	public boolean setMidiOut(JackPort out) {
		if (midiOut == out) 
			return false;
		midiOut = out;
		return true;
	}

	public void selectFile(int data2) {
		
	}
	
}
