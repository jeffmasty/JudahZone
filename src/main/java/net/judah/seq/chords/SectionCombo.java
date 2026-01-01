package net.judah.seq.chords;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;

import judahzone.gui.Gui;
import net.judah.gui.Size;

public class SectionCombo extends JComboBox<Section> {

	private static final ArrayList<SectionCombo> instances = new ArrayList<>();
	private final Chords chords;
	private final ActionListener sectarian;
	
	public SectionCombo(Chords chrds) {
		this.chords = chrds;
		sectarian = e -> {
			if (chords.getSection() != getSelectedItem()) 
				chords.setSection((Section)getSelectedItem(), true);};
		refill();
		instances.add(this);
		Gui.resize(this, Size.COMBO_SIZE);
	}
	
	public static void refresh() {
		for (SectionCombo instance : instances) 
			instance.refill();
	}

	public static void clear() {
		for (SectionCombo instance : instances) {
			instance.removeActionListener(instance.sectarian);
			instance.removeAllItems();
		}
	}
	
	public void refill() {
		removeActionListener(sectarian);
		removeAllItems();
		for (Section s : chords.getSections()) 
			addItem(s);
		addActionListener(sectarian);
	}

	public static void setSection(Section s) {
		for (SectionCombo instance : instances) 
			if (instance.getSelectedItem() != s)
				instance.setSelectedItem(s);
	}
	
}
