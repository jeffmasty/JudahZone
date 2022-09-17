package net.judah.tracker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

import javax.swing.DefaultListCellRenderer;

import net.judah.util.SettableCombo;

public class FileCombo extends SettableCombo<String> {

	private final Track track;
	ActionListener listen = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			highlight(FileCombo.this);
	}};
	
	
	public FileCombo(Track track) {
		super(() -> setFile(track));
		this.track = track;
				DefaultListCellRenderer center = new DefaultListCellRenderer(); 
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		setRenderer(center);
		refresh();
	}

	private static void setFile(Track t) {
		String select = "" + t.getView().getFilename().getSelectedItem();
		if (select.equals("_clear")) 
			t.clearFile();
		else 
			t.setFile(new File(t.getFolder(), select));
	}
	
	public void refresh() {
		String selected = (track.getFile() == null) ? "_clear" : track.getFile().getName();
		removeActionListener(listen);
		removeAllItems();
		String[] sort = track.getFolder().list();
		Arrays.sort(sort);
		for (String name : sort) 
			addItem(name);
		addItem("_clear");
		setSelectedItem(selected);
		addActionListener(listen);
	}

}
