package net.judah.tracker;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JComboBox;

public class FileCombo extends JComboBox<String> implements ItemListener {

	private boolean inUpdate;
	private final Track track;
	
	public FileCombo(Track track) {
		this.track = track;
		
		refresh();

		if (track.getFile() != null) {
			removeActionListener(this);
			setSelectedItem(track.getFile().getName());
			addActionListener(this);
		}
		
	}

	@Override public void itemStateChanged(ItemEvent e) {
		if (inUpdate) {
			inUpdate = false;
			return;
		}
		inUpdate = true;
		String select = "" + getSelectedItem();
		if (select.isBlank()) {
			track.setFile(null);
		}
		else {
			File file = new File(track.getFolder(), getSelectedItem().toString());
			track.setFile(file);
		}
	}
	
	public void refresh() {
		String selected = "" + getSelectedItem();
		removeActionListener(this);
		removeAllItems();
		addItem("");
		for (String name : track.getFolder().list()) 
			addItem(name);
		setSelectedItem(selected);
		addActionListener(this);
	}

}
