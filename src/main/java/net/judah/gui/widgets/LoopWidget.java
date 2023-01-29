package net.judah.gui.widgets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.looper.Looper;

public class LoopWidget extends JPanel {
	private static final String FRESH = "0.0s";
	public static final int BSYNC_UP = Integer.MAX_VALUE;
	public static final int BSYNC_DOWN = 1000000;
	
	private final Looper looper;
	private final Slider slider;
    private final JLabel loopLbl = new JLabel(FRESH);

	public LoopWidget(Looper sync) {
		this.looper = sync;
		sync.setFeedback(this);
		slider = new Slider(null);
		slider.setOrientation(JSlider.HORIZONTAL);
        slider.setEnabled(false);
        Gui.resize(slider, Size.MEDIUM_COMBO);
        add(new JLabel("Loop:"));
        add(loopLbl);
        add(slider);
	}
	
	public void update() {
		if (looper.getRecordedLength() == 0) {
			loopLbl.setText(FRESH);
			if (slider.getValue() != 0)
				slider.setValue(0);
		}
		else {
			if (loopLbl.getText().equals(FRESH)) {
				String time = Float.toString(looper.getRecordedLength() / 1000f);
				if (time.length() > 4)
					time = time.substring(0, 4);
				loopLbl.setText(time + "s");
			}
			if (looper.getPrimary() == null)
				return;
			if (looper.getPrimary() == null || looper.getPrimary().getLength() == null)
				return;
			int val = (int) Math.floor(100 * looper.getPrimary().getTapeCounter().intValue() / (float)looper.getPrimary().getLength());
			if (val != slider.getValue()) 
				slider.setValue(val);
		}
	}

}
