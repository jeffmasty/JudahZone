package net.judah.gui.settable;

import java.io.File;
import java.util.ArrayList;

import judahzone.gui.Gui;
import judahzone.util.Threads;
import net.judah.JudahZone;
import net.judah.gui.widgets.FileRender;
import net.judah.song.Overview;

public class SongCombo extends SetCombo<File> {

	private static ArrayList<SongCombo> instances = new ArrayList<>();
	private final JudahZone zone;

	public SongCombo(JudahZone judahZone) {
		super(judahZone.getSetlists().getCurrent().array(), null);
		this.zone = judahZone;
		setRenderer(new FileRender());
		instances.add(this);
		setFont(Gui.BOLD12);
	}

	public void update() {
		File select = (File)getSelectedItem();
		File song = zone.getOverview().getSong().getFile();

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
		Overview view = zone.getOverview();
		if (getSelectedItem() == null)
			view.newSong();
		File selected = (File)getSelectedItem();
		if (selected == null)
			view.newSong();
		else if (view.getSong() == null)
			view.loadSong(selected);
		else if (view.getSong().getFile() == null)
			view.loadSong(selected);
		else if (!view.getSong().getFile().equals(getSelectedItem()))
			view.loadSong(selected);
	}

	public static void refill(final File[] folder) {
		instances.forEach(combo-> combo.refill(folder, null));
		JudahZone.getInstance().getMidiGui().getTitle().doLayout();
	}

	public static void refresh(final File[] setlist, final File selected) {
		Threads.execute(()->
			instances.forEach(combo-> combo.refill(setlist, selected)));
	}
	public static void refresh() {
		instances.forEach(combo ->combo.update());
	}

}
