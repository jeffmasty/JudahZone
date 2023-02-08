package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Compressor;
import net.judah.fx.LFO;
import net.judah.gui.Gui;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.RowLabels;
import net.judah.gui.settable.Lfo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.GuitarTuner;
import net.judah.mixer.Channel;
import net.judah.util.KeyPair;

public class LFOKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.LFO;
	@Getter private final LFO lfo;
    @Getter private final Compressor comp;
    
    @Getter private final Channel channel;
    private final JPanel lfoPanel = new JPanel(new GridLayout(2, 4));
    private final JPanel compressor = new JPanel(new GridLayout(2, 4));
    @Getter private final GuitarTuner tuner = new GuitarTuner();
    private final JToggleButton tunerBtn = new JToggleButton("Tuner");
    private final ArrayList<RowLabels> labels = new ArrayList<>();
    private final Row row;

    public LFOKnobs(final Channel ch) {
    	super("extras");
    	this.channel = ch;
    	this.comp = channel.getCompression();
    	this.lfo = channel.getLfo();

    	KeyPair blankLfo = new KeyPair(" ", channel.getLfo());
    	KeyPair blankComp = new KeyPair(" ", channel.getCompression());
    	RowLabels lfoLbl = new RowLabels(channel, blankLfo, new KeyPair("LFO", channel.getLfo()), blankLfo, 
    			new KeyPair("Target", channel.getLfo()));
    	RowLabels compLbl = new RowLabels(channel, blankComp, new KeyPair("Compressor", channel.getCompression()), blankComp, blankComp);
    	
    	row = new Row(channel);
    	ArrayList<Component> knobs = row.getControls();
    	
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Min.ordinal(), "Min"));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Max.ordinal(), "Max"));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.MSec.ordinal(), "Time"));
    	knobs.add(new Lfo(channel));

    	for (Component c : lfoLbl.getControls())
			lfoPanel.add(c);
    	for (int i = 0; i < 4; i++) 
    		lfoPanel.add(knobs.get(i));
    	
    	labels.add(lfoLbl);
    	labels.add(compLbl);
    	
    	knobs.add(new FxKnob(channel, comp, Ratio.ordinal(), "Ratio"));
		knobs.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold"));
		knobs.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain"));
		knobs.add(new FxKnob(channel, comp, Release.ordinal(), "A/R"));
		for (Component c : compLbl.getControls())
			compressor.add(c);
		for (int i = 4; i < knobs.size(); i++)
			compressor.add(knobs.get(i));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		add(lfoPanel);
		add(compressor);
    	JPanel tunerPnl = new JPanel();
		tunerBtn.setSelected(false);
		tunerBtn.addChangeListener(e -> {
			tuner.setChannel(tunerBtn.isSelected() ? channel : null);
		});
		tunerPnl.add(tunerBtn);
		tunerPnl.add(tuner);
    	
    	add(tunerPnl);
    	add(Box.createVerticalGlue());
    	doLayout();
    	update();
    	
    }
    
    @Override
    public Component installing() {
    	return Gui.wrap(JudahZone.getMixer().getCombo(channel), new FxButton(channel));
    }

	@Override
	public boolean doKnob(int idx, int data2) {
    	switch(idx) {
    	case 0: 
    		lfo.set(LFO.Settings.Min.ordinal(), data2);
    		break;
    	case 1: 
    		lfo.set(LFO.Settings.Max.ordinal(), data2);
    		break;
    	case 2: 
    		lfo.set(LFO.Settings.MSec.ordinal(), data2);
    		break;
    	case 3:
    		lfo.set(LFO.Settings.Target.ordinal(), data2);
    		break;

    	case 4: 
			comp.set(Ratio.ordinal(), data2);
			comp.setActive(data2 > 5);
			break;
		case 5: // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
		case 6:
			comp.set(Boost.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
    	case 7:
			comp.set(Release.ordinal(), data2);
			comp.set(Attack.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
    	default: 
    		return false;
    	}
		return true;
	}

	@Override
	public final void update() {
		for (RowLabels lbl : labels) 
        	lbl.update();
		row.update();
		tunerBtn.setSelected(GuitarTuner.getChannel() == channel);

	}

}

// TODO if (channel instanceof Loop) main.addMouseListener(new WavTools((Loop)channel));
// TODO crave clock sync 
//	else if (channel instanceof MidiInstrument && 
//	midi.getSync().contains(((MidiInstrument)channel).getMidiPort()))	bg = PINK;
//			if (midi.getPath(in) == null) 
//				sync.setEnabled(false);
