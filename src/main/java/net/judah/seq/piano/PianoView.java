package net.judah.seq.piano;

import lombok.Getter;
import net.judah.api.Key;
import net.judah.gui.Gui;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Panic;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiView;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.TrackMenu;
import net.judah.util.RTLogger;

@Getter
public class PianoView extends MidiView implements PianoSize {
	public static final int MAX_OCTAVES = 7;
	public static final int MAX_RANGE = MAX_OCTAVES * Key.OCTAVE;
	public static final int DEFAULT_RANGE = 5 * Key.OCTAVE;
	public static final int DEFAULT_TONIC = 24;

	int range = DEFAULT_RANGE;
	int tonic = DEFAULT_TONIC;
	float scaledWidth = scale();

	public PianoView(MidiTrack t, MidiTab tab) {
		super(t);
		Piano piano = new Piano(BOUNDS_PIANIST, track, this);
		instrumentPanel = piano;

		steps = new PianoSteps(PIANO_STEPS, this);
		menu = new TrackMenu(BOUNDS_MENU, this, tab.getTracks(), tab);
		grid = new PianoBox(PIANO_GRID, this, (PianoSteps)steps, piano, tab);
		add(menu);
		add(instrumentPanel);
		add(steps);
		add(grid);

		Btn panic = new Btn(" ! ", e->new Panic(t), t.getName() + " Panic");
		panic.setFont(Gui.BOLD);
		panic.setBounds(0, MENU_HEIGHT, STEP_WIDTH, KEY_HEIGHT);
		add(panic);
	}

	public void tonic(boolean up) {
		setTonic(tonic + (up ? 12 : -12));
	}

	public void setTonic(int data1) {
		if (data1 < 0)
			data1 = DEFAULT_TONIC;
		if (data1 > MAX_RANGE - range)
			data1 = MAX_RANGE - range;
		tonic = data1;
		refresh();
	}

	public void setRange(int interval) {
		if (interval < Key.OCTAVE)
			interval = Key.OCTAVE;
		if (tonic + interval > 127)
			interval = 127 - tonic;
		if (range == interval)
			return;
		range = interval;
		scaledWidth = scale();
		refresh();
	}

	private float scale() {
		return PIANO_GRID.width / (float) (range + 1);
	}

	private void refresh() {
		grid.repaint();
		instrumentPanel.repaint();
		RTLogger.log(this, toString());
	}

	@Override
	public String toString() {
		return "bass " + Key.key(tonic) + tonic/12 + " range " + range + " width " + scaledWidth;
	}
}
