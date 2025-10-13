package net.judah.gui.widgets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import lombok.Getter;
import net.judah.looper.Loop;
import net.judah.looper.Looper;

public class LoopWidget extends JPanel {
	public static final String FRESH = "0.0s";

	private final Looper looper;
	@Getter private final Slider slider;
    private final JLabel loopLbl = new JLabel(FRESH);

	public LoopWidget(Looper loops) {
		this.looper = loops;
		slider = new Slider(null);
		slider.setOrientation(JSlider.HORIZONTAL);
        slider.setEnabled(false);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(-1);
        slider.setMinorTickSpacing(25);
        add(new JLabel("Loop:"));
        add(loopLbl);
        add(slider);
	}

	public void update() {
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
