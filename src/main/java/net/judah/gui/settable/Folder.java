package net.judah.gui.settable;

import java.io.File;

import judahzone.util.Folders;
import judahzone.widgets.FileRender;
import net.judah.seq.track.MidiTrack;

public class Folder extends SetCombo<File> {

	private final MidiTrack track;

	public Folder(MidiTrack t) {
		super(new File[] {}, null);
		this.track = t;
		setRenderer(new FileRender());
		refill();
	}

	public void refill() {
		removeActionListener(listener);
		removeAllItems();
		addItem(null);
		for (File f : Folders.sort(track.getFolder()))
			if (f.isFile())
				addItem(f);
		setSelectedItem(track.getFile());
		addActionListener(listener);
	}

	public void update() {
		if (track.getFile() != getSelectedItem())
			override(track.getFile());
	}

	@Override protected void action() {
		track.load((File)getSelectedItem());
	}

// TimeSig
//	public static void refillAll() {
//		Threads.execute(()->{
//			for (Folder folder : instances)
//				folder.refill();
//		});
//	}



}
