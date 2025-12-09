package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.controllers.Jamstik;
import net.judah.controllers.MPKmini;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Click;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiInstrument;
import net.judah.omni.Icons;
import net.judah.sampler.Sampler;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.Computer.TrackUpdate;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlists;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Constants;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel implements Cmdr {
	public static final Dimension COMBO_SIZE = new Dimension(125, 28);
	public static final String REC = " ⏺️ ";
	public static final String MPK = " 🔗 ";
	public static final String PLAY = "    ▶    ";

	private final JudahClock clock;
	private final Sampler sampler;
	private final Setlists setlists;

	@Getter private final KnobMode knobMode = KnobMode.MIDI;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	@Getter private final RecMpk toggler;
	private final SongCombo songsCombo = new SongCombo();
	private final Btn tape = new Btn(REC, e->JudahZone.getMains().tape());
	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob(Pastels.MY_GRAY);
	private final Knob swing = new Knob(Pastels.EGGSHELL);
	private final String SWING = " Swing ";
	private final JButton stepPlay = new JButton(PLAY);
	private final Program taco, fluid;
	private final ZoneMidi[] mpkRoutes;
	private final JLabel swingLbl;
	private final FluidSynth fluidSynth;
	private final TacoTruck tacoTruck;

	public MidiGui(TacoTruck t1, FluidSynth f1, MidiInstrument b1, JudahClock clock, final Jamstik jamstik, Sampler sampler, Setlists setlists) {

		class Lbl extends JLabel {
			Lbl(String txt) {super(txt, JLabel.CENTER);}}

		this.clock = clock;
		this.sampler = sampler;
		this.setlists = setlists;
		this.fluidSynth = f1;
		this.tacoTruck = t1;
		tape.setOpaque(true);
    	title.add(new JLabel(" Song  "));
    	title.add(Gui.resize(songsCombo, new Dimension(140, 28)));
    	title.add(new Btn(Icons.SAVE, e->JudahZone.getOverview().save()));
    	title.add(tape);

		mpkRoutes = new ZoneMidi[] {t1, f1, b1};
		toggler = new RecMpk(t1, f1, b1);

		taco = new Program(t1.getTrack());
		((JLabel)taco.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		fluid = new Program(f1.getTrack());
		((JLabel)fluid.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

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

    	JPanel labels = new JPanel(new GridLayout(0, 1, 1, 30));
    	JPanel widgets = new JPanel(new GridLayout(0, 1, 1, 7));
    	JPanel btns = new JPanel(new GridLayout(0, 1, 1, 7));

    	JPanel crickets = new JPanel();
    	crickets.add(swing);
    	crickets.add(stepper);

    	JPanel temp = new JPanel();
    	temp.setLayout(new GridLayout(1, 2));
    	temp.add(Gui.wrap(stepPlay));
    	temp.add(Gui.wrap(stepVol));
    	btns.add(temp);
    	temp = new JPanel();
    	temp.add(jamstik.getToggleBtn());
    	temp.add(jamstik.getOctBtn());
    	btns.add(temp);
    	btns.add(toggler.getTaco());
    	btns.add(toggler.getFluid());
    	btns.add(toggler.getBass());

    	labels.add(swingLbl);
    	labels.add(new Octaver(jamstik));
    	labels.add(new Click("Taco", e->TabZone.edit(t1.getTrack())));
    	labels.add(new Click("Fluid", e->TabZone.edit(f1.getTrack())));
    	labels.add(new Lbl("Setlist"));

    	widgets.add(crickets);
    	widgets.add(MPKmini.instance.fill(mpkRoutes, mpkRoutes[0]));
    	widgets.add(taco);
    	widgets.add(fluid);
    	temp = new JPanel();
    	temp.setLayout(new BoxLayout(temp, BoxLayout.LINE_AXIS));
    	temp.add(setlists.getCombo());
    	temp.add(new Click(" Bass", e->TabZone.edit(b1.getTrack())));
    	widgets.add(temp);

    	JPanel wrap = new JPanel();
    	wrap.add(new JLabel("  "));
    	wrap.add(labels);
    	wrap.add(widgets);
    	wrap.add(Gui.wrap(btns));
    	add(Gui.wrap(wrap));
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
 	    	MPKmini.instance.setMidiOut((ZoneMidi)Constants.ratio(data2 - 1, mpkRoutes));
    		break;
    	case 5:
    		taco.midiShow(patch(tacoTruck, data2));
    		break;
    	case 6:
    		fluid.midiShow(fluidSynth.getTrack().getPatches()[data2]);
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
//		taco.update();
//		fluid.update();
	}

	public void update(TrackUpdate update) {
		if (Update.PROGRAM == update.type()) {
			if (update.track() == taco.getTrack())
				taco.update();
			else if (update.track() == fluid.getTrack())
				fluid.update();
		}
		else if (Update.ARP == update.type())
			toggler.update((PianoTrack)update.track());
		else if (Update.CAPTURE == update.type()) {
			toggler.update((PianoTrack)update.track());
		}
	}

	public void update(PianoTrack p) {
		toggler.update(p);
	}

	public void updateTape() {
		tape.setBackground(JudahZone.getMains().getTape() == null ? null : Pastels.RED);
	}

	@Override public ZoneMidi resolve(String key) {
		for (ZoneMidi zone : mpkRoutes)
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

	public class RecMpk {
		@Getter JPanel taco = new JPanel();
		@Getter JPanel fluid = new JPanel();
		@Getter JPanel bass = new JPanel();

		private static final int TACO = 0;
		private static final int FLUID = 1;
		private static final int BASS = 2;
		private static final int LENGTH = 3;

		private final ZoneMidi[] tracks = new ZoneMidi[LENGTH];
		private final JToggleButton[] capture = new JToggleButton[LENGTH];
		private final JToggleButton[] mpk = new JToggleButton[LENGTH];

		public RecMpk(ZoneMidi tk1, ZoneMidi f1, ZoneMidi b1) {
			tracks[TACO] = tk1;
			tracks[FLUID] = f1;
			tracks[BASS] = b1;
			for (int i = 0; i < 3; i++) {
				capture[i] = new JToggleButton(REC);
				mpk[i] = new JToggleButton(MPK);
				final int idx = i;
				capture[i].addActionListener(l->capture(capture[idx]));
				mpk[i].addActionListener(l->mpk(mpk[idx]));
			}

			taco.add(capture[TACO]);
			taco.add(mpk[TACO]);
			fluid.add(capture[FLUID]);
			fluid.add(mpk[FLUID]);
			bass.add(capture[BASS]);
			bass.add(mpk[BASS]);
		}

		void update(PianoTrack in) {
			int idx = -1;
			for (int i = 0; i < LENGTH; i++)
				if (in == tracks[i]) {
					idx = i;
					break;
				}
			if (idx == -1)
				return;
			if (capture[idx].isSelected() != in.isCapture())
				capture[idx].setSelected(in.isCapture());
			if (mpk[idx].isSelected() != (in.getArp() == Arp.MPK))
				mpk[idx].setSelected(in.getArp() == Arp.MPK);
		}

		public void capture(ZoneMidi p, boolean on) {
			capture(index(p), on);
		}
		public void mpk(ZoneMidi p, boolean on) {
			mpk(index(p), on);
		}
		void capture(JToggleButton clicked) {
			capture(index(clicked), clicked.isSelected());
		}

		void mpk(JToggleButton clicked) {
			mpk(index(clicked), clicked.isSelected());
		}

		void capture(int idx, boolean on) {
			mpkOff();
			for (int i = 0; i < LENGTH; i++)
				if (i == idx) {
					tracks[i].getTrack().setCapture(on);
					if (on)
						TabZone.edit(tracks[i].getTrack());
				}
				else if (capture[i].isSelected())
					tracks[i].getTrack().setCapture(false);

		}

		void mpk(int idx, boolean on) {
			captureOff();
			for (int i = 0; i < LENGTH; i++)
				if (i == idx)
					((PianoTrack)tracks[i].getTrack()).setArp(on ? Arp.MPK : Arp.Off);
				else if (mpk[i].isSelected())
					((PianoTrack)tracks[i].getTrack()).setArp(Arp.Off);
		}

		void mpkOff() {
			for (int i = 0; i < LENGTH; i++)
				if (mpk[i].isSelected())
					((PianoTrack)tracks[i].getTrack()).setArp(Arp.Off);
		}
		void captureOff() {
			for (int i = 0; i < LENGTH; i++)
				if (capture[i].isSelected()) {
					tracks[i].getTrack().setCapture(false);
				}
		}

		int index(JToggleButton clicked) {
			for (int i = 0; i < LENGTH; i++)
				if (capture[i] == clicked)
					return i;
			for (int i = 0; i < LENGTH; i++)
				if (mpk[i] == clicked)
					return i;
			return -1;
		}
		int index(ZoneMidi t) {
			for (int i = 0; i < LENGTH; i++)
				if (t == tracks[i])
					return i;
			return -1;
		}

	} // end RecMpk



}
