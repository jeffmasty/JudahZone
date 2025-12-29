package net.judah.gui.fx;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.gui.settable.PresetsHandler;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TogglePreset;
import net.judah.mixer.Channel;
import net.judahzone.gui.Icons;
import net.judahzone.gui.Updateable;

public class PresetsBtns extends JPanel implements Updateable {

	private final Channel ch;
	protected final TogglePreset fx;

	public PresetsBtns(Channel ch) {
		this.ch = ch;
		fx = new TogglePreset(ch);

		add(fx);
		add(new Btn(Icons.SAVE, e->PresetsHandler.getPresets().replace(ch)), "Save Current");
		add(new Btn(Icons.NEW_FILE, e->create()), "New Preset");
	}

    public void create() {
        String name = JOptionPane.showInputDialog(JudahZone.getInstance().getFrame(), "Preset Name:");
        if (name == null || name.isEmpty()) return;
        PresetsHandler.getPresets().add(ch, name);
    }

	@Override
	public void update() {
		fx.update();
	}

}
