package net.judah.gui.drums.views;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import lombok.Getter;
import net.judah.drums.Drama;
import net.judah.drums.DrumUpdate;
import net.judah.drums.DrumUpdate.Mode;
import net.judah.drums.noise.NoiseOsc;
import net.judah.gui.drums.OscTuning;
import net.judah.gui.widgets.Slider;

/** Full params of a NoiseOsc (Base + custom1,2,3) */
public class NoiseView extends DrumView<NoiseOsc> {

	@Getter private final NoiseOsc drum;
	private final Drama drama;

	// custom controls
	private final JSlider param1Slider = new Slider(e->change(e));
	private final JLabel param1Val = new JLabel("0", JLabel.CENTER);
	private final JSlider param2Slider = new Slider(e->change(e));
	private final JLabel param2Val = new JLabel("0", JLabel.CENTER);
	private final JSlider param3Slider = new Slider(e->change(e));
	private final JLabel param3Val = new JLabel("0", JLabel.CENTER);
	@Getter private final OscTuning tuning;

	public NoiseView(NoiseOsc show) {
		super(show);
		this.drum = show;
		this.tuning = new OscTuning(drum);
		drama = drum.getDrama();

		child.setBorder(BorderFactory.createTitledBorder("Noise"));

		child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));

		String [] names = drama.getExtra();

		child.add(singleRow(names[0], param1Slider, param1Val));
		child.add(singleRow(names[1], param2Slider, param2Val));
		child.add(singleRow(names[2], param3Slider, param3Val));

		// wire listeners
		param1Slider.addChangeListener(this);
		param2Slider.addChangeListener(this);
		param3Slider.addChangeListener(this);

		update();
	}

	private void change(ChangeEvent e) {
		Object src = e.getSource();
		if (src == param1Slider)
			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 0, param1Slider.getValue()));
		else if (src == param2Slider)
			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 1, param2Slider.getValue()));
		else if (src == param3Slider)
			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 2, param3Slider.getValue()));
	}

//	@Override
//	public void stateChanged(ChangeEvent e) {
//		if (updating) return;
//
//		Object src = e.getSource();
//		if (src == param1Slider)
//			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 0, param1Slider.getValue()));
//		else if (src == param2Slider)
//			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 1, param2Slider.getValue()));
//		else if (src == param3Slider)
//			drum.getDrama().set(new DrumUpdate(drum, Mode.NOISE, 2, param3Slider.getValue()));
//		else
//			super.stateChanged(e);
//	}

	@Override
	protected void updateChild() {
		int p1 = drum.get(0);
		if (param1Slider.getValue() != p1)
			param1Slider.setValue(p1);

		int p2 = drum.get(1);
		if (param2Slider.getValue() != p2)
			param2Slider.setValue(p2);

		int p3 = drum.get(2);
		if (param3Slider.getValue() != p3)
			param3Slider.setValue(p3);

		param1Val.setText(drum.val(0));
		param2Val.setText(drum.val(1));
		param3Val.setText(drum.val(2));

	}

}
