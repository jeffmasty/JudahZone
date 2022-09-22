package net.judah.synth;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.*;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.CutFilter;
import net.judah.util.Constants;
import net.judah.util.FileChooser;
import net.judah.util.Pastels;
import net.judah.util.Slider;

public class SynthView extends JPanel {
	private static final Dimension SLIDER = new Dimension(75, STD_HEIGHT);
	private static final Dimension COMBO = new Dimension(60, STD_HEIGHT);
	private final JudahSynth synth;
	
	@Getter private final PresetsCombo presets;
	private final JToggleButton active = new JToggleButton("On");
	private final JButton save = new JButton("Save");
	private final JButton fx = new JButton("Fx");
	private final Slider volume = new Slider(null);
	private final AdsrView adsr;
	private final Slider lpFreq = new Slider(null);
	private final Slider lpReso = new Slider(0, 20, null);
	private final Slider hpFreq = new Slider(0, 50, null);
	private final Slider hpReso = new Slider(0, 20, null);
	
	private final ArrayList<Slider> gains = new ArrayList<>();
	private final ArrayList<JComboBox<Shape>> shapes = new ArrayList<>();

	SynthView(JudahSynth zynth) {
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
		for (Slider slider : new Slider[] { volume, lpFreq, lpReso, hpFreq, hpReso}) {
			slider.setPreferredSize(SLIDER);
			slider.setMaximumSize(SLIDER);
		}

		JPanel top = new JPanel();
		top.add(topCorner());
		top.add(adsr);

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
		
		JPanel bottom = new JPanel();
		bottom.add(filter);
		bottom.add(dco);

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(top);
		add(bottom);
		
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
		btns.add(active);
		fx.addActionListener(e -> MainFrame.setFocus(synth));
		btns.add(fx);
		btns.add(volume);

		JPanel presetPnl = new JPanel();
		
		presetPnl.add(presets);
		save.addActionListener(e -> save());
		presetPnl.add(save);

		JPanel corner = new JPanel();
		corner.setLayout(new BoxLayout(corner, BoxLayout.PAGE_AXIS));
		corner.add(btns);
		corner.add(presetPnl);
		corner.setBorder(BorderFactory.createTitledBorder("Synth " + synth.getName()));
		return corner;
	}
	
	private void listeners() {
		volume.addChangeListener(e->synth.getGain().setVol(volume.getValue()));
		active.setOpaque(true);
		active.addActionListener(e->{
			synth.setActive(active.isSelected());
			update();
		});
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
		hpReso.addChangeListener(e ->  JudahZone.getSynth().getLoCut().setResonance(hpReso.getValue()));

	}
	
	public void update() {
		active.setText(synth.isActive() ? "On" : "Off");
		active.setBackground(synth.isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
		if (volume.getValue() != synth.getGain().getVol())
			volume.setValue(synth.getGain().getVol());
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
	
}
