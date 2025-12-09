package net.judah.gui.fx;

import static net.judah.JudahZone.getFrame;
import static net.judah.JudahZone.getPresets;

import java.awt.FlowLayout;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.judah.gui.Updateable;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TogglePreset;
import net.judah.mixer.Channel;
import net.judah.omni.Icons;

public class PresetsBtns extends JPanel implements Updateable {

	private final Channel ch;
	protected final TogglePreset fx;


	public PresetsBtns(Channel ch) {
		super(new FlowLayout(FlowLayout.CENTER, 0, 0));
		this.ch = ch;
		fx = new TogglePreset(ch);

		add(fx);
		add(new Btn(Icons.SAVE, e->getPresets().replace(ch)), "Save Current");
		add(new Btn(Icons.NEW_FILE, e->create()), "New Preset");
	}

    public void create() {
        String name = JOptionPane.showInputDialog(getFrame(), "Preset Name:");
        if (name == null || name.isEmpty()) return;
        getPresets().add(ch, name);
    }

	@Override
	public void update() {
		fx.update();
	}

}
