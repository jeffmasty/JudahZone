package net.judah.gui.knobs;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumDB;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.KitMode;
import net.judah.effects.CutFilter;
import net.judah.gui.MainFrame;
import net.judah.widgets.CenteredCombo;
import net.judah.widgets.Knob;

public class KitKnobs extends KnobPanel {
	
	public static enum Modes {
		Pan, Volume, Attack, Decay, Reverb, HiCut
	}
	
	@Getter private final DrumKit kit;
	private boolean override;
	JComboBox<String> preset = new CenteredCombo<>();
	private ArrayList<KitPad> pads = new ArrayList<>(DrumKit.SAMPLES);
	private JComboBox<Modes> modes = new JComboBox<>(Modes.values());
	private final JPanel titleBar = new JPanel();
	
	private final JComboBox<KitMode> kits = new JComboBox<>(KitMode.values());

	public KitKnobs(DrumKit beats) {
		super(beats.getKitMode().name());
		this.kit = beats;
    	setOpaque(true);
    	
    	JPanel wrap = new JPanel(new GridLayout(0, 4, 1, 1));
    	for (int i = 0; i < DrumKit.SAMPLES; i++)	{
    		KitPad pad = new KitPad(this, DrumType.values()[i]);
    		pads.add(pad);
    		wrap.add(pad);
    	}
    	
    	for (String s : DrumDB.getKits())
    		preset.addItem(s);
    	
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	preset.addActionListener(e->beats.setKit("" + preset.getSelectedItem()));
    	kits.addActionListener(e->MainFrame.setFocus(JudahZone.getDrumMachine().getKits()[kits.getSelectedIndex()].getKnobs()));
    	modes.addActionListener(e->updateMode());
    	titleBar.add(kits);
    	titleBar.add(modes);
    	titleBar.add(preset);
    	update();
    	add(wrap);
    	modes.setSelectedItem(Modes.Decay);
	}

	@Override
	public Component installing() {
		MainFrame.setFocus(kit);
		return titleBar;
	}
	
	@Override
	public void update() {
		if (kit.getKit() != null)
			if (preset.getSelectedItem() != kit.getKit().getFolder().getName()) 
				preset.setSelectedItem(kit.getKit().getFolder().getName());
		pads.forEach(p->p.update());
	}

	public void update(DrumSample o) {
		for (KitPad p : pads) 
			if (p.getSample() == o) 
				p.update();
		
	}
	
	void updateMode() {
		for (KitPad p : pads) {
			Knob knob = p.getKnob();
			DrumSample s = p.getSample();
			override = true;
			Modes mode = (Modes)modes.getSelectedItem();
			switch (mode) {
				case Volume: 
					knob.setValue(s.getGain().getVol()); 
					break;
				case Pan: 
					knob.setValue(s.getGain().getPan());
					break;
				case Attack: 
					knob.setValue(s.getAttackTime() * 5);
					break;
				case Decay: 
					knob.setValue(s.getDecayTime());
					break;
				case HiCut: 
					knob.setValue(CutFilter.frequencyToKnob(s.getHiCut().getFrequency()));
					break;
				case Reverb: 
					knob.setValue((int)(s.getReverb().getWet() * 100));
					break;
			}		
		}
		update();
		override = false;
	}
	
	@Override
	public boolean doKnob(int idx, int data2) {

		// phase 1: possibly feed Knob gui from Midi Controller
		if (pads.get(idx).getKnob().getValue() != data2) {
			pads.get(idx).getKnob().setValue(data2);
			return true;
		}
		
		if (override) return true;
		
		// phase 2: update model
		DrumSample s = kit.getSamples()[idx]; 
		Modes mode = (Modes)modes.getSelectedItem();
		switch(mode) {
			case Volume: 
				s.getGain().setVol(data2); 
				break;
			case Attack: 
				s.setAttackTime((int)(data2 * 0.2f)); 
				break;
			case Decay: 
				s.setDecayTime((data2)); 
				break;
			case HiCut: 
				float hz = CutFilter.knobToFrequency(data2);
				s.getHiCut().setFrequency(hz); 
				s.getHiCut().setActive(data2 < 98);
				break;
			case Reverb: 
				s.getReverb().setWet(data2 * 0.01f); 
				s.getReverb().setActive(data2 > 2); 
				break;
			case Pan: 
				s.getGain().setPan(data2);
				break;
		}
		return true;
	}

	@Override
	public void pad1() {
		int i = 1 + modes.getSelectedIndex();
		if (i == modes.getItemCount())
			i = 0;
		modes.setSelectedIndex(i);
	}

}
