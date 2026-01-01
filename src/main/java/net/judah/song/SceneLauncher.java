package net.judah.song;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import net.judah.JudahZone;

/** launch scenes */
public class SceneLauncher extends JPanel implements Updateable {
	private static final int ROWS = 3;
	private static final int COLS = 4;
	public static final int MAX = ROWS * COLS; // 12 Scenes

	private final Song song;
	private Overview tab;

	public SceneLauncher(Song song, Overview tab) {
		this.song = song;
		this.tab = tab;
		setLayout(new GridLayout(ROWS, COLS, 1, 1));
		fill();
		setBackground(Color.WHITE);
	}

	public void fill() {
		removeAll();
		int size = song.getScenes().size();
		if (size > MAX)
			size = MAX;
		for (int i = 0; i < size; i++)
			add(new ScenePad(song.getScenes().get(i), tab, i));

		if (size >= MAX) return;
		if (JudahZone.getInstance().getMains().isRecording() == false) {
			JLabel create = new JLabel("(Create)", JLabel.CENTER);
			create.setBorder(UIManager.getBorder("TitledBorder.border"));
			create.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) { tab.getSongView().newScene();}});
			create.setBackground(Pastels.BUTTONS);
			create.setOpaque(true);
			add(create);
			size++;
		}
		while (++size < MAX)
			add(new JLabel()); // filler
		update();
		doLayout();
	}

	public void update(Scene s) {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof ScenePad) {
				ScenePad pad = (ScenePad)getComponent(i);
				if (pad.getScene() == s)
					pad.update();
			}
		}
	}

	@Override
	public void update() {
		for (int i = 0; i < getComponentCount(); i++) {
			if (getComponent(i) instanceof ScenePad)
				((ScenePad)getComponent(i)).update();
		}
	}

}
