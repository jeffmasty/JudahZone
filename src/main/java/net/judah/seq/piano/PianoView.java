package net.judah.seq.piano;

import java.awt.Point;

import lombok.Getter;
import net.judah.seq.Gate;
import net.judah.seq.MidiMenu;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;

@Getter 
public class PianoView extends MidiView implements PianoSize {
	
	private final PianoSteps steps;
	
	public PianoView(MidiTrack t, MidiTab tab) {
		super(t);
		Piano piano = new Piano(BOUNDS_PIANIST, track, tab);
		instrumentPanel = piano;
		steps = new PianoSteps(PIANO_STEPS, track, this);
		PianoMusic notes = new PianoMusic(PIANO_GRID, this, steps, piano, tab);
		grid = notes;
		menu = new MidiMenu(BOUNDS_MENU, this, tab.getTracks(), tab);
		setLayout(null);
		add(menu);
		add(steps);
		add(grid);
		add(instrumentPanel);
	}
	
	@Override
	public void update() {
		invalidate();
		steps.repaint();
		menu.update();
		menu.repaint();
		grid.repaint();
	}

	
	public static int toData1(Point p) {
		return p.x / KEY_WIDTH + NOTE_OFFSET;
	}
	public static int toX(int data1) {
		return (data1 - NOTE_OFFSET) * KEY_WIDTH;
	}

	public static int toY(long tick, long measure, int height) {
		return (int) (ratioY(tick, measure) * height);
	}
	
	public static float ratioY(long tick, long measure) {
		return tick / (float)measure;
	}

	public static long quantize(long tick, int resolution, Gate gate) { // TODO only does 1/16th quantization for now
		long unit = (long) (0.25f * resolution);
		float units = tick / unit;
		return (long)units * unit;
	}
	
}
