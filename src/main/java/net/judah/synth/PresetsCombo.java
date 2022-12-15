package net.judah.synth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import net.judah.JudahZone;

public class PresetsCombo extends JComboBox<String> {

	private final ActionListener handler;
	private final SynthPresets presets;
	
	public PresetsCombo(SynthPresets presets) {
		this.presets = presets;
		handler = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				presets.load("" + getSelectedItem());
			}};
		initPresets();
	}
	
	/**@param file the selected file or null */
	public void initPresets() {
		String selected = presets.getCurrent();
		removeActionListener(handler);
		removeAllItems();
		for (String s : JudahZone.getSynthPresets().keys()) {
			addItem(s);
			if (s.equals(selected))
				setSelectedItem(s);
		}
		addActionListener(handler);
	}
	
	public void select() {
		removeActionListener(handler);
		if (presets.getCurrent() != null)
			setSelectedItem(presets.getCurrent());
		addActionListener(handler);
	}
	
	
}