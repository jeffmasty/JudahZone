package net.judah.song;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.song.cmd.Param;

public class ScenePad extends JPanel implements Pastels {

	@Getter private final Scene scene;
	private final int idx;
	private final Overview tab;
	private final JLabel name = new JLabel();
	private final JLabel type = new JLabel();
	private final JLabel params = new JLabel();

	ScenePad(Scene scene, Overview tab, int idx) {
		this.scene = scene;
		this.tab = tab;
		this.idx = idx;
		setOpaque(true);
		setBorder(UIManager.getBorder("TitledBorder.border"));
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				tab.setOnDeck(scene);
			}});
		params.setFont(Gui.FONT10);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(name);
		add(type);
		add(params);
		update();
	}

	public void update() {
		if (idx == 0 && (scene.getNotes() == null || scene.getNotes().isBlank()))
			name.setText("Home");
		else
			name.setText(idx + ":" + (scene.getNotes() == null ? "" : scene.getNotes()));
		type.setText(scene.getType().name());
		StringBuffer sb = new StringBuffer("<html>");
		for (Param p : scene.getCommands())
			sb.append(p.getCmd()).append(": ").append(p.getVal()).append("<br/>");
		params.setText(sb.append("</html>").toString());
		if (tab.getScene() == scene)
			setBackground(GREEN);
		else if (tab.getOnDeck() == scene) {
			setBackground(scene.getType().getColor());
			if (scene.getType() == Trigger.REL)

				type.setText(tab.getCount() + "/" + scene.getCommands().getTimeCode() + "!");
		}
		else setBackground(Pastels.BUTTONS);
	}

}
