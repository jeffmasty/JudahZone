package net.judah.gui.knobs;

import static net.judah.JudahZone.getDrumMachine;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.drumkit.DrumDB;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.KitMode;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.CenteredCombo;

public class KitKnobs extends KnobPanel {
	
	public static enum Modes {
		Pan, Volume, Attack, Decay, Dist, pArTy;
	}
	private final JComboBox<Modes> modes = new JComboBox<>(Modes.values());
	
	@Getter private final DrumKit kit;
	@Getter private final KnobMode knobMode = KnobMode.Kits;
	private JComboBox<String> preset = new CenteredCombo<>();
	private ArrayList<KitPad> pads = new ArrayList<>(DrumKit.SAMPLES);
	private final JComboBox<KitMode> kits = new JComboBox<>(KitMode.values());
	private final JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

	public Modes getMode() {
		return (Modes) modes.getSelectedItem();
	}
	
	public KitKnobs(DrumKit k) {
		super("DrumKits");
		kit = k;
    	setOpaque(true);
    	
    	JPanel wrap = new JPanel(new GridLayout(0, 4, 1, 1));
    	for (DrumType t : DrumType.values()) {
    		KitPad pad = new KitPad(this, t);
    		pads.add(pad);
    		wrap.add(pad);
    	}
    	for (String s : DrumDB.getKits()) preset.addItem(s);
    	
    	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	preset.addActionListener(e->kit.setKit("" + preset.getSelectedItem()));
    	kits.setSelectedItem(kit.getKitMode());
    	kits.addActionListener(e->MainFrame.setFocus(getDrumMachine().getKnobs((KitMode)kits.getSelectedItem())));
    	modes.addActionListener(e-> update());
    	titleBar.add(kits);
    	titleBar.add(modes);
    	titleBar.add(preset);
    	add(Box.createGlue());
    	add(wrap);
    	add(Box.createGlue());

	}
	
	@Override
	public Component installing() {
		return titleBar;
	}
	
	@Override
	public void update() {
//		if (kits.getSelectedItem() != kit)
//			kits.setSelectedItem(kit);
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
