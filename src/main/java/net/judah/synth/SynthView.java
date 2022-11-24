package net.judah.synth;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;
import net.judah.controllers.KnobMode;
import net.judah.controllers.Knobs;
import net.judah.effects.CutFilter;
import net.judah.util.CenteredCombo;
import net.judah.util.Constants;
import net.judah.util.FileChooser;
import net.judah.util.FxButton;
import net.judah.util.Slider;

public class SynthView extends JPanel implements Knobs {

	private static final Dimension SLIDER = new Dimension(60, STD_HEIGHT);
	private static final Dimension COMBO = new Dimension(55, STD_HEIGHT);
	public static final String[] DETUNE_OPTIONS = new String[] {
			"OCT", "5th", "+3", "+2", "+1", "+/-0", "-5th", "SUB"};
	public static final float[] DETUNE_AMOUNT = new float[] {
			2f, 1.5f, 1.015f, 1.01f, 1.005f, 1f, 0.75f ,0.5f};
	public static final int DETUNE_NONE = 5;

	@Getter private final JudahSynth synth;
	@Getter private static ArrayList<SynthView> views = new ArrayList<>() {
		public boolean add(SynthView e) {
			SynthEngines.setCurrent(e);
			return super.add(e);};};
	@Setter @Getter private static boolean freqMode = true; //. vs resonance mode
	@Getter private final PresetsCombo presets;
	private final JButton save = new JButton("Save");
	private final Slider amp = new Slider(null);
	private final AdsrView adsr;
	private final Slider lpFreq = new Slider(null);
	private final Slider lpReso = new Slider(0, 20, null);
	private final Slider hpFreq = new Slider(0, 50, null);
	private final Slider hpReso = new Slider(0, 20, null);
	private final ArrayList<Slider> gains = new ArrayList<>();
	private final ArrayList<JComboBox<Shape>> shapes = new ArrayList<>();
	private final ArrayList<JComboBox<String>> detune = new ArrayList<>();
	private final JComboBox<Integer> mod = new JComboBox<>();
	
	public SynthView(JudahSynth zynth) {
		views.add(this);
		this.synth = zynth;
		this.presets = new PresetsCombo(synth.getSynthPresets());
		this.adsr = new AdsrView(synth.getAdsr());
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
			slider.setPreferredSize(SLIDER);
			slider.setMaximumSize(SLIDER);
			gains.add(slider);
		}
		for (Slider slider : new Slider[] { lpFreq, lpReso, hpFreq, hpReso}) {
			slider.setPreferredSize(SLIDER);
			slider.setMaximumSize(SLIDER);
		}

		JPanel filter = new JPanel();
		filter.setLayout(new BoxLayout(filter, BoxLayout.PAGE_AXIS));
		filter.setBorder(BorderFactory.createTitledBorder(SynthPresets.FILTER));
		filter.add(duo(new JLabel("High-Cut"), lpFreq));
		filter.add(duo(new JLabel("Resonance"), lpReso));
		filter.add(duo(new JLabel("Low-Cut"), hpFreq));
		filter.add(duo(new JLabel("Resonance"), hpReso));

		JPanel dco = new JPanel();
		dco.setLayout(new BoxLayout(dco, BoxLayout.PAGE_AXIS));
		for (int i = 0; i < synth.getDcoGain().length; i++) 
			dco.add(Constants.wrap(gains.get(i), shapes.get(i), detune.get(i)));
		dco.setBorder(BorderFactory.createTitledBorder("Oscillators"));

		setLayout(new GridLayout(2, 2));
		add(topCorner());
		add(adsr);
		add(filter);
		add(dco);
		
		update();
		listeners();
	}
	
	@Override
	public KnobMode getKnobMode() {
		return synth.getKnobMode();
	}

	private JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(1, 2));
		result.add(left);
		result.add(right);
		return result;
	}
	
	private JPanel topCorner() {
		JPanel btns = new JPanel();
		btns.add(amp);
		btns.add(new FxButton(synth));
		btns.add(mod);
		JPanel presetPnl = new JPanel();
		
		presetPnl.add(presets);
		save.addActionListener(e -> save());
		presetPnl.add(save);

		JPanel corner = new JPanel();
		corner.setLayout(new BoxLayout(corner, BoxLayout.PAGE_AXIS));
		corner.add(btns);
		corner.add(presetPnl);
		corner.setBorder(BorderFactory.createTitledBorder(synth.getName()));
		return corner;
	}
	
	private void listeners() {
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
		
		lpFreq.addChangeListener(e -> synth.getHiCut().setFrequency(
				CutFilter.knobToFrequency(lpFreq.getValue())));
		lpReso.addChangeListener(e ->  synth.getHiCut().setResonance(lpReso.getValue()));
		hpFreq.addChangeListener(e -> synth.getLoCut().setFrequency(
				CutFilter.knobToFrequency(hpFreq.getValue())));
		hpReso.addChangeListener(e ->  synth.getLoCut().setResonance(hpReso.getValue()));
	}
	
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
		adsr.update();
		conformDetune();
		
		repaint();
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
		SynthPresets disk = synth.getSynthPresets();
		if (disk.getLoaded() != null)
			FileChooser.setCurrentFile(disk.getLoaded());
		else 
			FileChooser.setCurrentDir(Constants.SYNTH);
		File f = FileChooser.choose();
		if (f != null) {
			disk.save(f);
		}
	}
	
	public void synthKnobs(int idx, int data2) {
		
		// preset, volume, hi-cut hz, lo-cut res, adsr
		switch (idx) {
			case 0: new Thread(() -> presets.setSelectedIndex(
						Constants.ratio(data2, presets.getItemCount() - 1))).start();
				break;
			case 1:
				amp.setValue(data2);
				break;
			case 2: 
				if (freqMode)
					lpFreq.setValue(data2);
				else 
					lpReso.setValue(data2);
				break;
			case 3: 
				if (freqMode)
					hpFreq.setValue(data2);
				else
					hpReso.setValue(data2);

				break;
			case 4:
				adsr.getA().setValue(data2 * 2);
				break;
			case 5:
				adsr.getD().setValue(data2);
				break;
			case 6:
				adsr.getS().setValue(data2);
				break;
			case 7:
				adsr.getR().setValue(data2 * 5);
				break;
		}
	}

	public static void update(JudahSynth target) {
		for (SynthView v : views)
			if (v.synth == target) {
				v.getPresets().initPresets();
				v.update();
			}
		
	}
	
}

		//	active = new JButton("On");
		//	active.setOpaque(true);
		//	if (primary) 
		//		active.setEnabled(false);
		//	if (!primary) {
		//		 active.setFont(Constants.Gui.BOLD);
		//		 active.addActionListener(e->{
		//			 synth.setActive(!synth.isActive());
		//			 active.setText(synth.isActive() ? "On" : "Off");
		//			 active.setBackground(synth.isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
		//		 });
		//	}
		//	btns.add(active);
		// update():
		// active.setText(synth.isActive() ? "On" : "Off");
		// active.setBackground(synth.isActive() ? Pastels.GREEN : Pastels.PURPLE);
