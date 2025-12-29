package net.judah.drumkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Recording;
import lombok.Getter;


public class DrumDB {

	/** drum sample cache */
	private static HashMap<File, Recording> db = new HashMap<>();

	public static Recording get(File file) throws Exception {
		if (db.containsKey(file) == false)
			db.put(file, Recording.loadInternal(file, 2f));
		return db.get(file);
	}

	private static final int LENGTH = DrumType.values().length;

	@Getter private static final ArrayList<ArrayList<File>> samples = new ArrayList<>();
	@Getter private static final ArrayList<String> kits = new ArrayList<>();

	static {
		try {
		for (int i = 0; i < DrumType.values().length; i++)
			samples.add(new ArrayList<File>());
		int count = 0;
		for (File folder : Folders.getKits().listFiles()) {
			if (folder.isDirectory() == false)
				continue;
			int oldDB = count;
			for (String drum : folder.list()) {
				String type  = drum.split("[.]")[0];
				try {
					DrumType t = DrumType.valueOf(type);
					if (t != null) {
						samples.get(t.ordinal()).add(new File(folder, drum));
						count++;
					}
				} catch (Throwable t) {
					RTLogger.log("Drum DB", t.getMessage() + ": " + drum + " in " + folder.getAbsolutePath());
				}
			}
			if (count - LENGTH == oldDB)
				kits.add(folder.getName());
			else {
				RTLogger.log(DrumDB.class, "Skipping " + folder.getAbsolutePath());
			}

		}
		Collections.sort(kits);
		RTLogger.log(DrumDB.class, count + " samples in DB. " + Arrays.toString(kits.toArray()));
		} catch (Throwable t) {
			RTLogger.warn(DrumDB.class, t);
		}
	}
	public static int indexOf(DrumPreset kit) {
		if (kit == null)
			return 0;
		for (int i = 0; i < kits.size(); i++)
			if (kits.get(i).equals(kit.getFolder().getName()))
				return i;
		return 0;
	}


}
