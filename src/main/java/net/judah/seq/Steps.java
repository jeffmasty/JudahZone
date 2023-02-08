package net.judah.seq;

import java.awt.Point;

import javax.swing.JPanel;

public abstract class Steps extends JPanel {
	
	public abstract void setStart(int num);
	

	public void highlight(Point p) {
	}
	
	public abstract void timeSig();

	
}
