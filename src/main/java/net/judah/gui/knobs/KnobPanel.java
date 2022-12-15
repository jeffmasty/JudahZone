package net.judah.gui.knobs;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.util.Constants;


public abstract class KnobPanel extends JPanel {

	public KnobPanel(String title) {
		setName(title);
	}

	public abstract boolean doKnob(int idx, int value);
	
	public abstract void update();
	
	/**Called when the KnobPanel is going to be displayed. 
	 * @return an optional and separate set of title bar component(s) */
	public Component installing() {
		return Constants.wrap(new JLabel(getName()));
	}

	public void pad1() {
		
	}
	
	public void pad2() {
		
	}
	
}
