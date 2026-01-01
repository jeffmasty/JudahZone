package net.judah.seq.beatbox;

import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.BoxLayout.Y_AXIS;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;

import judahzone.gui.Floating;
import judahzone.gui.Gui;
import lombok.Getter;
import net.judah.gui.Size;
import net.judah.seq.MusicBox;
import net.judah.seq.TrackList;
import net.judah.seq.Trax;
import net.judah.seq.automation.Automation;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.HiringAgency;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.TrackBindings;

/**Handles a top and bottom drum track */
public class DrumZone extends HiringAgency implements Floating {
	public static final String NAME = "BeatBox";

	static final int MUTES_CUTOUT = 19;

	@Getter private final TrackList<DrumTrack> tracks;
	@Getter private final ArrayList<DrumCage> views = new ArrayList<>();
	private final Mutes mutes1, mutes2;

	public DrumZone(TrackList<DrumTrack> list, Automation auto) {

		setName(NAME);
		this.tracks = list;
		Box menu1 = new Box(X_AXIS);
		Box layer1 = new Box(X_AXIS);
		Box menu2 = new Box(X_AXIS);
		Box layer2 = new Box(X_AXIS);

		mutes1 = new Mutes();
		mutes2 = new Mutes();

		DrumCage d1 = new DrumCage(tracks.get(Trax.D1.ordinal()), this, auto);
		DrumCage d2 = new DrumCage(tracks.get(Trax.D2.ordinal()), this, auto);
		DrumCage h1 = new DrumCage(tracks.get(Trax.H1.ordinal()), this, auto);
		DrumCage h2 = new DrumCage(tracks.get(Trax.H2.ordinal()), this, auto);
		views.add(d1);
		views.add(d2);
		views.add(h1);
		views.add(h2);

		menu1.add(d1.getMenu()); menu1.add(Box.createHorizontalStrut(1)); menu1.add(d2.getMenu());
		layer1.add(d1.getGrid()); layer1.add(mutes1); layer1.add(d2.getGrid());

		menu2.add(h1.getMenu()); menu2.add(Box.createHorizontalStrut(1)); menu2.add(h2.getMenu());
		layer2.add(h1.getGrid()); layer2.add(mutes2); layer2.add(h2.getGrid());


		setLayout(new BoxLayout(this, Y_AXIS));
		add(menu1); add(layer1);
		add(menu2); add(layer2);
		tracks.setCurrent(tracks.getFirst());
		resized(Size.TAB_SIZE.width, Size.TAB_SIZE.height);
		new TrackBindings(this);

	}

	public MidiTrack getCurrent() {
		return tracks.getCurrent();
	}

	public void setCurrent(MidiTrack track) {
		tracks.setCurrent(track);
	}

	public void update(DrumTrack t) {
		getView(t).update();
	}

	@Override
	public void resized(int w, int h) {

		Dimension howdy = new Dimension(w, h);
		if (w == Size.TAB_SIZE.width && h == Size.TAB_SIZE.height)
			howdy = new Dimension(w, h - Size.STD_HEIGHT - 2); // attached tab kludge

		Gui.resize(this, howdy);
		Dimension halfsies = quadrant(howdy);
		views.forEach(drum->drum.getGrid().setSize(halfsies));
		Dimension mutes = new Dimension(2 * MUTES_CUTOUT, h);
		Gui.resize(mutes1, mutes);
		Gui.resize(mutes2, mutes);
		doLayout();
	}

	@Override
	public MusicBox getMusician() {
		return getView(tracks.getCurrent()).grid;
	}

	public DrumCage getView(MidiTrack t) {
		for (DrumCage h : views)
			if (h.track == t)
				return h;
		return null;
	}

	private static Dimension quadrant(Dimension full) {
		return new Dimension(full.width / 2 - MUTES_CUTOUT, full.height / 2 - Size.KNOB_HEIGHT);
	}

}
