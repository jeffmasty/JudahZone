package net.judah.midi;

import java.util.ArrayList;
import java.util.Set;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.ZoneMidi;
import net.judah.gui.MainFrame;

@RequiredArgsConstructor @Getter
public class Actives extends ArrayList<ShortMessage> {
	
	protected final ZoneMidi midiOut;
	protected final int channel;
	/** max number of notes per channel */
	protected final int polyphony;

	// Polyphony class overrides for JudahSynth
	public void receive(ShortMessage msg) {
		if (Midi.isNoteOn(msg)) 
			add(msg);
		else 
			off(msg.getData1());
		MainFrame.update(this); 
	}

	/**@param ref  fill ref with data1 midi values of current voices */
	public void data1(Set<Integer> ref) {
		ref.clear();
		for (int i = 0; i < size(); i++)
			if (get(i) != null)
				ref.add(get(i).getData1());
	}

	public ShortMessage find(int data1) {
		for (int i = 0; i < size(); i++) 
			if (get(i) != null && get(i).getData1() == data1)
				return (get(i));
		return null;
	}

	@Override
	public void clear() {
		super.clear();
		MainFrame.update(this);
	}

	public void off(int data1) {
		ShortMessage midi = find(data1);
		while (midi != null) {
			remove(midi);
			MainFrame.update(this);
			midi = find(data1);
		}
	}

	public int indexOf(int data1) {
		for (int i = 0; i < size(); i++)
			if (get(i) != null && get(i).getData1() == data1)
				return i;
		return -1;
	}
	

}
