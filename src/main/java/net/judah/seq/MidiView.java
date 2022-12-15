package net.judah.seq;

import static net.judah.JudahZone.getSeq;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.midi.JudahClock;
import net.judah.seq.beatbox.BeatSteps;
import net.judah.seq.beatbox.BeatBox;
import net.judah.seq.beatbox.DrumMutes;
import net.judah.seq.piano.PianoGrid;
import net.judah.seq.piano.Piano;
import net.judah.seq.piano.PianoSteps;
import net.judah.widgets.Knob;

public class MidiView extends JPanel implements MidiSize {
	
	public static enum Source { Menu, Pianist, Steps, Grid, RecPlay }

	private final JudahClock clock;
	@Getter private final MidiTrack track;
	@Getter private final MidiMenu menu;
	@Getter private final Steps steps;
	@Getter private final JPanel grid;
	@Getter private final JPanel instrumentPanel;
	
	@Getter private final Snippet snippet = new Snippet();
	@Getter private final ArrayList<MidiPair> selected = new ArrayList<>();
	@Getter @Setter private long timeframe; // 2 bars
	@Getter private final Knob velocity = new Knob(val->{ });
	@Getter private final JComboBox<Gate> gate = new JComboBox<>(Gate.values());
	@Getter private final JCheckBox live = new JCheckBox("live");

	private final JLabel playWidget = new JLabel("  ▶️");
	private final JLabel recWidget = new JLabel("  ◉");
	
	public MidiView(MidiTrack t) {
		this.track = t;
		this.clock = track.getClock();
		timeframe = clock.getMeasure() * track.getResolution() * 2;

		menu = new MidiMenu(BOUNDS_MENU, this, t.isDrums() ? getSeq().getDrumTracks() : getSeq().getSynthTracks());
		

		if (track.isDrums()) {
			steps = new BeatSteps(BOUNDS_PIANIST, track);
			instrumentPanel = new DrumMutes(BOUNDS_STEPS, track); 
			grid = new BeatBox(BOUNDS_GRID, this, steps);
		}
		else {
			Piano piano = new Piano(BOUNDS_PIANIST, track);
			instrumentPanel = piano;
			PianoSteps currentBeat = new PianoSteps(BOUNDS_STEPS, track, this);
			steps = currentBeat;
			grid = new PianoGrid(BOUNDS_GRID, this, currentBeat, piano);
		}
		setLayout(null);
		add(menu);
		add(steps);
		add(grid);
		add(instrumentPanel);

		playRecord();
	}
	
	public void update() {
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : null);
		recWidget.setBackground(track.isRecord() ? Pastels.RED : null);
		invalidate();
		steps.repaint();
		menu.update();
		menu.repaint();
		grid.repaint();
	}
	
	private void playRecord() {
		Rectangle playBounds = new Rectangle(0, 10, STEP_WIDTH, Size.STD_HEIGHT);
		Rectangle recBounds = new Rectangle(0, BOUNDS_PIANIST.y, STEP_WIDTH, Size.STD_HEIGHT);
		playWidget.setBounds(playBounds);
		recWidget.setBounds(recBounds);
		playWidget.addMouseListener(new MouseAdapter() { 
			@Override public void mouseClicked(MouseEvent e) {
			if (track.isActive() || track.isOnDeck()) 
				track.setActive(false);
			else track.setActive(true);}});
		recWidget.addMouseListener(new MouseAdapter() { 
			@Override public void mouseClicked(MouseEvent e) {
			track.setRecord(!track.isRecord());
		}});
		playWidget.setOpaque(true);
		recWidget.setOpaque(true);
		add(playWidget);
		add(recWidget);
	}

	
	public static int toData1(Point p) {
		return p.x / KEY_WIDTH + NOTE_OFFSET;
	}
	public static int toX(int data1) {
		return (data1 - NOTE_OFFSET) * KEY_WIDTH;
	}

	public static long toTick(Point p, long twoBars) {
		return (long) (ratioY(p.y, BOUNDS_GRID.height) * twoBars);
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
	
	public static Source source(Point p) {
		if (p.y < BOUNDS_PIANIST.y)
			return Source.Menu;
		if (p.x < BOUNDS_STEPS.width)
			if (p.y > BOUNDS_PIANIST.y)
				return Source.Steps;
			else 
				return Source.RecPlay;
		if (p.y < BOUNDS_PIANIST.y)
			return Source.Menu;
		if (p.y >= BOUNDS_GRID.y)
			return Source.Grid;
		return Source.Pianist;
	}

}
