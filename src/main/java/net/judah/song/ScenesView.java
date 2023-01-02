package net.judah.song;

import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class ScenesView extends JPanel {

	private final int ROWS = 2;
	private final int COLS = 4;
	
	private final Song song;
	private final SongTab tab;
	
	public ScenesView(Song song, SongTab tab) {
		this.song = song;
		this.tab= tab;
		setLayout(new GridLayout(ROWS, COLS, 4, 8));
		fill();
	}
	
	public void fill() {
		removeAll();
		final int max = ROWS * COLS;
		int size = song.getScenes().size();
		if (size > max)
			size = max;
		for (int i = 0; i < size; i++) 
			add(new LaunchPad(song.getScenes().get(i), tab));
		
		if (size >= max) return;
		JLabel create = new JLabel("CREATE");
		create.setBorder(UIManager.getBorder("TitledBorder.border"));
		create.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
					tab.newScene();}});
		add(create);
		while (++size < max) 
			add(new JLabel(" ")); // filler
		update();
		repaint();
	}
	
	public void update() {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof LaunchPad)
				((LaunchPad)getComponent(i)).update();
		}
	}
	
}
