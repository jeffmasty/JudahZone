package net.judah.seq;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.seq.track.MidiTrack;

@Getter
public abstract class MidiTab extends JPanel {

	protected final TrackList tracks;
	protected MidiView current;
	
	/** ticks are zero-based */
	protected final Clipboard clipboard = new Clipboard();
	
	public MidiTab(TrackList list) {
		this.tracks = list;
		list.setUpdate(()->MainFrame.setFocus(list));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}
	
	public abstract void changeTrack();

	public final Musician getMusician() {
		return current.getGrid();
	} 
	
	public abstract void update(MidiTrack t);

}
