package net.judah.drums.gui;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumMachine;
import net.judah.drums.oldschool.OldSchool;
import net.judah.drums.oldschool.SampleParams;
import net.judah.drums.synth.DrumOsc;
import net.judah.drums.synth.DrumParams;
import net.judah.drums.synth.DrumSynth;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.midi.Actives;

public class KitParent extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Kitz;
	@Getter private final JPanel title = new JPanel();
	private final DrumMachine drumz;

	public KitParent(DrumMachine drumz) {
		this.drumz = drumz;
		title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
		ButtonGroup g = new ButtonGroup();
		for (DrumKit k : drumz.getKits()) {
			JToggleButton b = new JToggleButton(k.getName());
			g.add(b);
			b.addActionListener(e -> install(k.getKnobs()));
			title.add(b);
		}
		title.add(new Btn(Icons.SAVE, e->save()));

		install(drumz.getKits().get(0).getKnobs());

	}

	public void update(Object o) {
		if (o instanceof Actives a)
			update(a);
		else if (o instanceof DrumParams dp)
			routeDrumParams(dp);
		else if (o instanceof SampleParams sp)
			routeSampleParams(sp);
	}

	/** Broadcast actives to every kit's knobs (keeps GUI in sync). */
	public void update(Actives a) {
		drumz.getKits().forEach(kit -> kit.getKnobs().update(a));
	}

	/** Route DrumParams to the DrumSynth that owns the DrumOsc, else broadcast. */
	private void routeDrumParams(DrumParams dp) {
		DrumOsc src = dp.drum();
		for (DrumKit kit : drumz.getKits()) {
			if (kit instanceof DrumSynth ds) {
				DrumOsc match = ds.getDrum(src.getType());
				if (match == src) {
					kit.getKnobs().update(dp);
					return;
				}
			}
		}
		// fallback: broadcast to all kits
		drumz.getKits().forEach(kit -> kit.getKnobs().update(dp));
	}

	/**Route SampleParams to the OldSchool kit that owns the DrumSample, else* broadcast. */
	private void routeSampleParams(SampleParams sp) {
		for (DrumKit kit : drumz.getKits()) {
			if (kit instanceof OldSchool)
				kit.getKnobs().update(sp);
		}
		// fallback: broadcast to all kits
		drumz.getKits().forEach(kit -> kit.getKnobs().update(sp));
	}

	void save() {
		if (installed instanceof DrumKnobs knobs) {
			DrumKit kit = knobs.getKit();

			String old =
				kit instanceof DrumSynth synth ? synth.getProgram() :
				kit instanceof OldSchool samples ? samples.getKitName() : "??";

			String name = Gui.inputBox("Name (" + old + ")"); // current
			if (name == null)
				return;
			if (name.isBlank())
				name = old; // default to current if blank
			kit.save(name);
		}
		else {
			RTLogger.log(this, "No kit selected.");
			return;
		}
	}

	@Override public void pad1() {
		if (installed instanceof ZoneDrums synth)
			synth.pad1();
	}

	@Override public void pad2() {
		if (installed instanceof ZoneDrums synth)
			synth.pad2();
		else if (installed instanceof SampleDrums sd)
			sd.pad2();

	}

	@Override public boolean doKnob(int idx, int value) {
		if (installed instanceof ZoneDrums synth)
			synth.doKnob(idx, value);
		else if (installed instanceof SampleDrums sd)
			sd.doKnob(idx, value);
		else
			return false;
		return true;
	}

}