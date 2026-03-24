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
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import judahzone.api.MidiOut;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.gui.Pastels;
import judahzone.util.Constants;
import judahzone.widgets.Btn;
import judahzone.widgets.Click;
import judahzone.widgets.Knob;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Generator;
import net.judah.channel.Mains;
import net.judah.controllers.Jamstik;
import net.judah.controllers.MPKmini;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.midi.MidiInstrument;
import net.judah.sampler.Sampler;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlists;
import net.judah.synth.SynthEngine;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel implements Cmdr {
	public static final Dimension COMBO_SIZE = new Dimension(125, 28);
	public static final String REC = " ⏺️ ";
	public static final String MPK = " 🔗 ";
	public static final String PLAY = "    ▶    ";

	private final JudahClock clock;
	private final Sampler sampler;
	private final Setlists setlists;
	private final Mains mains;
	private final MPKmini mpkMini;

	@Getter private final KnobMode knobMode = KnobMode.MIDI;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	@Getter private RecMpk toggler;
	private final SongCombo songsCombo;
	private final Btn tape;
	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob();
	private final Knob swing = new Knob();
	private final String SWING = " Swing ";
	private final JButton stepPlay = new JButton(PLAY);

	private PianoTrack[] mpkRoutes = new PianoTrack[0];
	private JLabel swingLbl;
	private Jamstik jamstik;

	private Program taco;
	private Program fluid;

	class Lbl extends JLabel {
		Lbl(String txt) {super(txt, JLabel.CENTER);} }

	public MidiGui(JudahZone zone, JudahClock clock, Jamstik jamstik) {

		this.clock = clock;
		this.sampler = zone.getSampler();
		this.setlists = zone.getSetlists();
		this.mains = zone.getMains();
		this.mpkMini = zone.getMpkMini();
		this.jamstik = jamstik;
		tape = new Btn(REC, e->mains.tape());
		tape.setOpaque(true);
		songsCombo = new SongCombo(zone);

		// lightweight title portion stays in ctor
		title.add(new JLabel(" Song  "));
		title.add(Gui.resize(songsCombo, new Dimension(140, 28)));
		title.add(new Btn(Icons.SAVE, e->zone.getOverview().save()));
		title.add(tape);

		// basic controls that don't require heavy wiring
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
		stepPlay.addActionListener(e-> sampler.setStepping(!sampler.isStepping()));
		stepPlay.setOpaque(true);

		setOpaque(true);
		Gui.resize(this, Size.KNOB_PANEL);

		// keep constructor minimal: do layout in ping()
		add(Gui.wrap(new JPanel()));
		update();
	}

	public void ping(Generator tacoSynth, SynthEngine fluidSynth, MidiInstrument bass) {

		mpkRoutes = new PianoTrack[] {tacoSynth.getTrack(), (PianoTrack)fluidSynth.getTrack(), bass.getTrack()};
		toggler = new RecMpk(mpkRoutes);

		removeAll();

		// Build the rest of the layout similar to previous ctor code
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

		for (JPanel row : toggler.getButtons())
			btns.add(row);

		labels.add(swingLbl == null ? new Click(SWING, e->eighths()) : swingLbl);
		labels.add(new Octaver(jamstik));
		labels.add(new Click("Taco", e->TabZone.edit(tacoSynth.getTrack())));
		labels.add(new Click("Fluid", e->TabZone.edit(fluidSynth.getTrack())));
		labels.add(new Lbl("Setlist"));

		widgets.add(crickets);
		widgets.add(mpkMini.fill(mpkRoutes, mpkRoutes[0]));

		taco = new Program(tacoSynth.getTrack());
		((JLabel)taco.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		fluid = new Program(fluidSynth.getTrack());
		((JLabel)fluid.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

		widgets.add(taco);
		widgets.add(fluid);
		JLabel bassClick = new Click(" Bass", e->TabZone.edit(bass.getTrack()));
		widgets.add(Gui.box(setlists.getCombo(), bassClick));

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
 	    	mpkMini.setMidiOut((PianoTrack)Constants.ratio(data2 - 1, mpkRoutes));
			break;
    	case 5:
    		taco.midiShow(patch(taco.getMidiOut(), data2));
			break;
    	case 6:
    		fluid.midiShow(patch(fluid.getMidiOut(), data2));
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

	private String patch(MidiOut synth, int data2) {
		return (String) Constants.ratio(data2, synth.getPatches());
	}


	@Override public void pad2() {
		mains.hotMic();
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

	public void update(Update type, Computer c) {
		if (Update.PROGRAM == type) {
			fluid.update(); // TODO overkill
			taco.update();
		}
		else if (Update.ARP == type)
			toggler.update((PianoTrack)c);
		else if (Update.CAPTURE == type) {
			if (c instanceof PianoTrack p)
			toggler.update(p);
		}
	}

	public void update(PianoTrack p) {
		toggler.update(p);
	}

	public void updateTape() {
		tape.setBackground(mains.getTape() == null ? null : Pastels.RED);
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
		mpkMini.setMidiOut(resolve(p.val));
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
			if (rightClick)
				jamstik.octaver();
			else
				jamstik.toggle();

			setBackground(jamstik.isActive() ? (jamstik.isOctaver() ? Pastels.GREEN : Pastels.BLUE) : null);
		}
	}

	public class RecMpk {
		// toggle capture and MPK routing for each track, ensuring only one capture and one MPK at a time.

		private final NoteTrack[] tracks;
		private final JToggleButton[] capture;
		private final JToggleButton[] mpk;

		private final int LENGTH;

		public RecMpk(PianoTrack[] list) {
			LENGTH = list.length;
			tracks = new PianoTrack[LENGTH];
			capture = new JToggleButton[LENGTH];
			mpk = new JToggleButton[LENGTH];

			for (int i = 0; i < LENGTH; i++) {
				final int idx = i;
				tracks[i] = list[i];
				capture[i] = new JToggleButton(REC);
				mpk[i] = new JToggleButton(MPK);
				capture[i].addActionListener(l->capture(capture[idx]));
				mpk[i].addActionListener(l->mpk(mpk[idx]));
			}

		}

		public JPanel[] getButtons() {
			JPanel[] result = new JPanel[LENGTH];
			for (int i = 0; i < LENGTH; i++) {
				result[i] = new JPanel();
				result[i].add(capture[i]);
				result[i].add(mpk[i]);
			}
			return result;
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

		public void capture(MidiOut p, boolean on) {
			capture(index(p), on);
		}
		public void mpk(MidiOut p, boolean on) {
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
					tracks[i].setCapture(on);
					if (on)
						TabZone.edit(tracks[i]);
				}
				else if (capture[i].isSelected())
					tracks[i].setCapture(false);

		}

		void mpk(int idx, boolean on) {
			captureOff();
			for (int i = 0; i < LENGTH; i++)
				if (i == idx)
					((PianoTrack)tracks[i]).setArp(on ? Arp.MPK : Arp.Off);
				else if (mpk[i].isSelected())
					((PianoTrack)tracks[i]).setArp(Arp.Off);
		}

		void mpkOff() {
			for (int i = 0; i < LENGTH; i++)
				if (mpk[i].isSelected())
					((PianoTrack)tracks[i]).setArp(Arp.Off);
		}
		void captureOff() {
			for (int i = 0; i < LENGTH; i++)
				if (capture[i].isSelected()) {
					tracks[i].setCapture(false);
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
		int index(MidiOut t) {
			for (int i = 0; i < LENGTH; i++)
				if (t == tracks[i])
					return i;
			return -1;
		}

	} // end RecMpk



}