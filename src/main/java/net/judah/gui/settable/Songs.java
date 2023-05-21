package net.judah.gui.settable;

import java.io.File;
import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.gui.widgets.FileRender;
import net.judah.util.Constants;
import net.judah.util.Folders;

public class Songs extends SetCombo<File> {

	private static ArrayList<Songs> instances = new ArrayList<>();

	public Songs() {
		super(Folders.sortSetlist(), null);
		setRenderer(new FileRender());
		instances.add(this);
	}
	
	public static void refresh() {
		instances.forEach(combo ->combo.update());
	}

	public void update() {
		File select = (File)getSelectedItem();
		File song = JudahZone.getCurrent().getFile();
		
		if (select == null && song == null)
			return;
		if (song == null) {
			override(null);
			return;
		}
		if (false == song.equals(select))	
			override(song);
	}

	@Override
	protected void action() {
		JudahZone.loadSong((File)getSelectedItem()); 
	}

	public static void refill() {
		Constants.execute(()->
			instances.forEach(combo->
				combo.refill(Folders.getSetlist().listFiles(), JudahZone.getCurrent().getFile())));
	}

}
