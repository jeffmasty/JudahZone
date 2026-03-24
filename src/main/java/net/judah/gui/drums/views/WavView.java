package net.judah.gui.drums.views;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeListener;

import judahzone.drums.wav.WavDrum;
import judahzone.drums.wav.WavDrumDB;

/** Simple controls for a DrumSample: Vol, Pan, Attack, Decay. */
public class WavView extends DrumView<WavDrum> implements ChangeListener, DrumView.SZ {

	private final JComboBox<String> available = new JComboBox<>(WavDrumDB.getPatches());

	public WavView(WavDrum sample) {
		super(sample);

		child.add(new JLabel("Sample: "));
		child.add(available);
		update();
		available.addActionListener(e->change());
	}

	private void change() {
		String kit = (String) available.getSelectedItem();
		if (kit != null && !kit.equals(drum.getSampleKit()))
			drum.setRecording(kit, WavDrumDB.getDrum(kit, drum.getType()).orElseThrow());
	}

	@Override
	protected void updateChild() {
		String sample = drum.getSampleKit();
		if (available.getSelectedItem() != sample)
			available.setSelectedItem(sample);
	}



}
