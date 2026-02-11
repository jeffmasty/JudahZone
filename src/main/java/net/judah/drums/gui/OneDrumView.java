package net.judah.drums.gui;

import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import judahzone.api.Key;
import judahzone.data.Frequency;
import judahzone.fx.EQ.EqBand;
import judahzone.fx.Gain;
import judahzone.fx.MonoFilter.Settings;
import judahzone.gui.Gui;
import judahzone.widgets.DoubleSlider;
import judahzone.widgets.Knob;
import lombok.Getter;
import net.judah.drums.gui.OneFrame.LBL;
import net.judah.drums.synth.DrumOsc;
import net.judah.drums.synth.DrumParams;

/** Full params of a DrumOsc in a JPanel. */
public class OneDrumView extends JPanel implements ChangeListener, OneFrame.SZ {

	@Getter private final DrumOsc drum;
	private boolean updating = false;

	private final JSlider volSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel volVal = new JLabel("0%", JLabel.CENTER);
	private final JSlider atkSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 0);
	private final JLabel atkVal = new JLabel("0", JLabel.CENTER);

	private final JSlider panSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel panVal = new JLabel("0%", JLabel.CENTER);
	private final JSlider dkSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel dkVal = new JLabel("0", JLabel.CENTER);

	private final Knob lowResKnob = new Knob();
	private final Knob highResKnob = new Knob();
	private final JLabel loResVal = new JLabel("", JLabel.CENTER);
	private final JLabel hiResVal = new JLabel("", JLabel.CENTER);

	private final DoubleSlider filterRange;
	private final JLabel loCutVal = new JLabel("", JLabel.CENTER);
	private final JLabel hiCutVal = new JLabel("", JLabel.CENTER);

	private final JSlider pitchSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel pitchVal = new JLabel("", JLabel.CENTER);

	private final JSlider param1Slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel param1Val = new JLabel("0", JLabel.CENTER);
	private final JSlider param2Slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel param2Val = new JLabel("0", JLabel.CENTER);
	private final JSlider param3Slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel param3Val = new JLabel("0", JLabel.CENTER);

	public OneDrumView(DrumOsc show) {
		drum = show;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setFocusable(true);
		setBorder(Gui.SUBTLE);

		int freqIdx = Settings.Frequency.ordinal();
		filterRange = new DoubleSlider(drum.getLowCut(), freqIdx, drum.getHiCut(), freqIdx);
		filterRange.addChangeListener(this);

		add(singleRow("Vol", volSlider, volVal));
		add(singleRow("Pan", panSlider, panVal));
		add(singleRow("Attack", atkSlider, atkVal));
		add(singleRow("Decay", dkSlider, dkVal));

		String[] names = drum.getSettings().names();
		add(singleRow(names[0], param1Slider, param1Val));
		add(singleRow(names[1], param2Slider, param2Val));
		add(singleRow(names[2], param3Slider, param3Val));

		add(singleRow("Pitch", pitchSlider, pitchVal));
		add(filterRow());

		volSlider.addChangeListener(this);
		atkSlider.addChangeListener(this);
		panSlider.addChangeListener(this);
		dkSlider.addChangeListener(this);
		pitchSlider.addChangeListener(this);
		param1Slider.addChangeListener(this);
		param2Slider.addChangeListener(this);
		param3Slider.addChangeListener(this);

		lowResKnob.addListener(value-> {
			if (lowResKnob.getValue() != value)
				lowResKnob.setValue(value);
			else
				DrumParams.set(new DrumParams(drum, 1, 2, value));
			updateFilterLabels();
		});

		highResKnob.addListener(value -> {
			if (highResKnob.getValue() != value)
				highResKnob.setValue(value);
			else
				DrumParams.set(new DrumParams(drum, 1, 3, value));
			updateFilterLabels();
		});

		updateAllControls();
	}

	private JPanel singleRow(String label, JSlider slider, JLabel value) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createHorizontalGlue());
		LBL lbl = new LBL(label);
		p.add(lbl);
		p.add(Box.createHorizontalStrut(SPACING));
		slider.setPreferredSize(SLIDER_DIM);
		slider.setMaximumSize(SLIDER_DIM);
		p.add(slider);
		p.add(Box.createHorizontalStrut(SPACING));
		value.setPreferredSize(VAL_DIM);
		value.setMaximumSize(VAL_DIM);
		p.add(value);
		p.add(Box.createHorizontalGlue());
		return p;
	}

	private JPanel filterRow() {
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));


		JPanel top = new JPanel();
		JPanel bottom = new JPanel();
		result.add(Gui.wrap(new JLabel("Filter", JLabel.CENTER)));
		result.add(top);
		result.add(bottom);

		loResVal.setPreferredSize(VAL_DIM);
		loResVal.setMaximumSize(VAL_DIM);

		loCutVal.setPreferredSize(VAL_DIM);
		loCutVal.setMaximumSize(VAL_DIM);

		hiCutVal.setPreferredSize(VAL_DIM);
		hiCutVal.setMaximumSize(VAL_DIM);

		hiResVal.setPreferredSize(VAL_DIM);
		hiResVal.setMaximumSize(VAL_DIM);

		top.add(loCutVal);
		result.add(Box.createHorizontalStrut(SPACING));
		top.add(filterRange);
		result.add(Box.createHorizontalStrut(SPACING));
		top.add(hiCutVal);

		bottom.add(loResVal);
		result.add(Box.createHorizontalStrut(SPACING));
		bottom.add(lowResKnob);
		JPanel lbls = new JPanel(new GridLayout(2, 2, 10, 2));
		lbls.add(new JLabel("LoCut", JLabel.CENTER));
		lbls.add(new JLabel("HiCut", JLabel.CENTER));
		lbls.add(new JLabel("dB", JLabel.CENTER));
		lbls.add(new JLabel("dB", JLabel.CENTER));
		bottom.add(lbls);
		bottom.add(highResKnob);
		result.add(Box.createHorizontalStrut(SPACING));
		bottom.add(hiResVal);

		return result;

	}

	private void updateAllControls() {
		updating = true;

		volSlider.setValue(drum.getGain().get(Gain.VOLUME));
		atkSlider.setValue(drum.getAttack());
		panSlider.setValue(drum.getGain().get(Gain.PAN));
		dkSlider.setValue(drum.getDecay());

		lowResKnob.setValue(DrumParams.get(drum, 1, 2));
		highResKnob.setValue(DrumParams.get(drum, 1, 3));
		filterRange.update();

		pitchSlider.setValue(DrumParams.get(drum, 2, 3));

		param1Slider.setValue(DrumParams.get(drum, 2, 0));
		param2Slider.setValue(DrumParams.get(drum, 2, 1));
		param3Slider.setValue(DrumParams.get(drum, 2, 2));

		updateAllLabels();
		updating = false;
	}

	private void updateAllLabels() {
		volVal.setText(volSlider.getValue() + "%");
		atkVal.setText(atkSlider.getValue() + "");
		panVal.setText(panSlider.getValue() + "%");
		dkVal.setText(dkSlider.getValue() + "");

		updateFilterLabels();
		updatePitchLabel();
		updateCustomLabels();
	}

	private void updatePitchLabel() {
		float hz = drum.getHz();
		int midi1 = Frequency.hzToMidi(hz);
		Key key1 = Key.key(midi1);
		pitchVal.setText(key1.toString() + (midi1 / 12));
	}

	private void updateFilterLabels() {
		loCutVal.setText((int) drum.getHz(EqBand.Bass) + "");
		hiCutVal.setText((int) drum.getHz(EqBand.High) + "");
		loResVal.setText(drum.getResonance(EqBand.Bass) + "");
		hiResVal.setText(drum.getResonance(EqBand.High) + "");
	}

	private void updateCustomLabels() { // TODO enums and bends
		param1Val.setText(param1Slider.getValue() + "");
		param2Val.setText(param2Slider.getValue() + "");
		param3Val.setText(param3Slider.getValue() + "");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (updating) return;

		Object src = e.getSource();
		if (src == volSlider)
			drum.getGain().set(Gain.VOLUME, volSlider.getValue());
		else if (src == atkSlider)
			drum.setAttack(atkSlider.getValue());
		else if (src == panSlider)
			drum.getGain().set(Gain.PAN, panSlider.getValue());
		else if (src == dkSlider)
			drum.setDecay(dkSlider.getValue());
		else if (src == pitchSlider)
			DrumParams.set(new DrumParams(drum, 2, 3, pitchSlider.getValue()));
		else if (src == param1Slider)
			DrumParams.set(new DrumParams(drum, 2, 0, param1Slider.getValue()));
		else if (src == param2Slider)
			DrumParams.set(new DrumParams(drum, 2, 1, param2Slider.getValue()));
		else if (src == param3Slider)
			DrumParams.set(new DrumParams(drum, 2, 2, param3Slider.getValue()));

		updateAllLabels();
	}

}
