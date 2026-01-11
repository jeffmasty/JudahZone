// Java
package net.judah.seq.piano;

import java.awt.Dimension;

import judahzone.api.Key;
import judahzone.gui.Floating;
import judahzone.gui.Gui;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.gui.Size;
import net.judah.seq.Seq;
import net.judah.seq.track.HiringAgency;
import net.judah.seq.track.MusicBox;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.TrackBindings;


public class PianoView extends HiringAgency implements Floating, Size {

	public enum Orientation { NOTES_X, NOTES_Y};

	public static final int MAX_OCTAVES = 7;
	public static final int MAX_RANGE = MAX_OCTAVES * Key.OCTAVE;
	public static final int DEFAULT_RANGE = 24;
	public static final int DEFAULT_TONIC = 24;
	private static final Dimension PANIC = new Dimension(UNIT, UNIT);

	@Getter final PianoTrack track;
	@Getter final Piano grid;
	@Getter private final PianoMenu menu;
	@Getter private final PianoSteps steps;
	private final PianoKeys keyboard;

	int range = DEFAULT_RANGE;
	int tonic = DEFAULT_TONIC;
	float scaledWidth;
	private int pianoWidth;
	private final Pedal pedal;

	// Y-Notes toggle for piano roll display
	@Getter private Orientation orientation = Orientation.NOTES_X;

	public PianoView(PianoTrack t, Seq seq) {
		this.track = t;
		setName(t.getName());

		keyboard = new PianoKeys(track, this);
		steps = new PianoSteps(track, seq.getAutomation());
		grid = new Piano(this, steps, keyboard);
		menu = new PianoMenu(this, grid, seq);
		pedal = t.getPedal();
		Gui.resize(pedal, PANIC);

		menu.setLocation(0, 0);
		// pedal.setLocation(0, MENU_HEIGHT);
		pedal.setBounds(0, MENU_HEIGHT, UNIT, UNIT);

		setLayout(null);
		add(menu);
		add(pedal);
		add(keyboard);
		add(steps);
		add(grid);
	    pedal.setBounds(0, MENU_HEIGHT, UNIT, UNIT);
		setOrientation(Orientation.NOTES_X);

		new TrackBindings(this);
	}

	@Override
	public void resized(int w, int h) {
	    Dimension box = new Dimension(w, h);
	    Gui.resize(this, box);
	    setMinimumSize(box);

	    int gridHeight = h - (UNIT + Size.MENU_HEIGHT + 1);
	    if (h == Size.HEIGHT_TAB && w == Size.WIDTH_TAB)
	        gridHeight -= (Size.STD_HEIGHT + 3);

	    menu.resized(w, MENU_HEIGHT);
	    menu.setBounds(0, 0, w, MENU_HEIGHT);

	    if (orientation == Orientation.NOTES_X) {
	        // Standard: steps (left, narrow) | grid (right, wide)
	        pianoWidth = w - (UNIT + 2);
	        scaledWidth = scale();

	        steps.resized(UNIT, gridHeight - 1);
	        keyboard.resized(pianoWidth, UNIT);
	        grid.resized(pianoWidth, gridHeight - 1);

	        steps.setBounds(0, MENU_HEIGHT + UNIT, UNIT, gridHeight);
	        keyboard.setBounds(UNIT, MENU_HEIGHT, pianoWidth, UNIT);
	        grid.setBounds(UNIT, MENU_HEIGHT + UNIT, pianoWidth, gridHeight);
	    } else { // NOTES_Y
	        // Rotated: steps (top, wide) | grid (bottom, tall)
	        pianoWidth = w;
	        scaledWidth = scale();

	        // ensure steps width excludes left `UNIT` where pedal/keyboard live
	        steps.resized(pianoWidth - UNIT, UNIT);
	        keyboard.resized(UNIT, gridHeight - 1);
	        grid.resized(pianoWidth, gridHeight - 1);

	        steps.setBounds(UNIT, MENU_HEIGHT, pianoWidth - UNIT, UNIT);
	        keyboard.setBounds(0, MENU_HEIGHT + UNIT, UNIT, gridHeight);
	        grid.setBounds(UNIT, MENU_HEIGHT + UNIT, pianoWidth, gridHeight);
	    }

	    doLayout();
	}


	/** Set the visible range and center note for the piano view.
	    Ensures bounds are valid and notifies grid and keyboard. */
	public void setRangeAndTonic(int rangeSemitones, int centerNote) {
	    // Clamp range to valid bounds
	    if (rangeSemitones < Key.OCTAVE)
	        rangeSemitones = Key.OCTAVE;
	    if (rangeSemitones > MAX_RANGE)
	        rangeSemitones = MAX_RANGE;

	    // Calculate tonic (lowest note) from center
	    int newTonic = centerNote - (rangeSemitones / 2);

	    // Clamp tonic to keep range within 0–127
	    if (newTonic < 0)
	        newTonic = 0;
	    if (newTonic + rangeSemitones > 127)
	        newTonic = 127 - rangeSemitones;

	    // Update state only if changed
	    boolean rangeChanged = (range != rangeSemitones);
	    boolean tonicChanged = (tonic != newTonic);

	    if (!rangeChanged && !tonicChanged)
	        return;

	    range = rangeSemitones;
	    tonic = newTonic;
	    scaledWidth = scale();
	    grid.calculateUnits();
	    keyboard.calculateUnits();
	    refresh();
	}

	private float scale() {
		return pianoWidth / (float) (range + 1);
	}

	public void refresh() {
		grid.repaint();
		keyboard.repaint();
	}

	public void update() {
		menu.update();
		grid.repaint();
	}

	@Override
	public MusicBox getMusician() {
		return grid;
	}

	@Override
	public void unregister() {
		RTLogger.debug(this, "Unregsitered");
		track.getEditor().removeListener(grid);
		track.getEditor().removeListener(steps);
	}

	public void setOrientation(Orientation o) {
	    orientation = o;
	    final int topBanner = Size.MENU_HEIGHT + UNIT + 1;
	    final int gridWidth = getWidth() - UNIT;
        final int gridHeight = getHeight() - topBanner;

	    if (orientation == Orientation.NOTES_X) {
	        // original layout: steps on left, keys on top
	        keyboard.setOrientation(orientation, gridWidth, UNIT); // top
	        steps.setOrientation(orientation, UNIT, gridHeight); // left
	        grid.setOrientation(orientation, gridWidth, gridHeight);

	        keyboard.setBounds(UNIT, MENU_HEIGHT, gridWidth, UNIT); // top
	        steps.setBounds(0, topBanner, UNIT, gridHeight);		// left
	        grid.setBounds(UNIT, topBanner, gridWidth, gridHeight);

	    } else { // NOTES_Y
	        // Rotated layout: steps on top, keys on left
	        steps.setOrientation(orientation, gridWidth, UNIT); // top
	        keyboard.setOrientation(orientation, UNIT, gridHeight); // left
	        grid.setOrientation(orientation, gridWidth, gridHeight);

	        steps.setBounds(UNIT, MENU_HEIGHT, gridWidth, UNIT); // top
	        keyboard.setBounds(0, topBanner, UNIT, gridHeight);	 // left
	        grid.setBounds(UNIT, topBanner, gridWidth, gridHeight);
	    }

	    doLayout();
	    repaint();
	}

	public void flip() {
		setOrientation(orientation == Orientation.NOTES_X ? Orientation.NOTES_Y : Orientation.NOTES_X);
	}


}
