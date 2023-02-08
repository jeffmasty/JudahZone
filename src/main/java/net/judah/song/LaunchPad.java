package net.judah.song;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;

public class LaunchPad extends JPanel implements Pastels {

	@Getter private final Scene scene;
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
				tab.setOnDeck(scene);
			}});

		params.setFont(Gui.FONT10);
		Font font = Gui.FONT10;
		Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
		attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		notes.setFont(font.deriveFont(attributes));
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(lbl);
		add(notes);
		add(params);
		update();
	}
	
	public void update() {
		lbl.setText(scene.getType().name());
		StringBuffer sb = new StringBuffer("<html>");
		for (Param p : scene.getCommands())
			sb.append(p.getCmd()).append("-").append(p.getVal()).append("<br/>");
		params.setText(sb.append("</html>").toString());
		notes.setText(scene.getNotes());
		if (tab.getCurrent() == scene)
			setBackground(GREEN);
		else if (tab.getOnDeck() == scene) {
			setBackground(scene.getType().getColor());
			if (scene.getType() == Trigger.REL) {
				int countdown = (int)scene.getCommands().getTimeCode() - tab.getCount();
				lbl.setText(countdown + "!");
			}
		}
		
		else setBackground(Pastels.BUTTONS);
	}
	
}
