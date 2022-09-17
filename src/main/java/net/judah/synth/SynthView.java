package net.judah.synth;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.CutFilter;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.Slider;

public class SynthView extends JPanel implements ListCellRenderer<JPanel> {
	private static final Dimension SLIDER = new Dimension(75, STD_HEIGHT);
	private static final Dimension COMBO = new Dimension(60, STD_HEIGHT);
	
	private final JudahSynth synth;
	
	private final JLabel title = new JLabel("Judah Synth", JLabel.LEFT);
	private final JToggleButton active = new JToggleButton("On");
	private final Slider volume = new Slider(null);
	private final Slider lpFreq = new Slider(e -> JudahZone.getSynth().getHiCut()
			.setFrequency(CutFilter.knobToFrequency(((Slider)e.getSource()).getValue())));
	private final Slider lpReso = new Slider(e ->  JudahZone.getSynth().getHiCut()
			.setResonance(((Slider)e.getSource()).getValue() * 0.25f));
	private final JButton fx = new JButton("Fx");
	
	private final Slider a = new Slider(1, 25, null);
	private final Slider d = new Slider(1, 25, null);
	private final Slider s = new Slider(1, 100, null);
	private final Slider r = new Slider(1, 25, null);
	
	private final Slider gain1 = new Slider(null); 
	private final Slider gain2 = new Slider(null);
	private final Slider gain3 = new Slider(null);

	private final JComboBox<Shape> shape1 = new JComboBox<>();
	private final JComboBox<Shape> shape2 = new JComboBox<>();
	private final JComboBox<Shape> shape3 = new JComboBox<>();
	
	// sub/harmonics

	@SuppressWarnings("unchecked")
	SynthView(JudahSynth zoundz) {
		this.synth = zoundz;
		active.setOpaque(true);
		for (JComboBox<Shape> box : new JComboBox[] {shape1, shape2, shape3}) {
			for(Shape s : Shape.values())
				box.addItem(s);
			box.setMaximumSize(COMBO);
		}
		update();

		volume.addChangeListener(e->synth.getGain().setVol(volume.getValue()));
		active.addActionListener(e->{
			synth.setActive(active.isSelected());
			update();
		});

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.PAGE_AXIS));
		title.setFont(Constants.Gui.BOLD13);
		JPanel placeholder = new JPanel();
		placeholder.add(title);
		top.add(placeholder);
		
		JPanel btns = new JPanel();
		btns.add(active);
		fx.addActionListener(e -> MainFrame.setFocus(synth));
		btns.add(fx);
//		top.add(btns);
		
		a.addChangeListener(e->synth.getEnv().setAttackTime(a.getValue()));
		d.addChangeListener(e->synth.getEnv().setDecayTime(d.getValue()));
		s.addChangeListener(e-> synth.getEnv().setSustainGain(s.getValue() * 0.01f));
		r.addChangeListener(e->synth.getEnv().setReleaseTime(r.getValue()));
		
		gain1.addChangeListener(e->synth.setGain(0, gain1.getValue() * 0.01f));
		gain2.addChangeListener(e->synth.setGain(1, gain2.getValue() * 0.01f));
		gain3.addChangeListener(e->synth.setGain(2, gain3.getValue() * 0.01f));
		
		shape1.addActionListener(e->synth.setShape(0, (Shape)shape1.getSelectedItem()));
		shape2.addActionListener(e->synth.setShape(1, (Shape)shape2.getSelectedItem()));
		shape3.addActionListener(e->synth.setShape(2, (Shape)shape3.getSelectedItem()));
		
		for (Slider slider : new Slider[] {
				volume, lpFreq, lpReso, a, d, s, r, gain1, gain2, gain3}) {
			slider.setPreferredSize(SLIDER);
			slider.setMaximumSize(SLIDER);
		}
		
		setLayout(new GridLayout(0, 3));
		add(top);
		add(btns);
		add(duo(new JLabel("Attak "), a));
		
		add(duo(new JLabel(" Volume "), volume));
		add(duo(shape1, gain1));
		add(duo(new JLabel("Decay "), d));
		
		add(duo(new JLabel("  HiCut  "), lpFreq));
		add(duo(shape2, gain2));
		add(duo(new JLabel("Sustain "), s));
		
		add(duo(new JLabel(" Resonance"), lpReso));
		add(duo(shape3, gain3));
		add(duo(new JLabel("Release "), r));
		
	}
	
	private JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.LINE_AXIS));
		result.add(left);
		result.add(right);
		return result;
	}
	

	public void update() {
		active.setText(synth.isActive() ? "On" : "Off");
		active.setBackground(synth.isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
		if (volume.getValue() != synth.getGain().getVol())
			volume.setValue(synth.getGain().getVol());
		Adsr env = synth.getEnv();
		if (a.getValue() != env.getAttackTime())
			a.setValue(env.getAttackTime());
		if (d.getValue() != env.getDecayTime())
			d.setValue(env.getDecayTime());
		if (s.getValue() * 0.01f != env.getSustainGain())
			s.setValue((int)(env.getSustainGain() * 100));
		if (r.getValue() != env.getReleaseTime())
			r.setValue(env.getReleaseTime());
		checkGain(gain1, 0);
		checkGain(gain2, 1);
		checkGain(gain3, 2);
		
		checkShape(shape1, 0);
		checkShape(shape2, 1);
		checkShape(shape3, 2);
	}

	private void checkShape(JComboBox<Shape> shape, int i) {
		if (shape.getSelectedItem() != synth.getShape(i))
			shape.setSelectedItem(synth.getShape(i));
	}

	private void checkGain(Slider gain, int i) {
		if (gain.getValue() * 0.01f != synth.getDcoGain()[i])
			gain.setValue((int)(synth.getDcoGain()[i] * 100));
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends JPanel> list, JPanel value, int index,
			boolean isSelected, boolean cellHasFocus) {
		return this;
	}
	
	
}
