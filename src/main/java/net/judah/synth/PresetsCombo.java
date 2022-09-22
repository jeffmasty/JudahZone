package net.judah.synth;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComboBox;

import net.judah.util.Constants;

public class PresetsCombo extends JComboBox<String> {
	private final SynthPresets presets;

	private final ActionListener handler;

	public PresetsCombo(SynthPresets pre) {
		presets = pre;
		handler = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				File file = new File(Constants.SYNTH, "" + getSelectedItem());
				presets.load(file);}
			};
		initPresets();
	}
	
	/**@param file the selected file or null */
	public void initPresets() {
		String selected = presets.getLoaded() == null ? null : presets.getLoaded().getName();
		removeActionListener(handler);
		removeAllItems();
		for (String s : Constants.SYNTH.list()) {
			addItem(s);
			if (s.equals(selected))
				setSelectedItem(s);
		}
		addActionListener(handler);
	}
	
	public void select() {
		removeActionListener(handler);
		if (presets.getLoaded() != null)
			setSelectedItem(presets.getLoaded().getName());
		addActionListener(handler);
	}
}