package net.judah.drumkit;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.gui.Pastels;
import net.judah.widgets.CenteredCombo;

public abstract class Pad extends JPanel {
	protected static final Color borderColor = Pastels.PURPLE;

	protected final JComboBox<String> assign = new CenteredCombo<>(); 
	protected final JPanel top = new JPanel();
	protected final JPanel bottom = new JPanel();
	protected final DrumType type;
	
	public Pad(DrumType type) {
		this.type = type;
		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(0, 1, 0, 0));
		JLabel nombre = new JLabel(type.name());
		top.add(nombre);
		top.setOpaque(true);
		bottom.setOpaque(true);
		add(top);
		add(bottom);
		setOpaque(true);
	}
	
	public abstract void update();
}
