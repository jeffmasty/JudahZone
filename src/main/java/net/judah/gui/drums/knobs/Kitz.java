package net.judah.gui.drums.knobs;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import judahzone.data.ProgChange;
import judahzone.drums.Drum;
import judahzone.drums.DrumUpdate;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.drums.DrumMachine;
import net.judah.drums.Drumset;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;

public class Kitz extends KnobPanel {

	private static Kitz instance;

	@Getter private final KnobMode knobMode = KnobMode.Kitz;
	@Getter private final JPanel title = new JPanel();
	private KitKnobs knobs;
	private final JComboBox<Drumset> combo;

	private Kitz(DrumMachine drumz) {
		instance = this;
		title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
		combo = new JComboBox<>(drumz.getKits().toArray(new Drumset[0]));
		combo.addActionListener(e -> show(drumz));
		int idx = drumz.getTracks().indexOf(drumz.getTracks().getCurrent());
		combo.setSelectedIndex(idx);
		title.add(combo);
		title.add(new Btn(Icons.SAVE, e->save()));

	}

	protected void install(KitKnobs knobs) {
		this.knobs = knobs;
		super.install(knobs);
		// TODO make sure correct kit toggle button is selected
	}

	public void update(Object o) {
		if (o instanceof DrumUpdate up)
			up.drum().getEdit().set(up);
		else if (o instanceof Drum drum && knobs != null) {
			Drumset ours = knobs.getKit();
			for (int i = 0; i < Drumset.LENGTH; i++) {
				if (drum == ours.get(i)) {
					knobs.update(); // TODO targeted
					return;
				}
			}
		} else if (o instanceof Drumset kit) {
			// in this case one of the drumset's drums has fired or gone silent, update the background of respective drumset's pad, if that drumset is the currently viewed kit.
			if (knobs != null && knobs.getKit() == kit)
				knobs.update(kit.actives());
		}
		else
			RTLogger.log(this, "Unknown update: " + o);
	}

	void save() {
		if (installed instanceof KitKnobs knobs) {
			Drumset kit = knobs.getKit();
			String old = kit.getProgram();
			String name = Gui.inputBox("Name (" + old + ")"); // current
			if (name == null)
				return;
			if (name.isBlank())
				name = old; // default to current if blank
			ProgChange doit = new ProgChange(kit.getProgChange(), name);
			kit.save(doit);
		}
		else {
			RTLogger.log(this, "No kit selected.");
			return;
		}
	}

	@Override public void pad1() {

		if (installed instanceof KitKnobs knobs)
			knobs.pad1();
	}

	@Override public void pad2() {
		if (installed instanceof KitKnobs knobs)
			knobs.pad2();
	}

	@Override public boolean doKnob(int idx, int value) {
		if (installed instanceof KitKnobs knobs) {
			knobs.doKnob(idx, value);
			return true;
		}
		return false;
	}

	public static Kitz show(DrumMachine drums) {
		if (instance == null)
			instance = new Kitz(drums);
		else
			instance.install(new KitKnobs((Drumset)instance.combo.getSelectedItem()));
		return instance;
	}

}