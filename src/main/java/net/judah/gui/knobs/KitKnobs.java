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
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.CenteredCombo;

public class KitKnobs extends KnobPanel {
	
	public static enum Modes {
		Pan, Volume, Attack, Decay, Reverb, HiCut
	}
	
	@Getter private DrumKit kit;
	JComboBox<String> preset = new CenteredCombo<>();
	private ArrayList<KitPad> pads = new ArrayList<>(DrumKit.SAMPLES);
	private JComboBox<Modes> modes = new JComboBox<>(Modes.values());
	private final JComboBox<KitMode> kits = new JComboBox<>(KitMode.values());
	private final JPanel titleBar = new JPanel();

	public Modes getMode() {
		return (Modes) modes.getSelectedItem();
	}
	
	public KitKnobs() {
		super("DrumKits");
    	setOpaque(true);
    	
    	JPanel wrap = new JPanel(new GridLayout(0, 4, 1, 1));
    	for (DrumType t : DrumType.values()) {
    		KitPad pad = new KitPad(this, t);
    		pads.add(pad);
    		wrap.add(pad);
    	}
    	for (String s : DrumDB.getKits())
    		preset.addItem(s);
    	
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	preset.addActionListener(e->kit.setKit("" + preset.getSelectedItem()));
    	kits.addActionListener(e->setKit(JudahZone.getDrumMachine().getKits()[kits.getSelectedIndex()]));
    	modes.setSelectedItem(Modes.Pan);
    	
    	modes.addActionListener(e->{
    		update();
    	});
    	titleBar.add(kits);
    	titleBar.add(modes);
    	titleBar.add(preset);
    	add(wrap);
	}
	
	public void setKit(DrumKit k) {
		this.kit = k;
		MainFrame.update(k);
	}

	@Override
	public Component installing() {
		MainFrame.setFocus(kit);
		return titleBar;
	}
	
	@Override
	public void update() {
		if (kits.getSelectedItem() != kit)
			kits.setSelectedItem(kit);
		if (preset.getSelectedItem() != kit.getKit().getFolder().getName()) 
			preset.setSelectedItem(kit.getKit().getFolder().getName());
		pads.forEach(p->p.update());
	}

	public void update(DrumSample o) {
		for (int i = 0; i < DrumKit.SAMPLES; i++)
			if (kit.getSamples()[i] == o)
				pads.get(i).update();
	}
	
	@Override
	public boolean doKnob(int idx, int data2) {
		pads.get(idx).knobChanged(data2);
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
