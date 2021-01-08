package net.judah.mixer;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

public abstract class MixerGui extends JPanel {
	
	public abstract JToggleButton getLabelButton();
	public abstract void update();
	public abstract void setVolume(int vol);

}
