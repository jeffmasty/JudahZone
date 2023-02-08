package net.judah.gui.settable;

import java.io.File;
import java.util.ArrayList;

import net.judah.gui.widgets.FileRender;
import net.judah.seq.MidiTrack;
import net.judah.util.Constants;

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
		for (File f : track.getFolder().listFiles()) {
			if (f.isFile()) 
				addItem(f);
		}
		setSelectedItem(track.getFile());
		addActionListener(listener);
	}
	
	public static void update(MidiTrack t) {
		Constants.execute(()->{
			for (Folder f : instances)
				if (f.track == t)
					f.update();
		});
	}
	
	public void update() {
		if (track.getFile() != getSelectedItem())
			override(track.getFile());
//		if (getSelectedItem() == null) {
//			if (track.getFile() != null)
//				refill();
//		}
//		else if (false == getSelectedItem().equals(track.getFile()))
//			refill(track.getFolder().listFiles(), track.getFile());
	}

	@Override
	protected void action() {
		File f = (File)getSelectedItem();
		if (f == null) 
			track.load(null);
		else if (false == f.equals(track.getFile())) 
			track.load(f);
	}

	public static void refill(MidiTrack t) {
		Constants.execute(()->{
			for (Folder folder : instances)
				if (folder.track == t)
					folder.refill();
		});
	}
	
	
	
}
