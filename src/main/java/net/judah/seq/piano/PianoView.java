package net.judah.seq.piano;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Panic;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiView;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.TrackMenu;

@Getter 
public class PianoView extends MidiView implements PianoSize {
	
	public PianoView(MidiTrack t, MidiTab tab) {
		super(t);
		Piano piano = new Piano(BOUNDS_PIANIST, track, tab);
		instrumentPanel = piano;
		
		steps = new PianoSteps(PIANO_STEPS, this);
		menu = new TrackMenu(BOUNDS_MENU, this, tab.getTracks(), tab);
		grid = new PianoBox(PIANO_GRID, this, (PianoSteps)steps, piano, tab);
		setLayout(null);
		add(menu);
		add(instrumentPanel);
		add(steps);
		add(grid);
		
		Btn panic = new Btn(" ! ", e->new Panic(t), "MidiOut Panic");
		panic.setFont(Gui.BOLD);
		panic.setBounds(0, MENU_HEIGHT, STEP_WIDTH, KEY_HEIGHT);
		add(panic);
	}
	
}
