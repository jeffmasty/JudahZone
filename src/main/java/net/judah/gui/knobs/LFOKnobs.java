package net.judah.gui.knobs;

import static net.judah.fx.Compressor.Settings.*;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.Compressor;
import net.judah.fx.EffectColor;
import net.judah.fx.LFO;
import net.judah.fx.LFO.Target;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.fx.Row;
import net.judah.gui.fx.RowLabels;
import net.judah.gui.fx.TimePanel;
import net.judah.gui.settable.LfoCombo;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.FxKnob;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.util.Constants;
import net.judah.util.KeyPair;

public class LFOKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.LFO;
    @Getter private final JPanel title;
	@Getter private final LFO lfo;
    @Getter private final Compressor comp;
    
    @Getter private final Channel channel;
    private final JPanel lfoLbls =  new JPanel(new GridLayout(1, 4, 0, 1));
    private final JPanel lfoPanel = new JPanel(new GridLayout(1, 4, 0, 0));
    private final JPanel compressor = new JPanel(new GridLayout(1, 4, 0, 0));
    private final JPanel compressorLbls = new JPanel(new GridLayout(1, 4, 0, 1));
    private final LfoCombo lfoCombo;
    private final ArrayList<Row> labels = new ArrayList<>();
    private final Row row;
    private final JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 9));
    
    public LFOKnobs(final Channel ch, DJJefe mixer) {
    	this.channel = ch;
    	this.comp = channel.getCompression();
    	this.lfo = channel.getLfo();
    	title = Gui.wrap(mixer.getCombo(channel), new FxButton(channel));
    	
    	lfoCombo = new LfoCombo(channel);
    	LFO lfoFx = channel.getLfo();
    	
    	Row lfoLbl = new Row(ch);
    	ArrayList<Component> components = lfoLbl.getControls();
    	components.add(new FxTrigger("Target", channel.getLfo(), ch));
    	components.add(new FxTrigger(" ", lfoFx, ch));  // blank
    	components.add(new FxTrigger(" ", lfoFx, ch));  // blank
    	components.add(new TimePanel(lfoFx));
    			
    	KeyPair blankComp = new KeyPair(" ", channel.getCompression());
    	RowLabels compLbl = new RowLabels(channel, new KeyPair("Compressor", channel.getCompression()), blankComp, blankComp, blankComp);
    	wrap.setOpaque(true);
    	wrap.add(lfoCombo);

    	row = new Row(channel);
    	ArrayList<Component> knobs = row.getControls();
    	knobs.add(wrap);
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Min.ordinal(), "Min", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.Max.ordinal(), "Max", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, lfo, LFO.Settings.MSec.ordinal(), "Time", Pastels.EGGSHELL));

    	for (Component c : lfoLbl.getControls())
			lfoLbls.add(c);
    	for (int i = 0; i < 4; i++) 
    		lfoPanel.add(knobs.get(i));
    	labels.add(lfoLbl);
    	labels.add(compLbl);
    	
		knobs.add(new FxKnob(channel, comp, Threshold.ordinal(), "THold", Pastels.EGGSHELL));
		knobs.add(new FxKnob(channel, comp, Boost.ordinal(), "Gain", Pastels.EGGSHELL));
    	knobs.add(new FxKnob(channel, comp, Ratio.ordinal(), "Ratio", Pastels.EGGSHELL));
		knobs.add(new FxKnob(channel, comp, Release.ordinal(), "A/R", Pastels.EGGSHELL));
		for (Component c : compLbl.getControls())
			compressorLbls.add(c);
		for (int i = 4; i < knobs.size(); i++)
			compressor.add(knobs.get(i));

		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(lfoLbls);
		add(lfoPanel);
		add(compressorLbls);
		add(compressor);
    	add(Box.createVerticalGlue());
		update();
		validate();
    }
    
	@Override
	public boolean doKnob(int idx, int data2) {
    	switch(idx) {
    	case 0:
    		Target target = (Target)Constants.ratio(data2, Target.values());
    		if (lfo.isActive())
    			lfoCombo.midiShow(target);
    		else 
    			lfo.set(LFO.Settings.Target.ordinal(), target.ordinal());
    		break;
    	case 1: 
    		lfo.set(LFO.Settings.Min.ordinal(), data2);
    		break;
    	case 2: 
    		lfo.set(LFO.Settings.Max.ordinal(), data2);
    		break;
    	case 3: 
    		lfo.set(LFO.Settings.MSec.ordinal(), data2);
    		break;

		case 4: // -30 to -1
			comp.set(Threshold.ordinal(), data2);
			compress(2, data2);
			break;
		case 5:
			comp.set(Boost.ordinal(), data2);
			compress(2, data2);
			break;
    	case 6: 
			comp.set(Ratio.ordinal(), data2);
			compress(5, data2);
			break;
		case 7:
			comp.set(Release.ordinal(), data2);
			comp.set(Attack.ordinal(), data2);
			compress(2, data2);
			break;
    	default: 
    		return false;
    	}
		return true;
	}

	/**turn on/off compressor, run updates
	 * @param threshhold
	 * @param input */
	private void compress(int threshhold, int input) {
		boolean old = comp.isActive();
		comp.setActive(input > threshhold);
		if (old != comp.isActive()) 
			MainFrame.update(channel);
	}
	
	@Override public void pad1() {
		lfo.setActive(!lfo.isActive());
	}
	
	@Override public void pad2() {
		comp.setActive(!comp.isActive());
		MainFrame.update(channel);
		MainFrame.update(this);
	}
	
	@Override
	public final void update() {
		for (Row lbl : labels) 
        	lbl.update();
		wrap.setBackground(lfo.isActive() ? EffectColor.get(lfo.getClass()) : null);
		row.update();
		lfoCombo.update();
	}

}
