package net.judah.gui.knobs;
import static net.judah.JudahZone.*;
import static net.judah.fluid.FluidSynth.CHANNELS;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import net.judah.controllers.Jamstik;
import net.judah.drumkit.Sampler;
import net.judah.fluid.FluidSynth;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.Songs;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.LengthWidget;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Signature;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel {
	public static final Dimension COMBO_SIZE = new Dimension(110, 28);
	
	@Getter private final Songs songsCombo = new Songs();
	private final JudahClock clock;
	private final JudahMidi midi;
	private final Sampler sampler;
	private final FluidSynth fluid;
    private final LengthWidget sync;
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
	private final JPanel titleBar = new JPanel();
	private final JComboBox<Signature> sig = new JComboBox<Signature>(Signature.values());
	
	public MidiGui(JudahMidi midi, JudahClock clock, Jamstik jam, Sampler sampler,
			JudahSynth a, JudahSynth b, FluidSynth fsynth, Seq seq) {
		super(KnobMode.Midi.name());
		this.clock = clock;
		this.sampler = sampler;
		this.fluid = fsynth;
		this.jamstik = jam;
		this.midi = midi;
    	jamstik.setFrame(this);
    	
    	titleBar.add(new JLabel("Song"));
    	titleBar.add(songsCombo);
    	titleBar.add(new Btn("Load", e->loadSong((File)songsCombo.getSelectedItem())));
    	
    	setOpaque(true);
    	one = new Program(a);
    	two = new Program(b);
    	seq.getSynthTracks().forEach(track->mpk.addItem(track));
		mpk.setRenderer(STYLE);
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener(e-> {
			if (midi.getKeyboardSynth() != (MidiTrack)mpk.getSelectedItem())
				midi.setKeyboardSynth((MidiTrack)mpk.getSelectedItem());});

		sampler.getStepSamples().forEach(s->stepper.addItem(s.getName()));
		stepper.setSelectedIndex(sampler.getSelected());
		stepper.addActionListener(e->{
			if (sampler.getSelected() != stepper.getSelectedIndex())
				sampler.setSelected(stepper.getSelectedIndex());});
		stepVol.setValue((int) (sampler.getMix() * 100));
		stepVol.addListener(val->sampler.setMix(val * 0.01f));
		stepPlay.addActionListener(e-> sampler.setStepping(!sampler.isStepping()));
		stepPlay.setOpaque(true);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JPanel top = new JPanel();
		sync = new LengthWidget(clock);
		sig.addActionListener(e->clock.setTimeSig((Signature)sig.getSelectedItem()));
		
		top.add(new JLabel("Bars"));
		top.add(sync);
		top.add(sig);
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
		labels.add(zoneBtn);
		labels.add(fluidBtn);
		
			
		JPanel bottom = new JPanel(new GridLayout(1, 2));
		bottom.add(internals());
		bottom.add(fluids(fsynth));
		
        update();
        
        add(top);
		add(labels);
        add(Gui.wrap(bottom));
    }
	
	private JPanel internals() { // Synth1, Synth2, Jamstik, MPK
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(4, 1));
		JPanel row = new JPanel();
		row.add(new JLabel(getSynth1().getName()));
		row.add(Gui.resize(one, COMBO_SIZE));
		result.add(row);
		row = new JPanel();
		row.add(new JLabel(getSynth2().getName()));
		row.add(Gui.resize(two, COMBO_SIZE));
		result.add(row);
		
		row = new JPanel();
		row.add(new JLabel("Jam"));
		row.add(Gui.resize(jamstik, COMBO_SIZE));
		result.add(row);
		mpkPanel.add(new JLabel("MPK"));
        mpkPanel.add(Gui.resize(mpk, COMBO_SIZE));
		result.add(mpkPanel);

		return result;
	}
	
	private JPanel fluids(FluidSynth fluid) {
		JPanel result = new JPanel();
		try {
			fluid.progChange("Dulcimer", 1);
			fluid.progChange("Celesta", 2);
			fluid.syncChannels();
			for (int i = 0; i < CHANNELS; i++) {
				fluids[i] = new Program(fluid, i);
				fluids[i].setPreferredSize(COMBO_SIZE);
			}
			result.setLayout(new GridLayout(4, 1));
			for (int i = 0; i < CHANNELS; i++) {
				JPanel row = new JPanel();
				row.add(new JLabel("CH " + i));
				row.add(fluids[i]);
				result.add(row);
			}
		} catch (Exception e) { RTLogger.warn(this, e); }
		return result;
	}
	
	@Override
	public JPanel installing() {
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
				clock.setLength((int) Constants.ratio(data2, LengthWidget.LENGTHS));
			break;
    	case 1: // Select song
 	    	songsCombo.midiShow((File)Constants.ratio(data2 - 1, Folders.getSetlist().listFiles()));
    		break;
    	case 2: // Select StepSample
    		sampler.setSelected(Constants.ratio(data2, sampler.getStepSamples().size()));
    		break;
    	case 3: // Sampler volume
    		sampler.setMix(data2 * 0.01f);
    		break;
 	    case 4:
 	    	if (zoneBtn.isSelected()) 
 	    		one.midiShow(patch(one.getPort().getPatches(), data2));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluids[idx-4].getPrograms(), data2));
 	    	break;
    	case 5: // change sequencer track focus
 	    	if (zoneBtn.isSelected()) 
 	    		two.midiShow(patch(two.getPrograms(), data2));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluids[idx-4].getPrograms(), data2));
    		break;
    	case 6: // Jamstik out
    		if (zoneBtn.isSelected()) 
    			jamstik.setSelectedIndex(Constants.ratio(data2 - 1, getSynths().size()));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluids[idx-4].getPrograms(), data2));
    		break;
    	case 7: // MPK keys out
    		if (zoneBtn.isSelected()) 
    			mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
 	    	else 
 	    		fluids[idx - 4].midiShow(patch(fluids[idx-4].getPrograms(), data2));
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
	
	public void length(int bars) {
		sync.setSelectedItem(bars);
	}
	
	public void record(boolean active) {
		mpkPanel.setBackground(active ? Pastels.RED : Pastels.BUTTONS);
	}
	
	public void transpose(boolean active) {
		mpkPanel.setBackground(active ? Pastels.PINK: Pastels.BUTTONS);
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

	@Override
	public void update() {
		if (stepVol.getValue() != (int) (sampler.getMix() * 100))
			stepVol.setValue((int) (sampler.getMix() * 100));
		sync.setSelectedItem(clock.getLength());
		stepper.setSelectedIndex(sampler.getSelected());
		stepPlay.setBackground(sampler.isStepping() ? Pastels.GREEN : null);
		if (clock.getTimeSig() != sig.getSelectedItem())
			sig.setSelectedItem(clock.getTimeSig());
		if (midi.getKeyboardSynth() != (MidiTrack)mpk.getSelectedItem())
			mpk.setSelectedItem(midi.getKeyboardSynth());	
	}
	
}
