package net.judah.gui.settable;

import java.io.File;
import java.util.ArrayList;

import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.widgets.FileRender;
import net.judah.omni.Threads;
import net.judah.song.Overview;

public class SongCombo extends SetCombo<File> {

	private static ArrayList<SongCombo> instances = new ArrayList<>();

	public SongCombo() {
		super(JudahZone.getSetlists().getCurrent().array(), null);
		setRenderer(new FileRender());
		instances.add(this);
		setFont(Gui.BOLD12);
	}

	public void update() {
		File select = (File)getSelectedItem();
		File song = JudahZone.getOverview().getSong().getFile();

		if (select == null && song == null)
			return;
		if (song == null) {
			override(null);
			return;
		}
		if (false == song.equals(select))
			override(song);
	}

	@Override protected void action() {
		Overview songs = JudahZone.getOverview();
		if (getSelectedItem() == null)
			songs.newSong();
		else if (songs.getSong().getFile() == null)
			songs.loadSong((File)getSelectedItem());
		else if (!songs.getSong().getFile().equals(getSelectedItem()))
			songs.loadSong((File)getSelectedItem());
	}

	public static void refill(final File[] folder) {
		instances.forEach(combo-> combo.refill(folder, null));
		JudahZone.getMidiGui().getTitle().doLayout();
	}

	public static void refresh(final File[] setlist, final File selected) {
		Threads.execute(()->
			instances.forEach(combo-> combo.refill(setlist, selected)));
	}
	public static void refresh() {
		instances.forEach(combo ->combo.update());
	}

}
