package net.judah.seq.beatbox;

import java.awt.Point;

import lombok.Getter;
import net.judah.seq.MidiView;
import net.judah.seq.TrackList;
import net.judah.seq.track.Gate;
import net.judah.seq.track.TrackMenu;
import net.judah.seq.track.MidiTrack;

public class BeatsSection extends MidiView implements BeatsSize {
	
	@Getter private final DrumMutes mutes;
	@Getter private final BeatSteps steps;
	
	public BeatsSection(MidiTrack t, BeatsTab tab, TrackList tracks) {
		super(t);
		steps = new BeatSteps(BEAT_STEPS, track);

		mutes = new DrumMutes(BOUNDS_MUTES, this); 
		menu = new TrackMenu(BOUNDS_MENU, this, tracks, tab);
		grid = new BeatBox(BEATBOX_GRID, this, tab);
		instrumentPanel = mutes;
		
		setLayout(null);
		add(menu);
		add(grid);
		add(instrumentPanel);
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
