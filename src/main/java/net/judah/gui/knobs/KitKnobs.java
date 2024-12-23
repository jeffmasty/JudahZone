package net.judah.gui.knobs;

import static net.judah.JudahZone.getDrumMachine;
import static net.judah.JudahZone.getFrame;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
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
import net.judah.gui.widgets.CenteredCombo;
import net.judah.midi.Actives;

public class KitKnobs extends KnobPanel {
	
	public static enum Modes {
		Pan, Volume, Attack, Decay, Dist, pArTy;
	}
	
	@Getter private final DrumKit kit;
	@Getter private final KnobMode knobMode = KnobMode.KITS;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final JComboBox<String> preset = new CenteredCombo<>();
	private final ArrayList<KitPad> pads = new ArrayList<>(DrumKit.SAMPLES);
	private final JComboBox<KitMode> kits = new JComboBox<>(KitMode.values());
	private final JComboBox<Modes> modes = new JComboBox<>(Modes.values());

	public Modes getMode() {
		return (Modes) modes.getSelectedItem();
	}
	
	public KitKnobs(DrumKit k) {
		kit = k;
    	
    	JPanel wrap = new JPanel(new GridLayout(0, 4, 1, 1));
    	for (DrumType t : DrumType.values()) {
    		KitPad pad = new KitPad(this, t);
    		pads.add(pad);
    		wrap.add(pad);
    	}
    	for (String s : DrumDB.getKits()) 
    		preset.addItem(s);
    	
    	preset.addActionListener(e->kit.progChange("" + preset.getSelectedItem()));
    	kits.setSelectedItem(kit.getKitMode());
    	kits.addActionListener(e-> getDrumMachine().focus((KitMode)kits.getSelectedItem()));
    	
    	modes.setSelectedItem(Modes.pArTy);
    	modes.addActionListener(e-> update());
    	title.add(kits);
    	title.add(modes);
    	title.add(preset);
    	setOpaque(true);
    	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	add(Box.createGlue());
    	add(wrap);
    	add(Box.createGlue());

	}
	
	@Override
	public void update() {
		if (preset.getSelectedItem() != kit.getPreset().getFolder().getName()) 
			preset.setSelectedItem(kit.getPreset().getFolder().getName());
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
	
	@Override public void pad1() {
		getFrame().edit(JudahZone.getSeq().byName(kit.toString()));
	}
	
	@Override public void pad2() {
		int i = 1 + modes.getSelectedIndex();
		if (i == modes.getItemCount())
			i = 0;
		modes.setSelectedIndex(i);
	}

	public void update(Actives a) {
		pads.forEach(pad-> pad.background(a));
	}

}
