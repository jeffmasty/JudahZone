package net.judah.gui.drums.views;

import javax.swing.event.ChangeListener;

import net.judah.drums.wav.WavDrum;
import net.judah.gui.drums.OscTuning;

/** Simple controls for a DrumSample: Vol, Pan, Attack, Decay. */
public class WavView extends DrumView<WavDrum> implements ChangeListener, DrumView.SZ {

	public WavView(WavDrum sample) {
		super(sample);
		update();
	}

	@Override
	protected void updateChild() {
		// no custom controls
	}

	@Override
	protected OscTuning getTuning() {
		return null;
	}

}
