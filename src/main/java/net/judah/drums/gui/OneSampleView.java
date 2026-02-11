package net.judah.drums.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import judahzone.gui.Gui;
import net.judah.drums.KitDB.BaseParam;
import net.judah.drums.gui.OneFrame.LBL;
import net.judah.drums.oldschool.DrumSample;
import net.judah.drums.oldschool.SampleParams;

/** Simple controls for a DrumSample: Vol, Pan, Attack, Decay. */
public class OneSampleView extends JPanel implements ChangeListener, OneFrame.SZ {

	private final DrumSample s;
	private boolean updating = false;

	private final JSlider volSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel volVal = new JLabel("0%", JLabel.CENTER);

	private final JSlider panSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel panVal = new JLabel("0%", JLabel.CENTER);

	private final JSlider atkSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel atkVal = new JLabel("0", JLabel.CENTER);

	private final JSlider dkSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	private final JLabel dkVal = new JLabel("0", JLabel.CENTER);

	public OneSampleView(DrumSample sample) {
		this.s = sample;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setFocusable(true);
		setBorder(Gui.SUBTLE);

		add(singleRow("Vol", volSlider, volVal));
		add(singleRow("Pan", panSlider, panVal));
		add(singleRow("Attack", atkSlider, atkVal));
		add(singleRow("Decay", dkSlider, dkVal));

		volSlider.addChangeListener(this);
		panSlider.addChangeListener(this);
		atkSlider.addChangeListener(this);
		dkSlider.addChangeListener(this);

		updateAllControls();
	}

	private JPanel singleRow(String label, JSlider slider, JLabel value) {
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createHorizontalGlue());
		p.add(new LBL(label));
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

	private void updateAllControls() {
		updating = true;
		volSlider.setValue(s.getVol());
		panSlider.setValue(s.getPan());
		atkSlider.setValue(s.getAttack());
		dkSlider.setValue(s.getDecay());
		updateAllLabels();
		updating = false;
	}

	private void updateAllLabels() {
		volVal.setText(volSlider.getValue() + "%");
		panVal.setText(panSlider.getValue() + "%");
		atkVal.setText(atkSlider.getValue() + "");
		dkVal.setText(dkSlider.getValue() + "");
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (updating) return;
		Object src = e.getSource();
		if (src == volSlider)
			SampleParams.set(new SampleParams(s, BaseParam.Vol, volSlider.getValue()));
		else if (src == panSlider)
			SampleParams.set(new SampleParams(s, BaseParam.Pan, panSlider.getValue()));
		else if (src == atkSlider)
			SampleParams.set(new SampleParams(s, BaseParam.Attack, atkSlider.getValue()));
		else if (src == dkSlider)
			SampleParams.set(new SampleParams(s, BaseParam.Decay, dkSlider.getValue()));

		updateAllLabels();
	}
}
