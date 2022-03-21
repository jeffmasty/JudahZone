package net.judah.tracks;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;

import javax.sound.midi.ShortMessage;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;

/** a midi track. 
 * Ch#,  fileType: step/midi, file, midiOut/(AudioIn)  */
@Data @EqualsAndHashCode(callSuper = false)
public abstract class Track extends JPanel implements TimeListener {

	public static enum Type {
		MIDI_POLY, MIDI_MONO, MIDI_DRUM, STEP_MONO, STEP_DRUM, STEP_POLY, ARP, CONTROLLER, AUDIO
	}
	
	protected final String name;
	protected final TimeProvider clock;
	protected final ArrayList<Pat> patterns = new ArrayList<>(); // ??
	
	protected int ch;
	protected Type type;
	protected final OUT initial;
	protected JackPort midiOut;
	protected File file;
	
	protected boolean active = false;
	protected float gain = 1;

	protected JButton sound;
	protected JLabel filename;
	protected JComboBox<Integer> channel;
	protected JComboBox<String> instruments;
	protected MidiOut outPort;
	
	public ShortMessage applyPortLogic(ShortMessage in) {
		return in; // TODO
	}
	
	public Track(TimeProvider clock, String name, Type type, int ch, final OUT port) {
		this.ch = ch;
		this.name = name;
		this.type = type;
		this.initial = port;
		this.clock = clock;
		
		add(new JLabel(getName()));
//		this.port = new JLabel("[" + getInitial().port + "]");
//		add(this.port);
		sound = new JButton("▶");
		sound.addActionListener(e -> {
			
			setActive(!isActive());
		});
		add(sound);
		outPort = new MidiOut(this);
		add(outPort);
		add(channelCombo());
		
		add(subGui());
		
	}
	
	private Component channelCombo() {
		channel = new JComboBox<Integer>();
		for (int i = 0; i < 16; i++)
			channel.addItem(i);
		if (isDrums()) {
			channel.setSelectedItem(9);
			channel.setEnabled(false);
		}
		return channel;
	}


	public Track(TimeProvider clock, String name, Type type, OUT port) {
		this(clock, name, type, type == Type.MIDI_DRUM || type == Type.STEP_DRUM ? 9 : 0, port);
	}

	public boolean isDrums() {
		return ch == 9;
	}
	
	public boolean isMono() {
		return type == Type.STEP_MONO || type == Type.MIDI_MONO;
	}
	
	public boolean isMidi() {
		return type == Type.MIDI_DRUM || type == Type.MIDI_MONO || type == Type.MIDI_POLY;
	}
	
	public void setActive(boolean active) {
		if (active) 
			clock.addListener(this);
		else 
			clock.removeListener(this);
		this.active = active;
		if (!JudahClock.getInstance().isActive()) JudahClock.getInstance().begin();
	}
	
	protected void update() {
		sound.setText(active ? "■" : "▶");
	}
	
	public JackPort getMidiOut() {
		if (midiOut != null) return midiOut;
		midiOut = JudahMidi.getByName(initial.getPort());
		return midiOut;
	}

	public abstract void setFile(File file);
	
	public abstract JPanel subGui();

	public void close() {
		clock.removeListener(this);
	}
		
	
	
	
	
//	/*----- TimeListener -----*/
//	@Override
//	public void update(Property prop, Object value) {
//		if (isMidi()) {
//			
//		}
//		else if (prop == Property.STEP)
//			steps.step((int)value);
//
//	}
	
}
