package net.judah.tracker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;

public class FileCombo extends JComboBox<String> implements ActionListener {

	private final Track track;
	
	public FileCombo(Track track) {
		this.track = track;
				DefaultListCellRenderer center = new DefaultListCellRenderer(); 
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		setRenderer(center);
		refresh();
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		String select = "" + getSelectedItem();
		if (select.isBlank()) 
			track.setFile(null);
		else 
			track.setFile(new File(track.getFolder(), select));
	}
	
	public void refresh() {
		String selected = (track.getFile() == null) ? "" : track.getFile().getName();
		removeActionListener(this);
		removeAllItems();
		addItem("");
		String[] sort = track.getFolder().list();
		Arrays.sort(sort);
		for (String name : sort) 
			addItem(name);
		setSelectedItem(selected);
		addActionListener(this);
	}

}
