package net.judah.seq;

import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.seq.track.MidiTrack;

@Getter
public abstract class MidiTab extends JPanel {

	protected TrackList<? extends MidiTrack> tracks;

	/** ticks are zero-based */
	protected final Clipboard clipboard = new Clipboard();

	public MidiTab(TrackList<? extends MidiTrack> list) {
		this.tracks = list;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}

	public final Musician getMusician() {
		return getView(tracks.getCurrent()).getGrid();
	}

	public MidiTrack getCurrent() {
		return tracks.getCurrent();
	}

	public abstract void changeTrack();

	public abstract void update(MidiTrack t);

	public abstract MidiView getView(MidiTrack t);

	public MidiView getView() {
		return getView(tracks.getCurrent());
	}

	public void setCurrent(MidiTrack track) {
		if (JudahZone.isInitialized() == false)
			return;
		tracks.setCurrent(track);
		MainFrame.setFocus(this);
	}

	public Vector<? extends MidiTrack> getTracks() {
		return tracks;
	};

}
