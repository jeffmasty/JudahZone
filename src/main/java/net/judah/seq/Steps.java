package net.judah.seq;

import java.awt.Point;

import javax.swing.JPanel;

import net.judah.api.Signature;

public abstract class Steps extends JPanel {

	public abstract void timeSig(Signature value);

	public abstract void highlight(Point p);

}
