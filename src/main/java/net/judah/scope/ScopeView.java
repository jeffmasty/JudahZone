package net.judah.scope;

import javax.swing.JPanel;

public abstract class ScopeView extends JPanel {
	
	public abstract void process(float[][] stereo);
	
}
