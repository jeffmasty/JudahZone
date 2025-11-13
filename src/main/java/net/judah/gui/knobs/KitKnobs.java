package net.judah.gui.knobs;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.gui.Gui;
import net.judah.gui.TabZone;
import net.judah.gui.knobs.KitPad.Modes;
import net.judah.gui.widgets.Btn;
import net.judah.midi.Actives;
import net.judah.omni.Icons;
import net.judah.seq.Trax;

public class KitKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Kitz;
	@Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

	private  final DrumMachine drums;
	private final ArrayList<DrumKit> kits;
	private final ArrayList<KitPad> pads = new ArrayList<>(DrumKit.SAMPLES);
	private final JComboBox<Trax> trax = new JComboBox<>(Trax.drums);
	private final JComboBox<Modes> modes = new JComboBox<>(Modes.values());

	public KitKnobs(final DrumMachine drumz) {

		drums = drumz;
		kits = drums.getKits();
    	JPanel wrap = new JPanel(new GridLayout(0, 4, 1, 1));
    	for (DrumType t : DrumType.values()) {
    		KitPad pad = new KitPad(drums, t, modes);
    		pads.add(pad);
    		wrap.add(pad);
    	}
    	trax.setSelectedItem(Trax.D1);
    	trax.addActionListener(e-> drums.setCurrent((Trax)trax.getSelectedItem()));

    	modes.addActionListener(e-> pads.forEach(p->p.updateMode((Modes)modes.getSelectedItem())));
    	modes.setSelectedItem(Modes.Volume);

    	title.add(modes);
    	title.add(Gui.wrap(new Btn(Icons.NEW_FILE, e->reset(), "Reset Settings"),
    			new Btn(Icons.SAVE, e->	drums.getSettings().serialize(JudahZone.getOverview()), "Save Settings in Song")));
    	title.add(trax);

    	setOpaque(true);
    	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	add(Box.createGlue());
    	add(wrap);
    	add(Box.createGlue());

	}

	private void reset() {
		drums.getSettings().reset();
		JudahZone.getOverview().getSong().setKit(null);
		JudahZone.getOverview().save();
	}

	public void update(DrumSample o) {
		for (DrumKit kit : kits)
			for (int i = 0; i < DrumKit.SAMPLES; i++)
				if (kit.getSamples()[i] == o)
					pads.get(i).update();
	}

	@Override
	public void update() {
		if (trax.getSelectedItem() != drums.getCurrent().getType())
			trax.setSelectedItem(drums.getCurrent().getType());
		pads.forEach(p->p.update());
	}

	@Override
	public boolean doKnob(int idx, int data2) {
		pads.get(idx).knobChanged(data2);
		return true;
	}

	@Override public void pad1() {
		TabZone.edit(JudahZone.getSeq().byName(drums.getTracks().getCurrent().getName()));
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
