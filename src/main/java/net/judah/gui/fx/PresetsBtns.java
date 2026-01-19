package net.judah.gui.fx;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import judahzone.api.FX;
import judahzone.gui.Icons;
import judahzone.gui.Updateable;
import judahzone.widgets.Btn;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.PresetsDB;
import net.judah.gui.widgets.TogglePreset;

public class PresetsBtns extends JPanel implements FXAware, Updateable {

	private final Channel ch;
	protected final TogglePreset toggle;

	public PresetsBtns(Channel ch) {
		this.ch = ch;
		toggle = new TogglePreset(ch);

		add(toggle);
		add(new Btn(Icons.SAVE, e->PresetsDB.replace(ch)), "Save Current");
		add(new Btn(Icons.NEW_FILE, e->create()), "New Preset");
	}

	/**@returns null = presets identifier */
	@Override public FX getFx() { return null; }

	@Override public void update() { toggle.update(); }

    public void create() {
        String name = JOptionPane.showInputDialog(JudahZone.getInstance().getFrame(), "Preset Name:");
        if (name == null || name.isEmpty()) return;
        PresetsDB.add(ch, name);
    }


}
