package net.judah.seq;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.seq.track.MidiTrack;

@Getter
public abstract class MidiTab extends JPanel {

	protected final TrackList tracks;
	/** ticks are zero-based */
	protected final Clipboard clipboard = new Clipboard();

	protected MidiView current;

	public MidiTab(TrackList list) {
		this.tracks = list;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}
	
	public final Musician getMusician() {
		return current.getGrid();
	} 

	public abstract void changeTrack();
	
	public abstract void update(MidiTrack t);
	
	public abstract MidiView getView(MidiTrack t);

}
