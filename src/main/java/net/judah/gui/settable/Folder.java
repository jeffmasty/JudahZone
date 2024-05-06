package net.judah.gui.settable;

import java.io.File;
import java.util.ArrayList;

import net.judah.gui.widgets.FileRender;
import net.judah.seq.track.MidiTrack;
import net.judah.util.Constants;
import net.judah.util.Folders;

public class Folder extends SetCombo<File> {

	private static ArrayList<Folder> instances = new ArrayList<>();
	
	private final MidiTrack track;
			
	public Folder(MidiTrack t) {
		super(new File[] {}, null);
		this.track = t;
		setRenderer(new FileRender());
		instances.add(this);
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
	
	
	@Override
	public void override(File val) {
		super.override(val);
	}
	
	public static void update(MidiTrack t) {
		for (Folder f : instances)
			if (f.track == t && f.getBorder() != HIGHLIGHT)
				f.update();
	}
	
	public void update() {
		if (track.getFile() != getSelectedItem())
			override(track.getFile());
	}

	@Override
	protected void action() {
		track.load((File)getSelectedItem());
	}

	public static void refill(MidiTrack t) {
		Constants.execute(()->{
			for (Folder folder : instances)
				if (folder.track == t)
					folder.refill();
		});
	}

	public static void refillAll() {
		Constants.execute(()->{
			for (Folder folder : instances)
				folder.refill();
		});
	}
	
	
	
}
