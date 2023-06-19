package net.judah.song.setlist;

import java.io.File;
import java.util.ArrayDeque;

import javax.swing.JComboBox;

import net.judah.JudahZone;
import net.judah.gui.widgets.FileRender;

public class SetlistsCombo extends JComboBox<File> {

	private static ArrayDeque<SetlistsCombo> instances = new ArrayDeque<>();
	
    public SetlistsCombo(Setlists setlists) {
    	super(setlists.array());
    	setSelectedItem(setlists.getCurrent().getSource());
    	addActionListener(e->JudahZone.getSetlists().setCurrent((File)getSelectedItem()));
		setRenderer(new FileRender());
    	instances.add(this);
    }

	public static void setCurrent(File list) {
		for (SetlistsCombo combo : instances) {
			if (combo.getSelectedItem() != list)
				combo.setSelectedItem(list);
		}
	}
    
    
}
