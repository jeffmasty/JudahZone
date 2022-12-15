package net.judah.gui.knobs;
import static net.judah.JudahZone.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.SmashHit;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KnobMode;
import net.judah.fluid.FluidSynth;
import net.judah.gui.Pastels;
import net.judah.midi.*;
import net.judah.samples.Sampler;
import net.judah.synth.JudahSynth;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.widgets.Knob;
import net.judah.widgets.LengthWidget;
import net.judah.widgets.SettableCombo;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends KnobPanel {
	public static final Dimension COMBO_SIZE = new Dimension(110, 28);
	public static final int FLUIDS = 4;
	
   	private final JudahMidi midi;
	private final JudahClock clock;
	private final Sampler sampler;
	private final FluidSynth fluid;

	private final Jamstik jamstik;
    private final LengthWidget sync;
    private final JToggleButton zoneBtn = new JToggleButton("Zone");
    private final JToggleButton fluidBtn = new JToggleButton("Fluid");

    @Getter private final ProgChange one;
    @Getter private final ProgChange two;
    @Getter private final SettableCombo<Class<? extends SmashHit>> setlistCombo = new SettableCombo<>(()->loadSong());
	@Getter private final JComboBox<MidiPort> mpk = new JComboBox<>();
	private final JPanel mpkPanel = new JPanel();
	private final JPanel titleBar = new JPanel();
	private final ProgChange[] channels = new ProgChange[FLUIDS];
	private final JComboBox<String> stepper = new JComboBox<>();
	private final Knob stepVol = new Knob(Pastels.MY_GRAY);
	private final JButton stepPlay = new JButton("▶");
	
	public MidiGui(JudahMidi midi, JudahClock clock, Jamstik jam, Sampler sampler,
			JudahSynth a, JudahSynth b, FluidSynth fsynth) {
		super(KnobMode.Midi.name());
    	this.midi = midi;
		this.clock = clock;
		this.sampler = sampler;
		this.fluid = fsynth;
		this.jamstik = jam;
    	jamstik.setFrame(this);
    	
    	doSetlist(titleBar);
    	setOpaque(true);
    	one = new ProgChange(a, 0);
		two = new ProgChange(b, 0);
		for (MidiPort p : JudahZone.getSynthPorts()) 
			mpk.addItem(p);
		mpk.setRenderer(STYLE);
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener(e-> midi.setKeyboardSynth((MidiPort)mpk.getSelectedItem() ));

		sampler.getStepSamples().forEach(s->stepper.addItem(s.getName()));
		stepper.setSelectedIndex(sampler.getSelected());
		stepper.addActionListener(e->{
			if (sampler.getSelected() != stepper.getSelectedIndex())
				sampler.setSelected(stepper.getSelectedIndex());});
		stepVol.setValue((int) (sampler.getVelocity() * 100));
		stepVol.addListener(val->sampler.setVelocity(val * 0.01f));
		stepPlay.addActionListener(e-> sampler.setStepping(!sampler.isStepping()));
		stepPlay.setOpaque(true);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JPanel top = new JPanel();
		sync = new LengthWidget(clock);
		top.add(sync);
		top.add(new JLabel("Bars", SwingConstants.CENTER));
		top.add(new JComboBox<Signature>(Signature.values()));
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
        add(Constants.wrap(bottom));
    }
	
	private JPanel internals() { // Synth1, Synth2, Jamstik, MPK
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(4, 1));
		JPanel row = new JPanel();
		row.add(new JLabel(getSynth1().getName()));
		row.add(Constants.resize(one, COMBO_SIZE));
		result.add(row);
		row = new JPanel();
		row.add(new JLabel(getSynth2().getName()));
		row.add(Constants.resize(two, COMBO_SIZE));
		result.add(row);
		
		row = new JPanel();
		row.add(new JLabel("Jam"));
		row.add(Constants.resize(jamstik, COMBO_SIZE));
		result.add(row);
		mpkPanel.add(new JLabel("MPK"));
        mpkPanel.add(Constants.resize(mpk, COMBO_SIZE));
		result.add(mpkPanel);

		return result;
	}
	
	private JPanel fluids(FluidSynth fluid) {
		JPanel result = new JPanel();
		for (int i = 0; i < FLUIDS; i++) {
			channels[i] = new ProgChange(fluid, i);
			channels[i].setPreferredSize(COMBO_SIZE);
		}
		result.setLayout(new GridLayout(4, 1));
		for (int i = 0; i < FLUIDS; i++) {
			JPanel row = new JPanel();
			row.add(new JLabel("CH " + i));
			row.add(channels[i]);
			result.add(row);
		}
		return result;
	}
	
	@Override
	public JPanel installing() {
		return titleBar;
	}

	private void doSetlist(JPanel pnl) {
		setlistCombo.setRenderer(new BasicComboBoxRenderer() {
        	@Override public Component getListCellRendererComponent(
        			@SuppressWarnings("rawtypes") JList list, Object value,
        			int index, boolean isSelected, boolean cellHasFocus) {
        		setHorizontalAlignment(SwingConstants.CENTER);
        		@SuppressWarnings("unchecked")
				Class<? extends SmashHit> item = (Class<? extends SmashHit>) value;
        		setText(item == null ? "?" : item.getSimpleName());
        		return this;
    }});
		
		for (Class<? extends SmashHit> c : getSetlist())
			setlistCombo.addItem(c);
		pnl.add(setlistCombo);
        JButton load = new JButton("Load");
        load.addActionListener(e->loadSong());
        pnl.add(load);
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
 	    	setlistCombo.setSelectedIndex(Constants.ratio(data2 - 1, setlistCombo.getItemCount()));
    		break;
    	case 2: // Select StepSample
    		sampler.setSelected(Constants.ratio(data2, sampler.getStepSamples().size()));
    		break;
    	case 3: // Sampler volume
    		sampler.setVelocity(data2 * 0.01f);
    		break;
 	    case 4:
 	    	if (zoneBtn.isSelected()) 
 	    		one.setSelectedIndex(Constants.ratio(data2 - 1, one.getItemCount()));
 	    	else 
 	    		channels[idx - 4].setSelectedIndex(data2);
 	    	break;
    	case 5: // change sequencer track focus
 	    	if (zoneBtn.isSelected()) 
 	    		two.setSelectedIndex(Constants.ratio(data2 - 1, two.getItemCount()));
 	    	else 
 	    		channels[idx - 4].setSelectedIndex(data2);
    		break;
    	case 6: // Jamstik out
    		if (zoneBtn.isSelected()) 
    			jamstik.setSelectedIndex(Constants.ratio(data2 - 1, midi.getPaths().size()));
 	    	else 
 	    		channels[idx - 4].setSelectedIndex(data2);
    		break;
    	case 7: // MPK keys out
    		if (zoneBtn.isSelected()) 
    			mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
 	    	else 
 	    		channels[idx - 4].setSelectedIndex(data2);
    		break;
    	default: return false;
    	}   
    	return true;
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
        		MidiPort item = (MidiPort) value;
        		setText(item == null ? "?" : item.toString());
        		return this;
    }};

	@Override
	public void update() {
		if (stepVol.getValue() != (int) (sampler.getVelocity() * 100))
			stepVol.setValue((int) (sampler.getVelocity() * 100));
		sync.setSelectedItem(clock.getLength());
		stepper.setSelectedIndex(sampler.getSelected());
		stepPlay.setBackground(sampler.isStepping() ? Pastels.GREEN : null);
		if (one.getSelectedIndex() != getSynth1().getProg(0))
		for (int i = 0; i < FLUIDS; i++)
			if (channels[i].getSelectedIndex() != fluid.getProg(i)) {
				channels[i].setSelectedIndex(i);
			}
	}
	
}
