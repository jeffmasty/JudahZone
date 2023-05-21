package net.judah.seq;

import javax.sound.midi.MidiEvent;
import javax.swing.JButton;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;

public class Recorder extends JButton {

	private final MidiTrack track;
	@Getter private boolean active;
	@Setter @Getter private boolean transpose;
	
	public Recorder(MidiTrack track) {
		super(" ◉ ");
		this.track = track;
		addActionListener(e->toggle());
		setOpaque(true);
	}
	
	public boolean record(Midi midi) {
		if (!active)
			return false;
		long tick = track.quantize(track.getRecent());
		if (tick < track.getRecent()) 
			track.getMidiOut().send(midi, JudahMidi.ticker());

		if (Midi.isNoteOff(midi)) {
			// undoable	
			track.getT().add(new MidiEvent(midi, tick));
		}
		MainFrame.update(track);
		return true;
	}

	public void setActive(boolean active) {
		this.active = active;
		if (active)
    		JudahZone.getMidi().setKeyboardSynth(track);
		JudahZone.getMidiGui().record(active);
		MainFrame.update(track);
	}
	
	public void update() {
		setBackground(active ? Pastels.RED : null);
	}

	public void toggle() { 
    	setActive(!isActive());
	}
    

	
	
}
