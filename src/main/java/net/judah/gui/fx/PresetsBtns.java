package net.judah.gui.fx;

import static net.judah.JudahZone.getPresets;

import java.awt.FlowLayout;

import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.Pastels;
import net.judah.gui.Updateable;
import net.judah.gui.widgets.Btn;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;

public class PresetsBtns extends JPanel implements Updateable {

	private final Channel ch;
	private final Looper looper;
	private final Btn fx;
	private final Btn latch;
	
	
	public PresetsBtns(Channel ch, Looper looper) {
		super(new FlowLayout(FlowLayout.CENTER, 0, 0));
		this.ch = ch;
		this.looper = looper;
		fx = new Btn("Fx", e->ch.setPresetActive(!ch.isPresetActive()));
		latch = new Btn(" 🔁 ", e->looper.syncFx(ch), "Loop Sync");
		
		add(fx);
		add(latch);
		add(new Btn(Icons.SAVE, e->getPresets().replace(ch)), "Save Current");
		add(new Btn(Icons.NEW_FILE, e->create()), "New Preset");
	}
	
    public void create() {
        String name = Gui.inputBox("Preset Name:");
        if (name == null || name.isEmpty()) return;
        getPresets().add(ch.toPreset(name));
        getPresets().save();
    }
	
	
	@Override
	public void update() {
		fx.setBackground(ch.isPresetActive() ? Pastels.BLUE : null);
		latch.setBackground(looper.getFx().contains(ch) ? Pastels.YELLOW : null);
	}

}
