package net.judah.gui.knobs.synth;

import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.synth.Dco.DcoJson;
import judahzone.synth.SynthDB;
import judahzone.synth.WavSynth;
import judahzone.util.Folders;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.channel.Generator;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.settable.Program;
import net.judah.seq.track.Computer.Update;

public class SynthKnobs extends KnobPanel {

	private enum TABS {Synth, Envelope, DCOs}

	@Getter private final WavSynth synth;
	@Getter private final KnobMode knobMode = KnobMode.Taco;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
	private final Program program;
	private final DcoList list;
	private final FourStepEnv env;
	private final SynthFilter filter;
	private final GainStage gainStage;

	private final JTabbedPane tabs = new JTabbedPane();

	public SynthKnobs(WavSynth zynth, Generator line) {
		synth = zynth;
		program = new Program(synth);
		env = new FourStepEnv(synth);
		filter = new SynthFilter(synth);
		list = new DcoList(synth);
		gainStage = new GainStage(synth, line);

		title.add(program);
		title.add(new Btn(Icons.SAVE, e->SynthDB.save(Folders.getSynthPresets(), synth)));

		// prepare list panel to hold Oscbar components
		list.load(synth.getAlgo());

		JScrollPane scroll = new JScrollPane(list);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


		Box result = new Box(BoxLayout.Y_AXIS);
		result.add(Box.createVerticalStrut(10));
		result.add(gainStage);
		result.add(Gui.wrap(filter));
		result.add(Box.createVerticalStrut(5));

		tabs.add(result, TABS.Synth.name());
		tabs.add(env, TABS.Envelope.name());
		tabs.add(scroll, TABS.DCOs.name());

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(tabs);
		add(Box.createVerticalGlue());
		validate();
		update();
	}


	@Override
	public void update() {
		DcoJson[] algo = synth.getAlgo();

		// If the count changed (e.g. from 2 to 3), perform a full reload of the list
		if (algo.length != list.barCount()) {
		    list.load(algo);
		} else {
		    for (int i = 0; i < algo.length; i++)
		        list.updateBar(i, algo[i]);
		}

		env.update();
		filter.update();
		gainStage.update();
	}

	public static int cents(float detune) {
		return (int) ((detune - 1f) * 1000f);
	}

	private TABS mode() {
		tabs.getSelectedIndex();
		return TABS.values()[tabs.getSelectedIndex()];
	}

	// amp k1-8
	// env k1-8
	// filter k1-8
	// dco k1-8

	@Override
	public boolean doKnob(int idx, int data2) {
		TABS mode = mode();
		// TODO tabs mode

		// preset, volume, hi-cut hz, lo-cut res, adsr
		switch (idx) {
//			case 0:
//				adsr.attack = data2;
//				break;
//			case 1:
//				adsr.decay = data2;
//				break;
//			case 2:
//				adsr.sustain = data2;
//				break;
//			case 3:
//				adsr.release = rFactor * data2;
//				break;
//			case 4:
//				int focus = Constants.ratio(data2, program.getItemCount() - 1);
//				program.midiShow(synth.getPatches()[focus]);
//				//	Threads.execute(() -> presets.setSelectedIndex(
//				//		Constants.ratio(data2, presets.getItemCount() - 1)));
//				break;
//			case 5:
//				if (freqMode)
//					Threads.execute(() -> mod.setSelectedItem(Constants.ratio(data2, OCTAVE)));
//				else
//					detune0.setValue(data2);
//				break;
//			case 6:
//				if (freqMode)
//					synth.getHighPass().setFrequency(MonoFilter.knobToFrequency(data2));
//				else
//					synth.getHighPass().set(MonoFilter.Settings.Resonance.ordinal(), data2);
//				break;
//			case 7:
//				if (freqMode)
//					synth.getLowPass().setFrequency(MonoFilter.knobToFrequency(data2));
//				else
//					synth.getLowPass().set(MonoFilter.Settings.Resonance.ordinal(), data2);
//				break;
//			default:
//				return false;
		}
		return true;
	}

	@Override
	public void pad1() {

	}

	@Override
	public void pad2() {
// 		freqMode = !freqMode;
// 		MainFrame.update(this); // TODO freqMode feedback in update()

	}

	public void update(Update type) {
		if (Update.PROGRAM != type)
			return;
		update();
		program.update();
	}

}
