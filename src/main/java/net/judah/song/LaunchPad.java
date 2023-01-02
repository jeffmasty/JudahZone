package net.judah.song;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.judah.gui.Gui;
import net.judah.gui.Pastels;

public class LaunchPad extends JPanel implements Pastels {

	private final Scene scene;
	private final SongTab tab;
	private final JLabel lbl = new JLabel();
	private final JLabel notes = new JLabel();
	private final JLabel params = new JLabel();
	
	LaunchPad(Scene scene, SongTab tab) {
		this.scene = scene;
		this.tab = tab;
		setOpaque(true);
		setBorder(UIManager.getBorder("TitledBorder.border"));
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				tab.getSongView().setOnDeck(scene);
			}});

		params.setFont(Gui.FONT10);
		notes.setFont(Gui.FONT10);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(lbl);
		add(notes);
		add(params);
		update();
	}
	
	public void update() {
		lbl.setText(scene.toString() + " " + scene.getType());
		
		StringBuffer sb = new StringBuffer("<html>");
		for (Param p : scene.getParams())
			sb.append(p.getCmd()).append("-").append(p.getVal()).append("<br/>");
		params.setText(sb.append("</html>").toString());
		notes.setText(scene.getNotes());
		if (tab.getCurrent() == scene)
			setBackground(GREEN);
		else if (tab.getSongView() != null && tab.getSongView().getOnDeck() == scene) 
			setBackground(scene.getType().getColor());
		else setBackground(null);
	}
	
}
