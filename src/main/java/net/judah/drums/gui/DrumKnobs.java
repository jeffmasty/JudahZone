package net.judah.drums.gui;

import judahzone.gui.Updateable;
import net.judah.drums.DrumKit;
import net.judah.gui.knobs.Knobs;

public abstract class DrumKnobs extends Knobs implements Updateable {

	public abstract void update(Object o); // record: DrumParams, SampleParams, actives

	public abstract DrumKit getKit();

}
