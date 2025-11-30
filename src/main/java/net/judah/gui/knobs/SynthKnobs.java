package net.judah.gui.knobs;

import static net.judah.synth.taco.MonoFilter.Settings.Resonance;
import static net.judah.synth.taco.TacoSynth.DCO_COUNT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.CenteredCombo;
import net.judah.gui.widgets.DoubleSlider;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Slider;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.synth.taco.Adsr;
import net.judah.synth.taco.MonoFilter;
import net.judah.synth.taco.Shape;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Constants;

public class SynthKnobs extends KnobPanel {
	private static final int OCTAVE = 12;
	private static final int rFactor = 10;

	private static final Dimension COMBO = new Dimension(55, STD_HEIGHT);
	public static final String[] DETUNE_OPTIONS = new String[] {
			"OCT", "5th", "+3", "+2", "+1", "+/-0", "-5th", "SUB"};
	public static final float[] DETUNE_AMOUNT = new float[] {
			2f, 1.5f, 1.015f, 1.01f, 1.005f, 1f, 0.75f ,0.5f};
	public static final int DETUNE_NONE = 5;
	private static final Color KNOB_C = Pastels.BLUE;

	@Setter private static boolean freqMode = true; //. vs resonance mode
	@Getter private final TacoSynth synth;
	@Getter private final KnobMode knobMode = KnobMode.Taco;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
	private final Program presets;
	private final DoubleSlider filter;
	private final Knob hcReso = new Knob(KNOB_C);
	private final Knob lcReso = new Knob(KNOB_C);
	private final Adsr adsr;
	private final Knob a = new Knob(KNOB_C);//0, 200
	private final Knob d = new Knob(KNOB_C);
	private final Knob s = new Knob(KNOB_C);
	private final Knob r = new Knob(KNOB_C);//0, 500
	private final ArrayList<Slider> gains = new ArrayList<>();
	private final ArrayList<JComboBox<Shape>> shapes = new ArrayList<>();
	private final JComboBox<Integer> mod = new JComboBox<>();

	private final JSlider detune0 = new JSlider(-100, 100);
	private final JComboBox<String> detune1 = new JComboBox<String>(DETUNE_OPTIONS);
	private final JComboBox<String> detune2 = new JComboBox<String>(DETUNE_OPTIONS);
	private final Component[] detune;

	public SynthKnobs(TacoSynth zynth) {
		synth = zynth;
		adsr = synth.getAdsr();
		presets = new Program(synth);

		for (int i = 0; i < DCO_COUNT; i++) {
			JComboBox<Shape> combo = new CenteredCombo<>();
			for(Shape val : Shape.values())
				combo.addItem(val);
			combo.setMaximumSize(COMBO);
			shapes.add(combo);
		}

		detune0.setValue(0);
		detune0.setToolTipText("Detune Osc 0");
		detune1.setSelectedIndex(DETUNE_NONE);
		detune1.setToolTipText("Detune Osc 1");
		detune2.setSelectedIndex(DETUNE_NONE);
		detune2.setToolTipText("Detune Osc 2");
		detune = new Component[] {detune0, detune1, detune2};

		mod.setToolTipText("Pitchbend Semitones");
		Gui.resize(mod, MICRO);
		for (int i = 1; i <= OCTAVE; i++)
			mod.addItem(i);
		mod.addActionListener(e->{
			if (synth.getModSemitones() != (int)mod.getSelectedItem())
				synth.setModSemitones((Integer)mod.getSelectedItem());});
		for (int i = 0; i < DCO_COUNT; i++)
			gains.add(new Slider(null));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(Box.createVerticalStrut(12));

		Box me = new Box(BoxLayout.PAGE_AXIS);
		me.add(new JLabel(((TacoTruck)synth.getMidiOut()).getIcon()));
		me.add(new JLabel(synth.getName(), JLabel.CENTER));
		me.setBorder(Gui.SUBTLE);
		JPanel env = new JPanel(new GridLayout(1, 4, 6, 8));
		env.add(Gui.duo(a, new JLabel(" Atk")));
		env.add(Gui.duo(d, new JLabel(" Dk ")));
		env.add(Gui.duo(s, new JLabel(" Sus")));
		env.add(Gui.duo(r, new JLabel(" Rel")));
		Box wrap = new Box(BoxLayout.LINE_AXIS);
		wrap.add(Box.createHorizontalStrut(10));
		wrap.add(me);
		wrap.add(Box.createHorizontalStrut(15));
		wrap.add(env);
		add(wrap);

		JPanel dco = new JPanel(new GridLayout(shapes.size(), 4, 5, 1));
		Dimension sz = Size.SMALLER_COMBO;
		for (int i = 0; i < DCO_COUNT; i++) {
			dco.add(new JLabel("DCO " + i + " ", JLabel.RIGHT));
			dco.add(shapes.get(i));
			dco.add(Gui.resize(gains.get(i), Size.MEDIUM_COMBO));
			dco.add(Gui.resize(detune[i], sz));
		}
		wrap = new Box(BoxLayout.LINE_AXIS);
		wrap.add(Box.createHorizontalGlue());
		wrap.add(dco);
		wrap.add(Box.createHorizontalStrut(25));
		wrap.add(Box.createHorizontalGlue());
		add(wrap);


		int ordinal = MonoFilter.Settings.Frequency.ordinal();
		filter = new DoubleSlider(synth.getHighPass(), ordinal, synth.getLowPass(), ordinal);
		JPanel res = new JPanel();
		res.setLayout(new BoxLayout(res, BoxLayout.LINE_AXIS));
		res.add(Box.createHorizontalStrut(10));
		res.add(lcReso);
		res.add(Box.createHorizontalStrut(5));
		res.add(new JLabel("<html>Lo<br>Res</html>"));
		res.add(Box.createHorizontalStrut(25));
		res.add(new JLabel("Filter"));
		res.add(Box.createHorizontalStrut(25));
		res.add(new JLabel("<html>Hi<br>Res</html>"));
		res.add(Box.createHorizontalStrut(5));
		res.add(hcReso);
		res.add(Box.createHorizontalStrut(10));

		JPanel filters = new JPanel();
		filters.setLayout(new BoxLayout(filters, BoxLayout.PAGE_AXIS));
		filters.add(Gui.resize(filter, new Dimension(Size.WIDTH_KNOBS - 100, 30)));
		filters.add(Gui.wrap(res));

		add(Box.createVerticalStrut(8));
		add(filters);

		title.add(presets);
		title.add(new Btn(Icons.SAVE, e->save()));
		title.add(mod);
		title.add(new JLabel("Bend"));

		validate();
		update();
		listeners();
	}

	private void listeners() {
		for (int i = 0; i < DCO_COUNT; i++) {
			final int idx = i;
			gains.get(i).addChangeListener(e->synth.setGain(idx, gains.get(idx).getValue() * 0.01f));
		}
		for (int i = 0; i < DCO_COUNT; i++) {
			final int idx = i;
			shapes.get(i).addActionListener(e->synth.setShape(idx, (Shape)shapes.get(idx).getSelectedItem()));
		}

		detune0.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					synth.setDetune(50);
					conformDetune();
				}
			}});
		detune0.addChangeListener(l-> { // setDetune0() {
			float existing = synth.getDetune()[0];
			int translated = cents(existing);
			int knob = detune0.getValue();
			if (translated != knob)
				synth.setDetune(knob);
		});

		detune1.addActionListener(e->synth.getDetune()[1] = DETUNE_AMOUNT[detune1.getSelectedIndex()]);
		detune2.addActionListener(e->synth.getDetune()[2] = DETUNE_AMOUNT[detune2.getSelectedIndex()]);

		a.addListener(e->adsr.setAttackTime(a.getValue()));
		d.addListener(e->adsr.setDecayTime(d.getValue()));
		s.addListener(e-> adsr.setSustainGain(s.getValue() * 0.01f));
		r.addListener(e->adsr.setReleaseTime(rFactor * r.getValue()));

		hcReso.addListener(data2 ->  synth.getLowPass().set(Resonance.ordinal(), data2));
		lcReso.addListener(data2 ->  synth.getHighPass().set(Resonance.ordinal(), data2));
		if (synth.getModSemitones() != (int)mod.getSelectedItem())
				mod.setSelectedItem(synth.getModSemitones());
	}

	@Override
	public void update() {
		if (hcReso.getValue() != synth.getLowPass().get(Resonance.ordinal()))
			hcReso.setValue(synth.getLowPass().get(Resonance.ordinal()));
		if (lcReso.getValue() != synth.getHighPass().get(Resonance.ordinal()))
			lcReso.setValue((synth.getHighPass().get(Resonance.ordinal())));
		filter.update();
		for (int i = 0; i < DCO_COUNT; i++)
			checkGain(gains.get(i), i);
		for (int i = 0; i < DCO_COUNT; i++)
			checkShape(shapes.get(i), i);

		// adsr update
		if (a.getValue() != adsr.getAttackTime())
			a.setValue(adsr.getAttackTime());
		if (d.getValue() != adsr.getDecayTime())
			d.setValue(adsr.getDecayTime());
		if (s.getValue() * 0.01f != adsr.getSustainGain())
			s.setValue((int)(adsr.getSustainGain() * 100));
		if (r.getValue() != adsr.getReleaseTime() / rFactor)
			r.setValue(adsr.getReleaseTime() / rFactor);

		conformDetune();
	}

	public static int cents(float detune) {
		return (int) ((detune - 1f) * 1000f);
	}

	private void conformDetune() {
		int cents = cents(synth.getDetune()[0]);
		if (detune0.getValue() != cents)
			detune0.setValue(cents);

		String target = DETUNE_OPTIONS[DETUNE_NONE];
		for (int x = 0; x < DETUNE_AMOUNT.length; x++)
			if (synth.getDetune()[1] == DETUNE_AMOUNT[x]) {
				target = DETUNE_OPTIONS[x];
				break;
			}
		if (detune1.getSelectedItem() != target)
			detune1.setSelectedItem(target);
		target = DETUNE_OPTIONS[DETUNE_NONE];
		for (int x = 0; x < DETUNE_AMOUNT.length; x++)
			if (synth.getDetune()[2] == DETUNE_AMOUNT[x]) {
				target = DETUNE_OPTIONS[x];
				break;
			}
		if (detune2.getSelectedItem() != target)
			detune2.setSelectedItem(target);
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
		String name = JOptionPane.showInputDialog(JudahZone.getFrame(), "Synth Preset Name", synth.getProgram());
		if (name == null || name.length() == 0)
			return;
		JudahZone.getSynthPresets().save(synth, name);
		presets.refill(synth.getPatches(), name);
		synth.progChange(name);
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
				adsr.setReleaseTime(rFactor * data2);
				break;
			case 4:
				int focus = Constants.ratio(data2, presets.getItemCount() - 1);
				presets.midiShow(synth.getPatches()[focus]);
				//	Threads.execute(() -> presets.setSelectedIndex(
				//			Constants.ratio(data2, presets.getItemCount() - 1)));
				break;
			case 5:
				if (freqMode)
					Threads.execute(() -> mod.setSelectedItem(Constants.ratio(data2, OCTAVE)));
				else
					detune0.setValue(data2);
				break;
			case 6:
				if (freqMode)
					synth.getHighPass().setFrequency(MonoFilter.knobToFrequency(data2));
				else
					synth.getHighPass().set(MonoFilter.Settings.Resonance.ordinal(), data2);
				break;
			case 7:
				if (freqMode)
					synth.getLowPass().setFrequency(MonoFilter.knobToFrequency(data2));
				else
					synth.getLowPass().set(MonoFilter.Settings.Resonance.ordinal(), data2);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void pad1() {

	}

	@Override
	public void pad2() {
		freqMode = !freqMode;
		MainFrame.update(this); // TODO freqMode feedback in update()

	}

}
