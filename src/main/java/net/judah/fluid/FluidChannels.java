package net.judah.fluid;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.midi.ProgChange;

public class FluidChannels extends ArrayList<FluidChannel> {
	public static final int CHANNELS = 4;
	
	private ProgChange[] channels;

	/** @return preset instrument index for the channel */
	public int getCurrentPreset(int channel) {
		for (FluidChannel c : this)
			if (c.channel == channel)
				return c.preset;
		return 0;
	}
	public int getBank(int channel) {
		for (FluidChannel c : this)
			if (c.channel == channel)
				return c.bank;
		return -1;
	}
	
	public JPanel getGui(FluidSynth fluid)  { // legacy synth engines view
		JPanel result = new JPanel();
		channels = new ProgChange[CHANNELS];
		for (int i = 0; i < CHANNELS; i++) {
			channels[i] = new ProgChange(fluid, i);
		}

		result.setLayout(new GridLayout(4, 1));
		result.setBorder(BorderFactory.createTitledBorder("Fluid Instruments"));
		for (int i = 0; i < CHANNELS; i++) {
			JPanel row = new JPanel();
			row.add(new JLabel("CH " + i));
			row.add(channels[i]);
			result.add(row);
		}
		return result;
	}

	public void update() {
		if (channels == null)
			return;
		for (int i = 0; i < CHANNELS; i++) {
			channels[i].reset(get(i).preset);
		}
	}
}
