package net.judah.midi;

import java.util.ArrayList;
import java.util.Set;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.api.MidiReceiver;
import net.judah.gui.MainFrame;

@RequiredArgsConstructor @Getter
public class Actives extends ArrayList<ShortMessage> {
	
	protected final MidiReceiver midiOut;
	protected final int channel;
	/** max number of notes per channel */
	protected final int polyphony;

	public void receive(ShortMessage msg) {
		if (Midi.isNote(msg) == false) 
			return;
		if (Midi.isNoteOn(msg)) {
			if (add(msg))
				MainFrame.update(this);
		}
		else if (remove(msg))
			MainFrame.update(this); 
	}


	public void ints(Set<Integer> ref) {
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
		ShortMessage m = find(data1);
		if (m == null) return;
		remove(m);
		MainFrame.update(this);
	}

	public int indexOf(int data1) {
		for (int i = 0; i < size(); i++)
			if (get(i) != null && get(i).getData1() == data1)
				return i;
		return -1;
	}
	

}
