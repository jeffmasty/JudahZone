package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.Component;
import java.awt.Dimension;
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
import net.judah.fx.LFO.Target;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.RowLabels;
import net.judah.gui.fx.TimePanel;
import net.judah.gui.settable.LfoCombo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.FxKnob;
import net.judah.gui.widgets.GuitarTuner;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.KeyPair;

public class LFOKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.LFO;
	@Getter private final LFO lfo;
    @Getter private final Compressor comp;
    
    @Getter private final Channel channel;
    private final JPanel lfoPanel = new JPanel(new GridLayout(2, 4));
    private final JPanel compressor = new JPanel(new GridLayout(2, 4));
    private final LfoCombo lfoCombo;
    @Getter private final GuitarTuner tuner = new GuitarTuner();
    private final JToggleButton tunerBtn = new JToggleButton("Tuner");
    private final ArrayList<Row> labels = new ArrayList<>();
    private final Row row;

    public LFOKnobs(final Channel ch) {
    	this.channel = ch;
    	this.comp = channel.getCompression();
    	this.lfo = channel.getLfo();

    	lfoCombo = new LfoCombo(channel);
    	LFO lfoFx = channel.getLfo();
    	
    	Row lfoLbl = new Row(ch);
    	ArrayList<Component> components = lfoLbl.getControls();
    	components.add(new FxTrigger(" ", lfoFx, ch));  // blank
    	components.add(new FxTrigger("LFO", lfoFx, ch));
    	components.add(new FxTrigger("Target", channel.getLfo(), ch));
    	components.add(new TimePanel(lfoFx));
    			
    	KeyPair blankComp = new KeyPair(" ", channel.getCompression());
    	RowLabels compLbl = new RowLabels(channel, blankComp, new KeyPair("Compressor", channel.getCompression()), blankComp, blankComp);
    	
    	row = new Row(channel);
    	ArrayList<Component> knobs = row.getControls();
    	
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Min.ordinal(), "Min"));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Max.ordinal(), "Max"));
    	knobs.add(lfoCombo);
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.MSec.ordinal(), "Time"));

    	for (Component c : lfoLbl.getControls())
			lfoPanel.add(c);
    	for (int i = 0; i < 4; i++) 
    		lfoPanel.add(knobs.get(i));
    	
    	labels.add(lfoLbl);
    	labels.add(compLbl);
    	
		knobs.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold"));
		knobs.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain"));
    	knobs.add(new FxKnob(channel, comp, Ratio.ordinal(), "Ratio"));
		knobs.add(new FxKnob(channel, comp, Release.ordinal(), "A/R"));
		for (Component c : compLbl.getControls())
			compressor.add(c);
		for (int i = 4; i < knobs.size(); i++)
			compressor.add(knobs.get(i));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		add(lfoPanel);
		add(compressor);
		add(Box.createVerticalGlue());
    	JPanel tunerPnl = new JPanel();
		tunerBtn.setSelected(false);
		tunerBtn.addChangeListener(e -> {
			tuner.setChannel(tunerBtn.isSelected() ? channel : null);
		});
		tunerPnl.add(tunerBtn);
		tunerPnl.add(tuner);
    	
    	add(Gui.resize(tunerPnl, new Dimension(Size.WIDTH_KNOBS - 10, 150)));
    	add(Box.createVerticalGlue());
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
    		Target target = (Target)Constants.ratio(data2, Target.values());
    		if (lfo.isActive())
    			lfoCombo.midiShow(target);
    		else 
    			lfo.set(LFO.Settings.Target.ordinal(), target.ordinal());
    		break;

		case 4: // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
		case 5:
			comp.set(Boost.ordinal(), data2);
			comp.setActive(data2 > 0);
			break;
    	case 6: 
			comp.set(Ratio.ordinal(), data2);
			comp.setActive(data2 > 5);
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
	public void pad1() {
		tunerBtn.doClick();
	}
	
	@Override
	public void pad2() {
		lfo.setActive(!lfo.isActive());
	}
	
	@Override
	public final void update() {
		for (Row lbl : labels) 
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
