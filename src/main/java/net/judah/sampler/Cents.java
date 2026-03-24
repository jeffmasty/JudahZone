package net.judah.sampler;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;

import judahzone.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.Slider;

public class Cents extends Box {
	private JLabel label = new JLabel();
	private JSlider cents = new JSlider();
	private static final Dimension PREFERRED = new Dimension(65, Size.HEIGHT_KNOBS);

	public Cents(String labelTxt) {
		super(BoxLayout.Y_AXIS);
		int rate = 0;
	    cents = new Slider(-100, 100, null, "Pitch Cents");
	    cents.setOrientation(JSlider.VERTICAL);
	    cents.setMajorTickSpacing(25);
	    cents.setMinorTickSpacing(20);
	    cents.setPaintTicks(true);
	    cents.setPaintLabels(true);
	    cents.setValue(rate);
	    cents.addChangeListener(e -> {/* todo */});
	    // setRate(rate);
	    Gui.resize(cents, PREFERRED);

	    add(new Gui.Bold(labelTxt));
	    add(Gui.resize(cents, PREFERRED));
	    add(label);


	}

}
