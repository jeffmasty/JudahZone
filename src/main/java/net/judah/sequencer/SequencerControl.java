package net.judah.sequencer;

import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JTextField;

import net.judah.util.Tab;

public class SequencerControl extends Tab {

	JButton start, previous, next, end, play, pause, settings;
	JTextField current, tempo;
	JSlider loop;
	// ~/git/Swing-range-slider ---> https://ernienotes.wordpress.com/2010/12/27/creating-a-java-swing-range-slider/
	// https://www.infoworld.com/article/2071315/jslider-appearance-improvements.html
	
	public SequencerControl() {

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {}

	@Override
	public String getTabName() {return "Sequenca";}

	@Override
	public void setProperties(Properties p) {}
	
}
