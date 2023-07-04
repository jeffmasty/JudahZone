package net.judah.gui.widgets;

import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import net.judah.gui.Gui;
import net.judah.gui.Updateable;
import net.judah.looper.Loop;
import net.judah.looper.Looper;

public class LoopWidget extends JPanel implements Updateable {
	public static final String FRESH = "0.0s";
	public static final int BSYNC_UP = Integer.MAX_VALUE;
	public static final int BSYNC_DOWN = 1000000;
	
	private final Looper looper;
	private final Slider slider;
    private final JLabel loopLbl = new JLabel(FRESH);

	public LoopWidget(Looper loops, Dimension size) {
		this.looper = loops;
		slider = new Slider(null);
		slider.setOrientation(JSlider.HORIZONTAL);
        slider.setEnabled(false);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(-1);
        slider.setMinorTickSpacing(25);
        Gui.resize(slider, size);
        add(new JLabel("Loop:"));
        add(loopLbl);
        add(slider);
	}
	
	@Override public void update() {
		Loop primary = looper.getPrimary();
		if (primary == null) {
			loopLbl.setText(FRESH);
			if (slider.getValue() != 0)
				slider.setValue(0);
		}
		else {
			if (false == loopLbl.getText().equals(looper.getRecordedLength()))
				loopLbl.setText(looper.getRecordedLength());
			int val = (int) Math.floor(100 * primary.getTapeCounter().intValue() / (float)primary.getLength());
			if (val != slider.getValue()) 
				slider.setValue(val);
		}
	}

}
