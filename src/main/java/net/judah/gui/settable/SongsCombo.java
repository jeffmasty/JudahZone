package net.judah.gui.settable;

import static net.judah.JudahZone.*;

import java.io.File;
import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.gui.widgets.FileRender;
import net.judah.song.Song;
import net.judah.util.Constants;

public class SongsCombo extends SetCombo<File> {

	private static ArrayList<SongsCombo> instances = new ArrayList<>();

	public SongsCombo() {
		super(JudahZone.getSetlists().getCurrent().array(), null);
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
		if (set == this) return;
		if (getSelectedItem() == null)
			JudahZone.setCurrent(new Song(getSeq(), (int)(getClock().getTempo())));
		else 
			JudahZone.loadSong((File)getSelectedItem()); 
	}

	public static void refill() {
		Constants.execute(()->
			instances.forEach(combo->
				combo.refill(JudahZone.getSetlists().getCurrent().array(), JudahZone.getCurrent().getFile())));
	}

}
