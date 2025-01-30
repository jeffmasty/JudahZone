package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.controllers.MPKmini;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Click;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahMidi;
import net.judah.omni.Threads;
import net.judah.sampler.Sampler;
import net.judah.seq.Seq;
import net.judah.seq.arp.Arp;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlists;
import net.judah.song.setlist.SetlistsCombo;
import net.judah.synth.taco.TacoSynth;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.Constants;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel implements Cmdr {
	public static final Dimension COMBO_SIZE = new Dimension(125, 28);

	private final JudahMidi midi;
	private final Sampler sampler;
	private final Setlists setlists;
	private final TacoTruck truck;

	@Getter private final KnobMode knobMode = KnobMode.MIDI;
	@Getter private final SongCombo songsCombo = new SongCombo();
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob(Pastels.MY_GRAY);
	private final JButton stepPlay = new JButton("▶");
    private final JToggleButton zoneBtn = new JToggleButton("Zone");
    private final JToggleButton fluidBtn = new JToggleButton("Fluid");
	private final Btn tape = new Btn(" ⏺️ ", e->JudahZone.getMains().tape());
	private final JComboBox<ZoneMidi> jamOut;
 	private final Program tk0, tk1, tk2, f0, f1, f2, f3;

	public MidiGui(JudahMidi midi, Sampler sampler, TacoTruck synths, Seq seq, Setlists setlists) {

		this.midi = midi;
		this.sampler = sampler;
		this.setlists = setlists;
		this.truck = synths;
		tape.setOpaque(true);
    	title.add(new JLabel(" Song  "));
    	title.add(Gui.resize(songsCombo, new Dimension(150, 28)));
    	title.add(tape);

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
		stepPlay.addActionListener(e-> sampler.setStepping(!sampler.isStepping()));
		stepPlay.setOpaque(true);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(new JLabel("  Setlist "));
		top.add(Gui.resize(new SetlistsCombo(setlists), Size.COMBO_SIZE));
		top.add(Gui.resize(stepper, COMBO_SIZE));
		top.add(stepVol);
		top.add(stepPlay);

		JComponent labels = Box.createHorizontalBox();//new JPanel(new GridLayout(1, 2));
		fluidBtn.addActionListener(e->update());
		zoneBtn.addActionListener(e->update());
		ButtonGroup which = new ButtonGroup();
		which.add(zoneBtn);
		which.add(fluidBtn);
		zoneBtn.setSelected(true);
		labels.add(zoneBtn);
		labels.add(fluidBtn);

		jamOut = new JComboBox<ZoneMidi>(truck.mpkRoutes);
		jamOut.addActionListener(e->MPKmini.instance.setMidiOut((ZoneMidi)jamOut.getSelectedItem()));
		jamOut.setSelectedItem(truck.mpkRoutes[0]);
		tk0 = new Program(truck.mpkRoutes[0]);
		tk1 = new Program(truck.tracks.get(0));
		tk2 = new Program(truck.tracks.get(1));
		f0 = new Program(truck.mpkRoutes[1]);
		while (truck.fluid.getTracks().size() < 3)
			Threads.sleep(10);
		f1 = new Program(truck.fluid.getTracks().get(0));
		f2 = new Program(truck.fluid.getTracks().get(1));
		f3 = new Program(truck.fluid.getTracks().get(2));


		GridLayout grid = new GridLayout(4, 1, 1, 4);
		JPanel tLbls = new JPanel(grid);
		JPanel fLbls = new JPanel(grid);
		JPanel tacos = new JPanel(grid);
		JPanel fluids = new JPanel(grid);
		tLbls.add(new Octaver());
		tacos.add(jamOut);
		tLbls.add(new Click("Taco", e->midi.getJamstik().octaver()));
		tacos.add(tk0);
		tacos.add(tk1);
		tacos.add(tk2);
		fLbls.add(new Click("Fluid", e->MPKmini.override = !MPKmini.override));
		fluids.add(f0);
		fluids.add(f1);
		fluids.add(f2);
		fluids.add(f3);
		for (TacoSynth s : truck.tracks)
			tLbls.add(new ClickIt(s.getTracks().getFirst()));
		for (PianoTrack t : truck.fluid.getTracks())
			fLbls.add(new ClickIt(t));

		Box bottom = Box.createHorizontalBox();
		bottom.add(tLbls); bottom.add(tacos); bottom.add(fLbls); bottom.add(fluids);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	setOpaque(true);
        add(top);
		add(labels);
        add(bottom); // TODO better wrap layout
        update();
	}

	private String patch(ZoneMidi synth, int data2) {
		return (String) Constants.ratio(data2, synth.getPatches());
	}

	/**@param idx knob 0 to 7
     * @param data2  user input */
	@Override public boolean doKnob(int idx, int data2) {

    	switch(idx) {
    	case 0: // sync loop length
			if (data2 == 0)
				midi.getClock().setLength(1);
			else
				midi.getClock().setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
			break;
    	case 1: // Select song
 	    	songsCombo.midiShow((File)Constants.ratio(data2 - 1, setlists.getCurrent()));
    		break;
    	case 2: // Select StepSample
    		sampler.setSelected(Constants.ratio(data2, sampler.getStepSamples().size()));
    		break;
    	case 3: // Sampler volume
    		sampler.setStepMix(data2 * 0.01f);
    		break;
 	    case 4:
 	    	if (zoneBtn.isSelected()) // MPK keys out
 	    		jamOut.setSelectedItem(Constants.ratio(data2 - 1, truck.mpkRoutes));
 	    	else
 	    		f0.midiShow(patch(truck.fluid, data2));
    		break;
    	case 5:
    		if (zoneBtn.isSelected())
    			tk0.midiShow(patch(truck.taco, data2));
    		else
    			f1.midiShow(patch(truck.fluid, data2));
    		break;
    	case 6:
    		if (zoneBtn.isSelected())
    			tk1.midiShow(patch(truck.taco, data2));
    		else
    			f2.midiShow(patch(truck.fluid, data2));

    		break;
    	case 7:
    		if (zoneBtn.isSelected())
    			tk2.midiShow(patch(tk1.getMidiOut(), data2));
    		else
    			f3.midiShow(patch(truck.fluid, data2));
    		break;
    	default: return false;
    	}
    	return true;
	}

	@Override public void pad1() {
		if (zoneBtn.isSelected())
			fluidBtn.setSelected(true);
		else
			zoneBtn.setSelected(true);
	}

	@Override public void pad2() {
		JudahZone.getMains().hotMic();
	}

	@Override public void update() {
		if (stepVol.getValue() != (int) (sampler.getStepMix() * 100))
			stepVol.setValue((int) (sampler.getStepMix() * 100));
		stepper.setSelectedIndex(sampler.getSelected());
		stepPlay.setBackground(sampler.isStepping() ? Pastels.GREEN : null);
	}

	public void updateTape() {
		tape.setBackground(JudahZone.getMains().getTape() == null ? null : Pastels.RED);
	}

	@Override
	public ZoneMidi resolve(String key) {
		for (ZoneMidi zone : truck.mpkRoutes)
			if (zone.getName().equals(key))
				return zone;
		return null;
	}

	@Override
	public void execute(Param p) {
		if (p.cmd == Cmd.MPK)
			MPKmini.instance.setCaptureTrack((PianoTrack)resolve(p.val));
	}

	@Override
	public String[] getKeys() {
		String[] result = new String[truck.mpkRoutes.length];
		for (int i = 0; i < result.length; i++)
			result[i] = truck.mpkRoutes[i].getName();
		return result;
	}

	private class Octaver extends Click implements ActionListener {
		Octaver() {
			super("JAM");
			addActionListener(this);
		}
		@Override public void actionPerformed(ActionEvent e) {
			if (right)
				midi.getJamstik().octaver();
			else
				midi.getJamstik().toggle();
		}
	}
	private class ClickIt extends Click implements ActionListener {
		final PianoTrack t;
		ClickIt(PianoTrack target) {
			super(target.getName());
			t = target;
			addActionListener(this);
		}
		@Override public void actionPerformed(ActionEvent e) {
			if (right)
				t.setCapture(!t.isCapture());
			else
				t.toggle(Arp.MPK);
		}
	}
}
