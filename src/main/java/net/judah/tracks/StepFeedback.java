package net.judah.tracks;

import java.awt.Color;

import javax.swing.JTable;


// TODO offer queue 
public class StepFeedback extends JTable implements Runnable {

	Box beatbox;
	int step;
	
	public StepFeedback(Box beatbox) {
		super(2, 16); //TODO
		this.beatbox = beatbox;
		
		for (int i = 0; i < getColumnCount(); i++)
			getColumnModel().getColumn(i).setPreferredWidth(11);
		
		setShowHorizontalLines(true);
		setShowVerticalLines(true);
		setGridColor(Color.blue);
		setColumnSelectionAllowed(true);
		setRowSelectionAllowed(false);
		setColumnSelectionInterval(1,1);

		new Thread(this).start();

	}
	
	public void refresh() { 
		// Sequence one = beatbox.getCurrent().get(0);
		// Sequence two = beatbox.getCurrent().get(1);
	}
	
	public void step(int step) { 
		this.step = step;
		new Thread(this).start();
	}

	@Override
	public void run() {
		setColumnSelectionInterval(step, step);
	}
}
