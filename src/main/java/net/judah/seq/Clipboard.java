package net.judah.seq;

import java.util.ArrayList;

import javax.sound.midi.Track;

import lombok.RequiredArgsConstructor;

/** add undo redo peek */
@RequiredArgsConstructor
public class Clipboard {
	
	private final Track t;
	private final MusicBox b;
	
	private ArrayList<Edit> stack = new ArrayList<>();
	private int caret;
	
	
	private void execute() {
		Edit e = stack.get(caret);
		switch (e.getType()) {
		case DEL:
			for (MidiPair p: e.getNotes()) {
				MidiTools.delete(p.getOn(), t);
				if (p.getOff() != null)
					MidiTools.delete(p.getOff(), t);
            }
			b.selected.clear();
			b.repaint();
			break;
		case NEW:
			b.selected.clear(); // add zero-based selection
			for (MidiPair p : e.getNotes()) {
				t.add(p.getOn());
				t.add(p.getOff());
				b.selected.add(p); 
			}
			break;
		default:
			break;
		}
	}
	
	public void add(Edit e) {
		stack.add(e);
		caret = stack.size() - 1;	
		execute();
	}
	
	public Edit peek() {
		if (caret >= stack.size())
			return null;
		return stack.get(caret);
	}
	
	public boolean undo() {
		if (stack.size() <= caret) {
			return false;
		}
		Edit e = stack.get(caret);
		switch (e.getType()) {
		case DEL:
			
			break;
		case NEW:
			
		default:
			break;
		}
		caret--;
		return true;
	}
	
	public boolean redo() {
		if (stack.size() <= caret + 1) 
			return false;
		caret++;
		execute();
		return true;
	}
	
	

}
