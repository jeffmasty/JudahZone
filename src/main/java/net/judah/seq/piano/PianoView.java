package net.judah.seq.piano;

import lombok.Getter;
import net.judah.seq.MidiMenu;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;

@Getter 
public class PianoView extends MidiView implements PianoSize {
	
	public PianoView(MidiTrack t, MidiTab tab) {
		super(t);
		Piano piano = new Piano(BOUNDS_PIANIST, track, tab);
		instrumentPanel = piano;
		
		steps = new PianoSteps(PIANO_STEPS, this);
		menu = new MidiMenu(BOUNDS_MENU, this, tab.getTracks(), tab);
		grid = new PianoBox(PIANO_GRID, this, (PianoSteps)steps, piano, tab);
		setLayout(null);
		add(menu);
		add(instrumentPanel);
		add(steps);
		add(grid);
	}
	
}
