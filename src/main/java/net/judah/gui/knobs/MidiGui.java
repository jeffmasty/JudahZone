package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.controllers.Jamstik;
import net.judah.controllers.MPKmini;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Click;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.omni.Threads;
import net.judah.sampler.Sampler;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlists;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Constants;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel implements Cmdr {
	public static final Dimension COMBO_SIZE = new Dimension(125, 28);

	private final JudahClock clock;
	private final Sampler sampler;
	private final Setlists setlists;
	private final TacoTruck tacos;

	@Getter private final KnobMode knobMode = KnobMode.MIDI;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final SongCombo songsCombo = new SongCombo();

	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob(Pastels.MY_GRAY);
	private final Knob swing = new Knob(Pastels.EGGSHELL);
	private final String SWING = " Swing ";

	private final JButton stepPlay = new JButton("▶");
	private final Btn tape = new Btn(" ⏺️ ", e->JudahZone.getMains().tape());

	private final Program taco, fluid;
	private final PianoTrack[] mpkRoutes;
	private final JLabel swingLbl;

	public MidiGui(JudahClock clock, final Jamstik jamstik, Sampler sampler, TacoTruck synths, Setlists setlists) {

		class Lbl extends JLabel {
			Lbl(String txt) {
				super(txt, JLabel.CENTER);
			}
		}

		this.clock = clock;
		this.sampler = sampler;
		this.setlists = setlists;
		this.tacos = synths;
		tape.setOpaque(true);
    	title.add(new JLabel(" Song  "));
    	title.add(Gui.resize(songsCombo, new Dimension(140, 28)));
    	title.add(tape);

		while (tacos.fluid.getTracks().isEmpty())
			Threads.sleep(10);
		mpkRoutes = new PianoTrack[] {tacos.taco.getTrack(), tacos.fluid.getTrack(), tacos.bass.getTrack()};

		taco = new Program(tacos.taco);
		fluid = new Program(tacos.fluid.getTrack());

		sampler.getStepSamples().forEach(s->stepper.addItem(s.toString()));
		stepper.setSelectedIndex(sampler.getSelected());
		stepper.addActionListener(e->{
			if (sampler.getSelected() == stepper.getSelectedIndex())
				return;
			sampler.setSelected(stepper.getSelectedIndex());
			stepVol.setValue((int) (sampler.getStepMix() * 100));
			});
		stepVol.setValue((int) (sampler.getStepMix() * 100));
		stepVol.addListener(val->sampler.setStepMix(val * 0.01f));

		swing.addListener(val->clock.setSwing((val - 50) * 0.01f));
		swingLbl = new Click(SWING, e->eighths());
		stepPlay.addActionListener(e-> sampler.setStepping(!sampler.isStepping()));
		stepPlay.setOpaque(true);

		setOpaque(true);
    	Gui.resize(this, Size.KNOB_PANEL);

    	JPanel labels = new JPanel(new GridLayout(0, 1, 12, 30));
    	JPanel widgets = new JPanel(new GridLayout(0, 1, 12, 7));
    	JPanel crickets = new JPanel();
    	crickets.add(swing);
    	crickets.add(stepper);
    	crickets.add(stepPlay);
    	crickets.add(stepVol);

    	labels.add(new Lbl("Setlist"));
    	labels.add(swingLbl);
    	labels.add(new Octaver(jamstik));
    	labels.add(new ClickIt(tacos.taco.getTrack()));
    	labels.add(new ClickIt(tacos.fluid.getTrack()));

    	widgets.add(setlists.getCombo());
    	widgets.add(crickets);
    	widgets.add(MPKmini.instance.fill(mpkRoutes, mpkRoutes[0]));
    	widgets.add(taco);
    	widgets.add(fluid);

    	JPanel wrap = new JPanel();
    	wrap.add(labels);
    	wrap.add(widgets);
    	wrap.setBorder(Gui.SUBTLE);
    	add(wrap);
        update();
	}

	private void eighths() {
		clock.setEighths(!clock.isEighths());
		swingLbl.setText( clock.isEighths() ? " Swing8" : SWING);
	}

	/**@param idx knob 0 to 7
     * @param data2  user input */
	@Override public boolean doKnob(int idx, int data2) {

    	switch(idx) {
    	case 0: // Select song
 	    	songsCombo.midiShow((File)Constants.ratio(data2 - 1, setlists.getCurrent().list()));
			break;
    	case 1: // set swing
    		clock.setSwing((data2 - 50f) * 0.01f);
    		break;
    	case 2: // Select StepSample
    		sampler.setSelected(Constants.ratio(data2, sampler.getStepSamples().size()));
    		break;
    	case 3: // Metronome volume
    		sampler.setStepMix(data2 * 0.01f);
    		break;
 	    case 4:
 	    	MPKmini.instance.setMidiOut((PianoTrack)Constants.ratio(data2 - 1, mpkRoutes));
    		break;
    	case 5:
    		taco.midiShow(patch(tacos.taco, data2));
    		break;
    	case 6:
    		fluid.midiShow(patch(tacos.fluid, data2));
    		break;
    	case 7: // sync loop length
			if (data2 == 0)
				data2 = 1;
			clock.setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
    		break;
    	default: return false;
    	}
    	return true;
	}

	private String patch(ZoneMidi synth, int data2) {
		return (String) Constants.ratio(data2, synth.getPatches());
	}

	@Override public void pad2() {
		JudahZone.getMains().hotMic();
	}

	@Override public void update() {
		if (stepVol.getValue() != (int) (sampler.getStepMix() * 100))
			stepVol.setValue((int) (sampler.getStepMix() * 100));
		stepper.setSelectedIndex(sampler.getSelected());
		stepPlay.setBackground(sampler.isStepping() ? Pastels.GREEN : null);

		int target = (int) (clock.getSwing() * 100 + 50f);
		if (swing.getValue() != target)
			swing.setValue(target);

	}

	public void updateTape() {
		tape.setBackground(JudahZone.getMains().getTape() == null ? null : Pastels.RED);
	}

	@Override public PianoTrack resolve(String key) {
		for (PianoTrack zone : mpkRoutes)
			if (zone.getName().equals(key))
				return zone;
		return null;
	}

	@Override public void execute(Param p) {
		if (p.cmd != Cmd.MPK)
			return;
		MPKmini.instance.setMidiOut(resolve(p.val));
		MainFrame.update(this);
	}

	@Override public String[] getKeys() {
		String[] result = new String[mpkRoutes.length];
		for (int i = 0; i < result.length; i++)
			result[i] = mpkRoutes[i].getName();
		return result;
	}

	private class Octaver extends Click implements ActionListener {
		private final Jamstik jamstik;
		Octaver(Jamstik jam) {
			super("JAM");
			jamstik = jam;
			addActionListener(this);
		}
		@Override public void actionPerformed(ActionEvent e) {
			if (right)
				jamstik.octaver();
			else
				jamstik.toggle();

			setBackground(jamstik.isActive() ? (jamstik.isOctaver() ? Pastels.GREEN : Pastels.BLUE) : null);
		}
	}

	private class ClickIt extends Click implements ActionListener {
		final PianoTrack t;
		ClickIt(PianoTrack target) {
			super(target.getType().getName());
			t = target;
			addActionListener(this);
		}
		@Override public void actionPerformed(ActionEvent e) {
			if (right)
				t.toggle(Arp.MPK);
			else
				t.setCapture(!t.isCapture());
			setBackground(t.isCapture() ? Pastels.RED :
					t.getArp() == Arp.MPK ? Arp.MPK.getColor() : null);
		}
	}
}
