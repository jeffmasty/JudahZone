package net.judah.gui.drums.knobs;

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumKit;
import net.judah.drums.DrumUpdate;
import net.judah.drums.noise.NoiseKit;
import net.judah.drums.wav.WavKit;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.midi.Actives;

public class Kitz extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.Kitz;
	@Getter private final JPanel title = new JPanel();
	private KitKnobs knobs;

	public Kitz(List<DrumKit> kits) {
		title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
		ButtonGroup g = new ButtonGroup();
		for (DrumKit k : kits) {
			JToggleButton b = new JToggleButton(k.getName());
			if (g.getButtonCount() == 0)
				b.setSelected(true);
			g.add(b);

			b.addActionListener(e -> install(k.getKnobs()));
			title.add(b);
		}
		title.add(new Btn(Icons.SAVE, e->save()));

		install(kits.get(0).getKnobs());

	}

	protected void install(KitKnobs knobs) {
		this.knobs = knobs;
		super.install(knobs);
		// TODO make sure correct kit toggle button is selected

	}

	public void update(Object o) {
		if (o instanceof Actives a && knobs != null) {
			if (a.getMidiOut() == knobs.getKit())
				knobs.update(a);
		}
		else if (o instanceof DrumUpdate up)
			up.drum().getDrama().set(up);
		else if (o instanceof Drum drum && knobs != null) {
			DrumKit ours = knobs.getKit();
			for (int i = 0; i < DrumKit.LENGTH; i++) {
				if (drum == ours.get(i)) {
					knobs.update(); // TODO targeted
					return;
				}
			}
		}
	}

//	/** Broadcast actives to every kit's knobs (keeps GUI in sync). */
//	public void update(Actives a) {
//		drumz.getKits().forEach(kit -> kit.getKnobs().update(a));
//	}

//	/** Route DrumParams to the DrumSynth that owns the DrumOsc, else broadcast. */
//	private void routeDrumParams(NoiseParams dp) {
//
//		for (DrumKit kit : drumz.getKits()) {
////			if (kit instanceof NoiseKit ds) {
////				if (match == src) {
////					kit.getKnobs().update(dp);
////					return;
////				}
////			}
//		}
//		// fallback: broadcast to all kits
//		drumz.getKits().forEach(kit -> kit.getKnobs().update(dp));
//	}

//	/**Route SampleParams to the OldSchool kit that owns the DrumSample, else* broadcast. */
//	private void routeSampleParams(SampleParams sp) {
//		for (DrumKit kit : drumz.getKits()) {
//			if (kit instanceof SampleKit)
//				kit.getKnobs().update(sp);
//		}
//		// fallback: broadcast to all kits
//		drumz.getKits().forEach(kit -> kit.getKnobs().update(sp));
//	}

	void save() {
		if (installed instanceof KitKnobs knobs) {
			DrumKit kit = knobs.getKit();

			String old =
				kit instanceof NoiseKit synth ? synth.getProgram() :
				kit instanceof WavKit samples ? samples.getKitName() : "??";

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

}