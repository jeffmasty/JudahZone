package net.judah.seq.beatbox;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.drumkit.DrumSample;
import net.judah.gui.MainFrame;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiView;
import net.judah.seq.TrackList;
import net.judah.seq.track.MidiTrack;


/**Handles a top and bottom drum track */
public class BeatsTab extends MidiTab {
	public static final String NAME = "BeatBox";

	@Getter private final TrackList top = new TrackList();
	@Getter private final TrackList bottom = new TrackList();
	private final ArrayList<BeatsSection> bottomViews = new ArrayList<>();
	private final ArrayList<BeatsSection> topViews = new ArrayList<>();
	
	public BeatsTab(TrackList list) {
		super(list);
		setName(NAME);
		top.addAll(list);
		top.forEach(t->topViews.add(new BeatsSection(t, this, top)));
		top.init(top.get(0)); // drum1 
		bottom.addAll(list);
		bottom.forEach(t->bottomViews.add(new BeatsSection(t, this, bottom)));
		bottom.init(bottom.get(2)); // hats

		current = topViews.get(0);
	}
	
	@Override
	public MidiView getView(MidiTrack t) {
		BeatsSection it = getView(t, true);
		if (it != null)
			return it;
		return getView(t, false);
	}

	public BeatsSection getView(MidiTrack t, boolean upper) {
		for (BeatsSection v : upper ? topViews : bottomViews)
				if (v.getTrack() == t)
					return v;
		return null;
	}
	
	@Override
	public void changeTrack() {
		removeAll();
		add(getView(top.getCurrent(), true));
		add(getView(bottom.getCurrent(), false));
		repaint();
		MainFrame.qwerty();
	}
	
	public void midiUpdate(boolean upper) {
		if (upper)
			getView(top.getCurrent(), true).getInstrumentPanel().repaint();
		else
			getView(bottom.getCurrent(), false).getInstrumentPanel().repaint();

	}

	@Override public void update(MidiTrack o) {
		for (MidiView v : topViews)
			if (v.getTrack() == o)
				v.update();
		for (MidiView v : bottomViews)
			if (v.getTrack() == o)
				v.update();
	}
	public void update(DrumSample pad) {
		getView(top.getCurrent(), true).getMutes().update(pad);
		getView(bottom.getCurrent(), false).getMutes().update(pad);
	}

	public void setCurrent(MidiView view) {
		current = view;
		getView(top.getCurrent(), true).getMenu().update();
		getView(bottom.getCurrent(), false).getMenu().update();
		requestFocusInWindow();
	}
	
}
