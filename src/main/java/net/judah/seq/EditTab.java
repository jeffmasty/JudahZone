package net.judah.seq;

import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;

public class EditTab extends JPanel {

	public static final String NAME = "Seq";
	
	@Getter private final TrackList tracks;
	
	public EditTab(TrackList list) {
		this.tracks = list;
		list.setUpdate(()->changeTrack(tracks.getCurrent().getView()));
		setName(NAME);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		changeTrack(list.getCurrent().getView());
	}
	
	
	public void changeTrack(MidiView view) {
		new Thread(()->{
			removeAll();
			add(view);
			view.requestFocus();
			view.repaint();
		}).start();
	}
	
	public boolean keyPressed(KeyEvent e) {
//		if (current != null && current.viewVisible())
//			return current.getView().keyPressed(e);
		return false;
	}
	
	public boolean keyReleased(KeyEvent e) {
//		if (current != null && current.viewVisible())
//			return current.getView().keyReleased(e);
		return false;
	}


	
}
