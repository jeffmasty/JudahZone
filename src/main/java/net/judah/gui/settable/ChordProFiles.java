package net.judah.gui.settable;

import java.io.File;

import net.judah.JudahZone;
import net.judah.gui.widgets.FileRender;
import net.judah.song.Song;
import net.judah.util.Folders;

public class ChordProFiles extends SetCombo<File> {

	public ChordProFiles() {
		setRenderer(new FileRender());
		refill();
	}
	
	public void refill() {
		removeActionListener(listener);
		removeAllItems();
		addItem(null);
		
		for (File f : Folders.sort(Folders.getChordPro())) {
			if (f.isFile()) 
				addItem(f);
		}
		Song song = JudahZone.getCurrent();
		if (song != null && song.getChordpro()!=null)
			setSelectedItem(song.getChordpro());
		addActionListener(listener);
	}
	
	@Override
	protected void action() {
		Song song = JudahZone.getCurrent();
		if (song == null || song.getChordpro() == null || 
				!song.getChordpro().equals(getSelectedItem()))
			JudahZone.getChords().load((File)getSelectedItem(), song);
	}

	
}
