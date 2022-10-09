package net.judah.tracker;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import net.judah.util.Pastels;

public class ActiveTracks extends JPanel {

	private final JudahBeatz beats;
	private final JudahNotez notes;
	private final int width;
	private final int height;
	private final Dimension block;
	
	public ActiveTracks(JudahBeatz beatz, JudahNotez notez, Dimension d) {
		beats = beatz;
		notes = notez;
		this.width = d.width;
		this.height = d.height;
		block = new Dimension(width / 4, height / 2);
		setPreferredSize(d);
		setOpaque(true);
	}
	
	@Override
	public void paint(Graphics g) {
		// super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		
		// top row
		for (int i = 0; i < beats.size(); i++) {
			g2d.setPaint(beats.get(i).isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
			g2d.fillRect(i * block.width, 0, block.width, block.height);
		}
		// bottom row
		for (int i = 0; i < notes.size(); i++) {
			g2d.setPaint(notes.get(i).isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
			g2d.fillRect(i * block.width, block.height, block.width, block.height);
		}
		
		g2d.setPaint(Pastels.MY_GRAY);
		for (int y = 0; y < 2; y++)
			for (int x = 0; x < beats.size(); x++)
				g2d.drawRect(x * block.width, y * block.height, block.width, block.height);
	}
	
	
}
