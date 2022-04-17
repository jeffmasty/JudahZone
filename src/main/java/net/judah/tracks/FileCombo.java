package net.judah.tracks;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

import javax.swing.JComboBox;

public class FileCombo extends JComboBox<String> {

	private final Track track;
	private final boolean isMidi;
	
	private final ItemListener listener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			File file = new File(track.getFolder(), getSelectedItem().toString() + ".mid");
			track.setFile(file);
		}
	};
	
	public FileCombo(Track track) {
		this.track = track;
		
		File selected = track.getFile();
		File folder = track.getFolder();
		
		String select = "";
		isMidi = track instanceof MidiTrack;
		
		if (selected != null) 
			select = isMidi ? selected.getName().replace(".mid", "") : selected.getName();
		
		for (String name : folder.list()) {
			if (isMidi && name.endsWith(".mid"))
				name = name.replace(".mid", "");
			addItem(name);
			if (name.equals(select))
				setSelectedItem(name);
		}
		addItemListener(listener);
	}
	
}
