package net.judah.seq.chords;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JComboBox;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.FileRender;
import net.judah.util.Folders;

public class ChordProCombo extends JComboBox<File> {
	
	private static final ArrayList<ChordProCombo> instances = new ArrayList<>();
	protected final ActionListener listener = (e)->action();
	
	public ChordProCombo() {
		Gui.resize(this, Size.MEDIUM_COMBO);
		setRenderer(new FileRender());
		refill(null);
		instances.add(this);
	}

	public static void refresh(File selected) {
		for (ChordProCombo instance : instances) 
			if (instance.getSelectedItem() != selected)
				instance.setSelectedItem(selected);
	}
	
	public void refill(File selected) {
		removeActionListener(listener);
		removeAllItems();
		addItem(null); 
		for (File f : Folders.sort(Folders.getChordPro())) {
			if (f.isFile()) {
				addItem(f);
				if (f.equals(selected))
					setSelectedItem(f);
			}
		}
		addActionListener(listener);
	}
	
	protected void action() {
		File target = getSelectedItem() == null ? null : (File)getSelectedItem();
		JudahZone.getChords().load(target);
	}
	
	
}
