package net.judah.gui.widgets;

import java.util.HashSet;

import net.judah.drumkit.DrumKit;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.seq.track.DrumTrack;

public class TrackGain extends Slider {

	private static final HashSet<TrackGain> instances = new HashSet<TrackGain>();

	DrumKit kit;

	public TrackGain(DrumKit kit) {
		super(null);
		this.kit = kit;
		instances.add(this);
		Gui.resize(this, Size.MODE_SIZE);
		setValue(kit.getVolume());
		addChangeListener(e->{
			if (kit.getVolume() != getValue()) {
				kit.getGain().set(Gain.VOLUME, getValue());
				MainFrame.update(kit);
			}
		});
	}

	public TrackGain(DrumTrack t) {
		this(t.getKit());
	}

	public static void update(DrumKit k) {
		for (TrackGain g : instances)
			if (g.kit == k)
				if (g.getValue() != k.getVolume())
					g.setValue(k.getVolume());
	}

}
