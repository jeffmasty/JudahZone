package net.judah.seq;

import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;

@Getter
public abstract class MidiTab extends JPanel {

	protected final TrackList tracks;
	protected MidiView current;
	protected final ArrayList<MidiPair> clipboard = new ArrayList<>();
	
	public MidiTab(TrackList list) {
		this.tracks = list;
		list.setUpdate(()->MainFrame.setFocus(list));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}
	
	public abstract void changeTrack();

	public final Musician getMusician() {
		return current.getGrid().getMusician();
	} 
	
	public abstract void update(MidiTrack t);

}
