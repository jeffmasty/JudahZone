package net.judah.song;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.judah.gui.Pastels;

/** launch scenes */
public class ScenesView extends JPanel {
	private static final int ROWS = 2; // 3 rows allows 12 scenes
	private static final int COLS = 4;
	public static final int MAX = ROWS * COLS;
	
	private final Song song;
	private final SongTab tab;
	
	public ScenesView(Song song, SongTab tab) {
		this.song = song;
		this.tab= tab;
		setLayout(new GridLayout(ROWS, COLS, 4, 8));
		fill();
		setBackground(Color.WHITE);
	}
	
	public void fill() {
		removeAll();
		int size = song.getScenes().size();
		if (size > MAX)
			size = MAX;
		for (int i = 0; i < size; i++) 
			add(new LaunchPad(song.getScenes().get(i), tab));
		
		if (size >= MAX) return;
		JLabel create = new JLabel("CREATE");
		create.setBorder(UIManager.getBorder("TitledBorder.border"));
		create.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
					tab.newScene();}});
		create.setBackground(Pastels.BUTTONS);
		create.setOpaque(true);
		add(create);
		while (++size < MAX) 
			add(new JLabel()); // filler
		update();
	}
	
	public void update(Scene s) {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof LaunchPad) {
				LaunchPad pad = (LaunchPad)getComponent(i);
				if (pad.getScene() == s) {
					pad.update();
					pad.repaint();
				}
			}
		}
	}
	
	
	public void update() {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof LaunchPad)
				((LaunchPad)getComponent(i)).update();
		}
		repaint();
	}
	
}
