package net.judah.gui.knobs.synth;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;

import judahzone.fx.MonoFilter;
import judahzone.fx.op.Kernel.KernelParams;
import judahzone.gui.Gui;
import judahzone.synth.Synth;
import judahzone.widgets.Knob;
import judahzone.widgets.RangeSlider;
import net.judah.gui.drums.views.DrumView.DataLbl;

public class SynthFilter extends JPanel implements KernelParams {

//	LoRes      "Filter"      HiRes
//  knob   doubleSlider      knob
// "res"   loCutVal hiCutVal "res"

	private final RangeSlider hilo;
	private final Knob loResK = new Knob();
	private final Knob hiResK = new Knob();

	private final DataLbl loResVal = new DataLbl();
	private final DataLbl hiResVal = new DataLbl();

	private final JLabel loCutVal = new JLabel("", JLabel.CENTER);
	private final JLabel hiCutVal = new JLabel("", JLabel.CENTER);

	private final Synth synth;

	public SynthFilter(Synth synth) {
		this.synth = synth;
		setLayout(new BorderLayout());
		hilo = new RangeSlider(0, 100,
				synth.get(LOHZ),
				synth.get(HIHZ));

		hilo.addChangeListener(l -> hilo(l));
		loResK.addListener(val -> synth.set(LORES, loResK.getValue()));
		hiResK.addListener(val -> synth.set(HIRES, hiResK.getValue()));
		loResK.setToolTipText("Low cut resonance");
		hiResK.setToolTipText("High cut resonance");

		JPanel left = new JPanel(new GridLayout(3,  1));
		JPanel right = new JPanel(new GridLayout(3, 1));

		left.add(new JLabel("LoRes", JLabel.CENTER));
		left.add(loResVal);
		left.add(loResK);

		JLabel label = new JLabel("Filter", JLabel.CENTER);
		label.setFont(Gui.BOLD12);
		JPanel freqs = new JPanel(new GridLayout(1,  3));
		freqs.add(loCutVal);
		freqs.add(new JLabel("hz", JLabel.CENTER));
		freqs.add(hiCutVal);

		JPanel center = new JPanel(new GridLayout(3, 1));
		center.add(label);
		center.add(hilo);
		center.add(freqs);

		right.add(new JLabel("HiRes", JLabel.CENTER));
		right.add(hiResVal);
		right.add(hiResK);

		add(left, BorderLayout.WEST);
		add(center, BorderLayout.CENTER);
		add(right, BorderLayout.EAST);

	}

	private void hilo(ChangeEvent l) {
		// low
		int loKnob = hilo.getValue();
		int lo = synth.get(LOHZ);
		if (lo != loKnob)
			synth.set(LOHZ, loKnob);

		// high
		int hiKnob = hilo.getUpperValue();
		int hi = synth.get(HIHZ);
		if (hi != hiKnob)
			synth.set(HIHZ, hiKnob);
	}

	/** synchronize UI to synth state */
	public void update() {
		int loCut = synth.getLowCut().get(MonoFilter.Settings.Frequency.ordinal());
		if (hilo.getValue() != loCut)
			hilo.setValue(loCut);
		loCutVal.setText("" + (int)synth.getLowCut().getFrequency());

		int hiCut = synth.getHiCut().get(MonoFilter.Settings.Frequency.ordinal());
		if (hilo.getUpperValue() != hiCut)
			hilo.setUpperValue(hiCut);
		hiCutVal.setText("" + (int)synth.getHiCut().getFrequency());

		int hiRes = synth.getHiCut().get(MonoFilter.Settings.Resonance.ordinal());
		if (hiResK.getValue() != hiRes)
			hiResK.setValue(hiRes);
		hiResVal.setText(synth.val(HIRES));

		int loRes = synth.getLowCut().get(MonoFilter.Settings.Resonance.ordinal());
		if (loResK.getValue() != loRes)
			loResK.setValue(loRes);
		loResVal.setText(synth.val(LORES));
	}

}