package net.judah.drumz;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.judah.util.CenteredCombo;
import net.judah.util.Constants;
import net.judah.util.Pastels;

public abstract class Pad extends JPanel {
	protected static final Color borderColor = Pastels.PURPLE;

	protected final JComboBox<String> assign = new CenteredCombo<>(); 
	protected final JPanel top = new JPanel();
	protected final JPanel bottom = new JPanel();
	protected final DrumType type;
	
	public Pad(DrumType type) {
		this.type = type;
		setBorder(BorderFactory.createSoftBevelBorder(BevelBorder.RAISED, borderColor, borderColor.darker()));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JLabel nombre = new JLabel(type.name());
		nombre.setFont(Constants.Gui.BOLD);
		top.add(nombre);
		top.setOpaque(true);
		add(top);
		add(bottom);
		setOpaque(true);
	}
	
	public abstract void update();
}
