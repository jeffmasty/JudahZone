package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.controllers.Jamstik;
import net.judah.drumkit.Sampler;
import net.judah.fluid.FluidCommand;
import net.judah.fluid.FluidSynth;
import net.judah.fluid.FluidSynth.Drum;
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
import net.judah.midi.JudahMidi;
import net.judah.seq.Seq;
import net.judah.seq.arp.Mode;
import net.judah.seq.track.MidiTrack;
import net.judah.song.setlist.Setlists;
import net.judah.song.setlist.SetlistsCombo;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel {
	public static final Dimension COMBO_SIZE = new Dimension(113, 28);
	public static final int CHANNELS = 3;
	
	@Getter private final KnobMode knobMode = KnobMode.Midi;
	@Getter private final SongCombo songsCombo = new SongCombo();
	private final JudahClock clock;
	private final JudahMidi midi;
	private final Seq seq;
	private final Sampler sampler;
	private final FluidSynth fluid;
	private final JudahSynth synth1, synth2;
	private final Setlists setlists;
    private final Program one;
    private final Program two;
	private final Program[] fluids = new Program[CHANNELS];
    private final JComboBox<MidiTrack> mpk = new JComboBox<>();
	private final Jamstik jamstik;
	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob(Pastels.MY_GRAY);
	private final JButton stepPlay = new JButton("▶");
    private final JToggleButton zoneBtn = new JToggleButton("Zone");
    private final JToggleButton fluidBtn = new JToggleButton("Fluid");
	private final JPanel mpkPanel = new JPanel();
	private final JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final Btn tape = new Btn(" ⏺️ ", e->JudahZone.getMains().tape());

	public MidiGui(JudahMidi midi, JudahClock clock, Jamstik jam, Sampler sampler,
			JudahSynth a, JudahSynth b, FluidSynth fsynth, Seq seq, Setlists setlists) {
		this.clock = clock;
		this.seq = seq;
		this.sampler = sampler;
		this.fluid = fsynth;
		this.synth1 = a;
		this.synth2 = b;
		this.jamstik = jam;
		this.midi = midi;
		this.setlists = setlists;
    	jamstik.setFrame(this);
    	
    	titleBar.add(new JLabel(" Song  "));
    	titleBar.add(Gui.resize(songsCombo, new Dimension(150, 28)));
    	tape.setOpaque(true);
    	titleBar.add(tape);
    	
    	one = new Program(synth1, synth1.getChannel());
    	two = new Program(synth2, synth2.getChannel());
    	seq.getSynthTracks().forEach(track->mpk.addItem(track));
		mpk.setRenderer(STYLE);
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener(e-> {
			if (midi.getKeyboardSynth() != (MidiTrack)mpk.getSelectedItem())
				midi.setKeyboardSynth((MidiTrack)mpk.getSelectedItem());});

		sampler.getStepSamples().forEach(s->stepper.addItem(s.getName()));
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
		top.add(stepper);
		top.add(stepVol);
		top.add(stepPlay);
		
		JPanel labels = new JPanel(new GridLayout(1, 2));
		fluidBtn.addActionListener(e->{
			try { 
				fluid.syncChannels();
				update();
			} catch (Exception err) { RTLogger.warn(this, err); }});
		
		ButtonGroup which = new ButtonGroup();
		zoneBtn.addActionListener(e->update());
		which.add(zoneBtn);
		which.add(fluidBtn);
		zoneBtn.setSelected(true);
		labels.add(fluidBtn);
		labels.add(zoneBtn);
		
		JPanel bottom = new JPanel(new GridLayout(1, 2));
		bottom.add(fluids());
		bottom.add(internals());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	setOpaque(true);
        add(top);
		add(labels);
        add(Gui.wrap(bottom));
        update();
	}
	
	private JPanel internals() { // Synth1, Synth2, Jamstik, MPK
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(4, 1));
		JPanel row = new JPanel();
		
		row.add(new Click(synth1.getName(), e-> {
			MainFrame.setFocus(KnobMode.DCO);	
			midi.setKeyboardSynth(seq.lookup(synth1, synth1.getChannel())); }));
		row.add(Gui.resize(one, COMBO_SIZE));
		result.add(row);

		row = new JPanel();
		row.add(new Click(synth2.getName(), e-> {
			MainFrame.setFocus(synth2.getSynthKnobs());
			midi.setKeyboardSynth(seq.lookup(synth2, synth2.getChannel())); }));
		row.add(Gui.resize(two, COMBO_SIZE));
		result.add(row);
		
		row = new JPanel();
		row.add(new Click("JAM", e->jamstik.toggle()));
		row.add(Gui.resize(jamstik, COMBO_SIZE));
		result.add(row);
		
		mpkPanel.add(new Click("  MPK  ", e->midi.getKeyboardSynth().getArp().toggle(
				((Click)e.getSource()).isRight() ? Mode.REC : Mode.MPK)));
        mpkPanel.add(Gui.resize(mpk, COMBO_SIZE));
		result.add(mpkPanel);
		return result;
	}
	
	private JPanel fluids() {
		JPanel result = new JPanel();
		try {
			for (int i = 0; i < CHANNELS; i++) {
				fluids[i] = new Program(fluid, i);
				Gui.resize(fluids[i], COMBO_SIZE);
			}
			result.setLayout(new GridLayout(4, 1));
			for (int i = 0; i < CHANNELS; i++) {
				JPanel row = new JPanel();
				row.add(new JLabel("CH " + (i + 1)));
				row.add(fluids[i]);
				result.add(row);
			}
			JComboBox<String> drums = new JComboBox<>(); // TODO broken
			for (Drum d : fluid.getDrums())
				drums.addItem(d.name);
			Gui.resize(drums, COMBO_SIZE);
			drums.addActionListener(e->{
				Integer prog = fluid.getDrums().get(drums.getSelectedIndex()).prog;
				fluid.sendCommand(FluidCommand.PROG_CHANGE, 9 + " " + prog);
			});
			JPanel row = new JPanel();
			row.add(new JLabel("Drum"));
			row.add(drums);
			result.add(row);
			
		} catch (Exception e) { RTLogger.warn(this, e); }
		return result;
	}
	
	@Override public JPanel installing() {
		return titleBar;
	}

	/**@param idx knob 0 to 7
     * @param data2  user input */
	@Override public boolean doKnob(int idx, int data2) {
		// seq.setCurrent(seq.get(Constants.ratio(data2 - 1, seq.getTotal())));
    	switch(idx) {
    	case 0: // sync loop length 
			if (data2 == 0) 
				clock.setLength(1);
			else 
				clock.setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
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
 	    	if (zoneBtn.isSelected()) 
 	    		one.midiShow(patch(one.getPort().getPatches(), data2));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluid.getPatches(), data2));
 	    	break;
    	case 5: // change sequencer track focus
 	    	if (zoneBtn.isSelected()) 
 	    		two.midiShow(patch(two.getPort().getPatches(), data2));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluid.getPatches(), data2));
    		break;
    	case 6: // Jamstik out
    		if (zoneBtn.isSelected()) 
    			jamstik.setSelectedIndex(Constants.ratio(data2 - 1, jamstik.getItemCount()));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluid.getPatches(), data2));
    		break;
    	case 7: // MPK keys out
    		if (zoneBtn.isSelected()) 
    			mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluid.getPatches(), data2));
    		break;
    	default: return false;
    	}   
    	return true;
	}
	
	private String patch(String[] patches, int data2) {
		return (String) Constants.ratio(data2, patches);
	}
	
	@Override
	public void pad1() {
		if (zoneBtn.isSelected())
			fluidBtn.setSelected(true);
		else 
			zoneBtn.setSelected(true);
	}
	
	static final BasicComboBoxRenderer STYLE = new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		setHorizontalAlignment(SwingConstants.CENTER);
        		MidiTrack item = (MidiTrack) value;
        		setText(item == null ? "?" : item.toString());
        		return this;
    }};
    
    public void updateSynth() {
    	Mode m = midi.getKeyboardSynth().getArp().getMode();
    	if (m == Mode.MPK || m == Mode.REC) 
    		mpkPanel.setBackground(m.getColor());
    	else mpkPanel.setBackground(null);
    }

	@Override
	public void update() {
		if (stepVol.getValue() != (int) (sampler.getStepMix() * 100))
			stepVol.setValue((int) (sampler.getStepMix() * 100));
		stepper.setSelectedIndex(sampler.getSelected());
		stepPlay.setBackground(sampler.isStepping() ? Pastels.GREEN : null);
		if (midi.getKeyboardSynth() != (MidiTrack)mpk.getSelectedItem())
			mpk.setSelectedItem(midi.getKeyboardSynth());	
	}

	public void updateTape() {
		tape.setBackground(JudahZone.getMains().getTape() == null ? null : Pastels.RED);
	}
}
