package net.judah.gui.knobs;

import static net.judah.gui.Size.STD_HEIGHT;
import static net.judah.util.Constants.duo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.controllers.KnobMode;
import net.judah.effects.CutFilter;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.synth.Adsr;
import net.judah.synth.JudahSynth;
import net.judah.synth.PresetsCombo;
import net.judah.synth.Shape;
import net.judah.util.Constants;
import net.judah.widgets.CenteredCombo;
import net.judah.widgets.Knob;
import net.judah.widgets.Slider;

public class SynthKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Synth;
	private static final Dimension COMBO = new Dimension(55, STD_HEIGHT);
	public static final String[] DETUNE_OPTIONS = new String[] {
			"OCT", "5th", "+3", "+2", "+1", "+/-0", "-5th", "SUB"};
	public static final float[] DETUNE_AMOUNT = new float[] {
			2f, 1.5f, 1.015f, 1.01f, 1.005f, 1f, 0.75f ,0.5f};
	public static final int DETUNE_NONE = 5;

	@Getter private final JudahSynth synth;
	@Setter @Getter private static boolean freqMode = true; //. vs resonance mode
	@Getter private final PresetsCombo presets;
	private final JButton save = new JButton("Save");
	private final Slider amp = new Slider(0, 1, null, "Velocity Amplification");
	private static final Color C = Pastels.PURPLE;
	private final Knob lpFreq = new Knob(C);
	private final Knob lpReso = new Knob(C);//0, 20 
	private final Knob hpFreq = new Knob(C);//0, 50 
	private final Knob hpReso = new Knob(C);//0, 20
	private final Adsr adsr;
	@Getter private final Knob a = new Knob(C);//0, 200
	@Getter private final Knob d = new Knob(C);
	@Getter private final Knob s = new Knob(C);
	@Getter private final Knob r = new Knob(C);//0, 500
	private final ArrayList<Slider> gains = new ArrayList<>();
	private final ArrayList<JComboBox<Shape>> shapes = new ArrayList<>();
	private final ArrayList<JComboBox<String>> detune = new ArrayList<>();
	private final JComboBox<Integer> mod = new JComboBox<>(); 
	
	private final JButton synthOne = new JButton(JudahZone.getSynth1().getName());
	private final JButton synthTwo = new JButton(JudahZone.getSynth2().getName());
	private final JPanel titleBar = new JPanel();
	
	public SynthKnobs(JudahSynth zynth) {
		
		super(zynth.getName());
		this.synth = zynth;
		this.presets = new PresetsCombo(synth.getSynthPresets());
		this.adsr = synth.getAdsr(); 
		
		for (int i = 0; i < synth.getShapes().length; i++) {
			JComboBox<Shape> combo = new CenteredCombo<>();
			for(Shape val : Shape.values())
				combo.addItem(val);
			combo.setMaximumSize(COMBO);
			shapes.add(combo);
		}

		for (int i = 0; i < synth.getDetune().length; i++) {
			JComboBox<String> tune = new CenteredCombo<>();
			for (String s : DETUNE_OPTIONS)
				tune.addItem(s);
			tune.setSelectedIndex(DETUNE_NONE);
			tune.setToolTipText("Detuning");
			detune.add(tune);
		}
		mod.setToolTipText("Pitchbend Semitones");
		for (int i = 1; i <= 12; i++)
			mod.addItem(i);
		mod.addActionListener(e->synth.setModSemitones((Integer)mod.getSelectedItem()));
		
		for (int i = 0; i < synth.getDcoGain().length; i++) {
			Slider slider = new Slider(null);
			gains.add(slider);
		}
		JPanel row = new JPanel();
		row.add(presets);
		row.add(new JLabel("Vol"));
		row.add(amp);
 		row.add(mod);
		add(row);
		
		JPanel knobs = new JPanel(new GridLayout(2, 4, 4, 4));
		knobs.add(duo(a, new JLabel(" A")));
		knobs.add(duo(d, new JLabel(" D")));
		knobs.add(duo(s, new JLabel(" S")));
		knobs.add(duo(r, new JLabel(" R")));
		knobs.add(duo(lpFreq, new JLabel("HiCut")));
		knobs.add(duo(lpReso, new JLabel("Res")));
		knobs.add(duo(hpFreq, new JLabel("LoCut")));
		knobs.add(duo(hpReso, new JLabel("Res")));
		add(Constants.wrap(knobs));

		JPanel dco = new JPanel();
		dco.setLayout(new BoxLayout(dco, BoxLayout.PAGE_AXIS));
		for (int i = 0; i < synth.getDcoGain().length; i++) 
			dco.add(Constants.wrap(new JLabel("DCO " + i), gains.get(i), shapes.get(i), detune.get(i)));
		add(dco);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		update();
		listeners();
		
		synthOne.setBackground(synth == JudahZone.getSynth1() ? Pastels.GREEN : null);
		synthTwo.setBackground(synth == JudahZone.getSynth2() ? Pastels.GREEN : null);
		synthOne.addActionListener(e->MainFrame.setFocus(JudahZone.getSynth1().getSynthKnobs()));
		synthTwo.addActionListener(e->MainFrame.setFocus(JudahZone.getSynth2().getSynthKnobs()));
		titleBar.add(synthOne);
		titleBar.add(synthTwo);
		titleBar.add(save);
	}

	@Override
	public JPanel installing() {
		MainFrame.setFocus(synth);
		return titleBar;
	}
	
	private void listeners() {
		save.addActionListener(e->save());
		amp.addChangeListener(e->synth.setAmplification(amp.getValue() * 0.01f));
		for (int i = 0; i < gains.size(); i++) {
			final int idx = i;
			gains.get(i).addChangeListener(e->synth.setGain(idx, gains.get(idx).getValue() * 0.01f));
		}
		for (int i = 0; i < shapes.size(); i++) {
			final int idx = i;
			shapes.get(i).addActionListener(e->synth.setShape(idx, (Shape)shapes.get(idx).getSelectedItem()));
		}
		for (int i = 0; i < detune.size(); i++) {
			final int idx = i;
			detune.get(i).addActionListener(e->synth.getDetune()[idx] = DETUNE_AMOUNT[detune.get(idx).getSelectedIndex()]);
		}
		
		a.addListener(e->adsr.setAttackTime(a.getValue()));
		d.addListener(e->adsr.setDecayTime(d.getValue()));
		s.addListener(e-> adsr.setSustainGain(s.getValue() * 0.01f));
		r.addListener(e->adsr.setReleaseTime(r.getValue()));

		lpFreq.addListener(data2 -> synth.getHiCut().setFrequency(
				CutFilter.knobToFrequency(data2)));
		lpReso.addListener(data2 ->  synth.getHiCut().setResonance(data2));
		hpFreq.addListener(data2 -> synth.getLoCut().setFrequency(
				CutFilter.knobToFrequency(data2)));
		hpReso.addListener(data2 ->  synth.getLoCut().setResonance(data2));
		
	}
	
	@Override
	public void update() {
		if (amp.getValue() != synth.getAmplification() * 100)
			amp.setValue((int)(synth.getAmplification() * 100));
		if (lpReso.getValue() != synth.getHiCut().getResonance())
			lpReso.setValue((int)(synth.getHiCut().getResonance()));
		if (hpReso.getValue() != synth.getLoCut().getResonance())
			hpReso.setValue((int)(synth.getLoCut().getResonance()));
		if (CutFilter.knobToFrequency(lpFreq.getValue()) != synth.getHiCut().getFrequency())
			lpFreq.setValue((int)Math.ceil(CutFilter.frequencyToKnob(synth.getHiCut().getFrequency())));
		if (CutFilter.knobToFrequency(hpFreq.getValue()) != synth.getLoCut().getFrequency())
			hpFreq.setValue((int)Math.ceil(CutFilter.frequencyToKnob(synth.getLoCut().getFrequency())));
		for (int i = 0; i < gains.size(); i++)
			checkGain(gains.get(i), i);
		for (int i = 0; i < shapes.size(); i++)
			checkShape(shapes.get(i), i);
		
		// adsr update
		if (a.getValue() != adsr.getAttackTime())
			a.setValue(adsr.getAttackTime());
		if (d.getValue() != adsr.getDecayTime())
			d.setValue(adsr.getDecayTime());
		if (s.getValue() * 0.01f != adsr.getSustainGain())
			s.setValue((int)(adsr.getSustainGain() * 100));
		if (r.getValue() != adsr.getReleaseTime())
			r.setValue(adsr.getReleaseTime());

		conformDetune();
		presets.select();
		
		lpFreq.setEnabled(freqMode);
		lpReso.setEnabled(freqMode);
		hpFreq.setEnabled(!freqMode);
		hpReso.setEnabled(!freqMode);

		
	}

	private void conformDetune() {
		for (int i = 0; i < detune.size(); i++) {
			String target = DETUNE_OPTIONS[DETUNE_NONE];
			for (int x = 0; x < DETUNE_AMOUNT.length; x++)
				if (synth.getDetune()[i] == DETUNE_AMOUNT[x]) {
					target = DETUNE_OPTIONS[x];
					break;
				}
			detune.get(i).setSelectedItem(target);
		}
	}
	
	private void checkShape(JComboBox<Shape> shape, int i) {
		if (shape.getSelectedItem() != synth.getShape(i))
			shape.setSelectedItem(synth.getShape(i));
	}

	private void checkGain(Slider gain, int i) {
		if (gain.getValue() * 0.01f != synth.getDcoGain()[i])
			gain.setValue((int)(synth.getDcoGain()[i] * 100));
	}

	private void save() {
		String name = JOptionPane.showInputDialog("Synth Preset Name", synth.getSynthPresets().getCurrent());
		if (name == null || name.length() == 0)
			return;
		JudahZone.getSynthPresets().save(synth, name);
	}
	
	@Override
	public boolean doKnob(int idx, int data2) {
		
		// preset, volume, hi-cut hz, lo-cut res, adsr
		switch (idx) {
			case 0:
				adsr.setAttackTime(data2);
				break;
			case 1:
				adsr.setDecayTime(data2);
				break;
			case 2:
				adsr.setSustainGain(data2 * 0.01f);
				break;
			case 3:
				adsr.setReleaseTime(data2);
				break;
			case 4: new Thread(() -> presets.setSelectedIndex(
						Constants.ratio(data2, presets.getItemCount() - 1))).start();
				break;
			case 5:
				amp.setValue(data2);
				break;
			case 6: 
				if (freqMode)
					synth.getHiCut().setFrequency(CutFilter.knobToFrequency(data2));
				else 
					synth.getHiCut().setResonance(data2);
				break;
			case 7: 
				if (freqMode)
					synth.getLoCut().setFrequency(CutFilter.knobToFrequency(data2));				
				else
					synth.getLoCut().setResonance(data2);
				break;
				
			default:
				return false;
		}
		return true;
	}
	
	@Override
	public void pad1() {
		JudahSynth target = (synth == JudahZone.getSynth1()) ? JudahZone.getSynth2() : JudahZone.getSynth1();
			MainFrame.setFocus(target.getSynthKnobs());
	}

	@Override
	public void pad2() {
		freqMode = !freqMode;
		MainFrame.update(this);
	}
	
}
