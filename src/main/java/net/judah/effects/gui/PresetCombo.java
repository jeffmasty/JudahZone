package net.judah.effects.gui;

import java.util.List;

import javax.swing.JComboBox;

import net.judah.effects.api.Effect;
import net.judah.effects.api.Preset;
import net.judah.mixer.Channel;
import net.judah.util.Pastels;

public class PresetCombo extends JComboBox<Preset> implements Effect {

	private final Channel ch;
	
	public PresetCombo(Channel channel, List<Preset> list) {
		ch = channel;
		for (Preset p : list) 
			addItem(p);
		setSelectedItem(ch.getPreset());
		addActionListener( e -> {
			Preset p = (Preset)getSelectedItem();
			if (ch.getPreset() != p)
				ch.setPreset(p);
		});
	}
	
	public void update() {
		if (ch.getPreset() != null)
			setSelectedItem(ch.getPreset());
		setBackground(ch.isPresetActive() ? Pastels.GREEN: null);
	}
	
	@Override public boolean isActive() {
		return ch.isPresetActive();
	}

	@Override public void setActive(boolean active) {
		ch.setPresetActive(active);
	}

	@Override public void set(int idx, int value) {	}

	@Override public int get(int idx) {
		return -1;
	}

	@Override public int getParamCount() {
		return 0;
	}

}
