package net.judah.seq;

import javax.swing.JPanel;

public abstract class Steps extends JPanel {
	
	public abstract void setStart(int num);
	
	public abstract int getUnit();
	public abstract int getTotal();

	
}
