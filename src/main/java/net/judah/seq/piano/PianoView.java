package net.judah.seq.piano;

import java.awt.Dimension;

import lombok.Getter;
import net.judah.api.Key;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.gui.Detached.Floating;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Panic;
import net.judah.seq.MusicBox;
import net.judah.seq.track.HiringAgency;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackBindings;

@Getter
public class PianoView extends HiringAgency implements TimeListener, Floating, Size {
	public static final int MAX_OCTAVES = 7;
	public static final int MAX_RANGE = MAX_OCTAVES * Key.OCTAVE;
	public static final int DEFAULT_RANGE = 5 * Key.OCTAVE;
	public static final int DEFAULT_TONIC = 24;
	private static final Dimension PANIC = new Dimension(STEP_WIDTH, KEY_HEIGHT);

	protected final MidiTrack track;
	protected final PianoMenu menu;
	protected final Piano grid;
	protected final PianoKeys keyboard;

	int range = DEFAULT_RANGE;
	int tonic = DEFAULT_TONIC;

	private final PianoSteps steps;

	float scaledWidth;
	private int pianoWidth;
	Btn panic;

	public PianoView(PianoTrack t) {
		this.track = t;
		setName(t.getName());

		keyboard = new PianoKeys(track, this);
		steps = new PianoSteps(this);
		grid = new Piano(this, steps, keyboard);
		menu = new PianoMenu(this, grid);
		panic = (Btn) Gui.resize(new Btn(" ! ", e->new Panic(t), t.getName() + " Panic"), PANIC);
		panic.setFont(Gui.BOLD);
		track.getClock().addListener(this);

		menu.setLocation(0, 0);
		panic.setLocation(0, MENU_HEIGHT);
		keyboard.setLocation(STEP_WIDTH, MENU_HEIGHT);
		steps.setLocation(0, MENU_HEIGHT + KEY_HEIGHT);
		grid.setLocation(STEP_WIDTH, MENU_HEIGHT + KEY_HEIGHT);

		setLayout(null);
		add(menu);
		add(panic);
		add(keyboard);
		add(steps);
		add(grid);

		new TrackBindings(this);
	}

	@Override public void resized(int w, int h) {
		pianoWidth = w - (STEP_WIDTH - 2);
		scaledWidth = scale();
		int gridHeight = h - (KEY_HEIGHT + Size.MENU_HEIGHT + 1);
		if (h == Size.HEIGHT_TAB && w == Size.WIDTH_TAB)
			gridHeight -= (Size.STD_HEIGHT + 3); // Magic for attached tab// titleBar
		Dimension box = new Dimension(w, h);
		Gui.resize(this, box);
		setMinimumSize(box);
		menu.resized(w, MENU_HEIGHT);
		keyboard.resized(w - (STEP_WIDTH + 1), KEY_HEIGHT);
		steps.resized(STEP_WIDTH, gridHeight - 1);
		grid.resized(pianoWidth, gridHeight - 1);
		menu.setBounds(menu.getLocation().x, menu.getLocation().y, w, MENU_HEIGHT);
		keyboard.setBounds(keyboard.getLocation().x, keyboard.getLocation().y, w - (STEP_WIDTH + 1), KEY_HEIGHT);
		steps.setBounds(0, MENU_HEIGHT + KEY_HEIGHT, STEP_WIDTH, gridHeight);
		grid.setBounds(STEP_WIDTH, MENU_HEIGHT + KEY_HEIGHT, pianoWidth, gridHeight);
		panic.setBounds(0, MENU_HEIGHT, STEP_WIDTH, KEY_HEIGHT);
		doLayout();
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
		return pianoWidth / (float) (range + 1);
	}

	private void refresh() {
		grid.repaint();
		keyboard.repaint();
	}

	public void update() {
		menu.update();
		grid.repaint();
	}

	@Override
	public void update(Property prop, Object value) {
		// if (prop == Property.STEP && track.isActive() && isVisible()) {
		// steps.setStart((int)value); // waterfall
		// grid.repaint();
		if (value instanceof Signature sig) {
			grid.timeSig(sig);
			steps.timeSig(sig);
		}
	}

	@Override
	public MusicBox getMusician() {
		return grid;
	}


}
