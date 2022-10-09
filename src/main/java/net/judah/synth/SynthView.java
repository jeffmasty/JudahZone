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
import net.judah.effects.CutFilter;
import net.judah.util.Constants;
import net.judah.util.FileChooser;
import net.judah.util.FxButton;
import net.judah.util.Slider;

public class SynthView extends JPanel {
	private static final Dimension SLIDER = new Dimension(75, STD_HEIGHT);
	private static final Dimension COMBO = new Dimension(60, STD_HEIGHT);

	@Getter private static ArrayList<SynthView> views = new ArrayList<>() {
		public boolean add(SynthView e) {
			SynthEngines.setCurrent(e);
			return super.add(e);};};
	@Setter @Getter private static boolean freqMode = true; //. vs resonance mode
	
	@Getter private final JudahSynth synth;
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

	public SynthView(JudahSynth zynth) {
		views.add(this);
		this.synth = zynth;
		this.presets = new PresetsCombo(synth.getPresets());
		this.adsr = new AdsrView(synth.getAdsr());
		for (int i = 0; i < synth.getShapes().length; i++) {
			JComboBox<Shape> combo = new JComboBox<>();
			for(Shape val : Shape.values())
				combo.addItem(val);
			combo.setMaximumSize(COMBO);
			shapes.add(combo);
		}

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
		filter.setBorder(BorderFactory.createTitledBorder(JudahSynth.FILTER));
		filter.add(duo(new JLabel("High-Cut"), lpFreq));
		filter.add(duo(new JLabel("Resonance"), lpReso));
		filter.add(duo(new JLabel("Low-Cut"), hpFreq));
		filter.add(duo(new JLabel("Resonance"), hpReso));

		JPanel dco = new JPanel();
		dco.setLayout(new BoxLayout(dco, BoxLayout.PAGE_AXIS));
		for (int i = 0; i < synth.getDcoGain().length; i++) 
			dco.add(Constants.wrap(new JLabel("Dco-" + i + " Mix"), gains.get(i), shapes.get(i)));
		dco.setBorder(BorderFactory.createTitledBorder("Oscillators"));

		setLayout(new GridLayout(2, 2));
		add(topCorner());
		add(adsr);
		add(filter);
		add(dco);
		
		update();
		listeners();
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
		repaint();
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
		SynthPresets disk = synth.getPresets();
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
				adsr.getA().setValue(data2);
				break;
			case 5:
				adsr.getD().setValue(data2);
				break;
			case 6:
				adsr.getS().setValue(data2);
				break;
			case 7:
				adsr.getR().setValue(data2);
				break;
		}
	}

	public static void update(JudahSynth target) {
		for (SynthView v : views)
			if (v.synth == target)
				v.update();
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
