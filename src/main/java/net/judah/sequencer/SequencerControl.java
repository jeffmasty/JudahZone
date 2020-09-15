package net.judah.sequencer;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import net.judah.song.Edits;

public class SequencerControl extends JPanel implements Edits {

	JButton start, previous, next, end, play, pause, settings;
	JTextField current, tempo;
	JSlider loop;
	// ~/git/Swing-range-slider ---> https://ernienotes.wordpress.com/2010/12/27/creating-a-java-swing-range-slider/
	// https://www.infoworld.com/article/2071315/jslider-appearance-improvements.html
	
	public SequencerControl() {

	}
	
	@Override
	public void add() {
	}
	@Override
	public void delete() {
	}
	@Override
	public void copy() {
	}
	
}
