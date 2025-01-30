package net.judah.gui.knobs;

import static net.judah.fx.Filter.Settings.Frequency;
import static net.judah.fx.Filter.Settings.Resonance;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.fx.Filter;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.TabZone;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.CenteredCombo;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Slider;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.synth.taco.Adsr;
import net.judah.synth.taco.Shape;
import net.judah.synth.taco.TacoSynth;
import net.judah.util.Constants;

public class SynthKnobs extends KnobPanel {
	private static final int OCTAVE = 12;

	@Getter private final KnobMode knobMode = KnobMode.Taco;
	private static final Dimension COMBO = new Dimension(55, STD_HEIGHT);
	public static final String[] DETUNE_OPTIONS = new String[] {
			"OCT", "5th", "+3", "+2", "+1", "+/-0", "-5th", "SUB"};
	public static final float[] DETUNE_AMOUNT = new float[] {
			2f, 1.5f, 1.015f, 1.01f, 1.005f, 1f, 0.75f ,0.5f};
	public static final int DETUNE_NONE = 5;

	@Getter private final TacoSynth synth;
	@Setter @Getter private static boolean freqMode = true; //. vs resonance mode
	@Getter private final Program presets;

	private final Btn save = new Btn("Save", e->save());

	private static final Color C = Pastels.PURPLE;
	private final Knob hcFreq = new Knob(C);
	private final Knob hcReso = new Knob(C);//0, 20
	private final Knob lcFreq = new Knob(C);//0, 50
	private final Knob lcReso = new Knob(C);//0, 20
	private final Adsr adsr;
	@Getter private final Knob a = new Knob(C);//0, 200
	@Getter private final Knob d = new Knob(C);
	@Getter private final Knob s = new Knob(C);
	@Getter private final Knob r = new Knob(C);//0, 500
	private final ArrayList<Slider> gains = new ArrayList<>();
	private final ArrayList<JComboBox<Shape>> shapes = new ArrayList<>();
	private final ArrayList<JComboBox<String>> detune = new ArrayList<>();
	private final JComboBox<Integer> mod = new JComboBox<>();

	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 1));

	public SynthKnobs(TacoSynth zynth) {
		synth = zynth;
		adsr = synth.getAdsr();
		presets = synth.getTracks().isEmpty() ? new Program(synth)
				: new Program(synth.getTracks().get(0));

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
		Gui.resize(mod, MICRO);
		for (int i = 1; i <= OCTAVE; i++)
			mod.addItem(i);
		mod.addActionListener(e->{
			if (synth.getModSemitones() != (int)mod.getSelectedItem())
				synth.setModSemitones((Integer)mod.getSelectedItem());});
		for (int i = 0; i < synth.getDcoGain().length; i++) {
			Slider slider = new Slider(null);
			gains.add(slider);
		}
		JPanel row = new JPanel();
		row.add(presets);
		row.add(save);
		row.add(new JLabel(" Bend "));
		row.add(mod);
 		add(row);

		JPanel knobs = new JPanel(new GridLayout(2, 4, 4, 4));
		knobs.add(Gui.duo(a, new JLabel(" Atk")));
		knobs.add(Gui.duo(d, new JLabel(" Dec")));
		knobs.add(Gui.duo(s, new JLabel(" Sus")));
		knobs.add(Gui.duo(r, new JLabel(" Rel")));
		knobs.add(Gui.duo(lcFreq, new JLabel("LoCut")));
		knobs.add(Gui.duo(lcReso, new JLabel("Res")));
		knobs.add(Gui.duo(hcFreq, new JLabel("HiCut")));
		knobs.add(Gui.duo(hcReso, new JLabel("Res")));
		add(Gui.wrap(knobs));

		JPanel dco = new JPanel();
		dco.setLayout(new BoxLayout(dco, BoxLayout.PAGE_AXIS));
		for (int i = 0; i < synth.getDcoGain().length; i++)
			dco.add(Gui.wrap(new JLabel("DCO " + i + " "), shapes.get(i), gains.get(i), detune.get(i)));
		add(dco);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		update();
		listeners();

		title.add(new JLabel(synth.getIcon()));
		title.add(new JLabel(" "  + synth.getName() + "    " ));
		title.add(new Btn(" next ", e->JudahZone.getTacos().rotate()));
		if (!synth.getTracks().isEmpty())
			title.add(new Btn(Icons.DETAILS_VEW,
					e->TabZone.edit(synth.getTracks().getFirst())));
		validate();
	}

	private void listeners() {
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

		hcFreq.addListener(data2 -> synth.getHiCut().set(Frequency.ordinal(), data2));
		hcReso.addListener(data2 ->  synth.getHiCut().set(Resonance.ordinal(), data2));
		lcFreq.addListener(data2 -> synth.getLoCut().set(Frequency.ordinal(), data2));
		lcReso.addListener(data2 ->  synth.getLoCut().set(Resonance.ordinal(), data2));
		if (synth.getModSemitones() != (int)mod.getSelectedItem())
				mod.setSelectedItem(synth.getModSemitones());
	}

	@Override
	public void update() {
		if (hcReso.getValue() != synth.getHiCut().getResonance())
			hcReso.setValue((int)(synth.getHiCut().getResonance()));
		if (lcReso.getValue() != synth.getLoCut().getResonance())
			lcReso.setValue((int)(synth.getLoCut().getResonance()));
		if (Filter.knobToFrequency(hcFreq.getValue()) != synth.getHiCut().getFrequency())
			hcFreq.setValue((int)Math.ceil(Filter.frequencyToKnob(synth.getHiCut().getFrequency())));
		if (Filter.knobToFrequency(lcFreq.getValue()) != synth.getLoCut().getFrequency())
			lcFreq.setValue((int)Math.ceil(Filter.frequencyToKnob(synth.getLoCut().getFrequency())));
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
			case 4: Threads.execute(() -> presets.setSelectedIndex(
						Constants.ratio(data2, presets.getItemCount() - 1)));
				break;
			case 5: Threads.execute(() -> mod.setSelectedItem(Constants.ratio(data2, OCTAVE)));
				break;
			case 6:
				if (freqMode)
					synth.getLoCut().setFrequency(Filter.knobToFrequency(data2));
				else
					synth.getLoCut().setResonance(data2);
				break;
			case 7:
				if (freqMode)
					synth.getHiCut().setFrequency(Filter.knobToFrequency(data2));
				else
					synth.getHiCut().setResonance(data2);
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void pad1() {
//		JudahSynth target = (synth == JudahZone.getSynth1()) ? JudahZone.getSynth2() : JudahZone.getSynth1();
//		MainFrame.setFocus(target.getSynthKnobs());
	}

	@Override
	public void pad2() {
		freqMode = !freqMode;
		MainFrame.update(this); // TODO freqMode feedback in update()

	}

}
