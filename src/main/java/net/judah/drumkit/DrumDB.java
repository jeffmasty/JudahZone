package net.judah.drumkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Recording;
import judahzone.util.WavConstants;
import lombok.Getter;


public class DrumDB {

	/** drum sample cache */
	private static HashMap<File, Recording> db = new HashMap<>();
	@Getter private static final ArrayList<String> kits = new ArrayList<>();
	@Getter private static boolean initialized = false;
	private static final int LENGTH = DrumType.values().length;

	public static Recording get(File file) throws Exception {
		if (db.containsKey(file) == false) {
			try {
				db.put(file, Recording.loadInternal(file, WavConstants.FILE_LEVEL));
				RTLogger.warn(DrumDB.class, "Lazy Load drum sample? " + file.getAbsolutePath());
			} catch (Throwable t) {
				RTLogger.warn(DrumDB.class, "Failed to lazy load drum sample: " + t.getMessage());
			}
		}
		return db.get(file);
	}


	public static void init() {
		final long startTime = System.currentTimeMillis();
		final ArrayList<ArrayList<File>> samples = new ArrayList<>();

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
		// PRE-CACHE
		for (ArrayList<File> list : samples)
			for (File f : list)
				try {
					db.put(f, Recording.loadInternal(f, WavConstants.FILE_LEVEL));
				} catch (Throwable t) {
					RTLogger.warn(DrumDB.class, "Pre-cache failed: " + t.getMessage());
				}
		RTLogger.debug(DrumDB.class, "Loaded " + count + " samples in " + (System.currentTimeMillis() - startTime)
				+ " msec. " + Arrays.toString(kits.toArray()));
		initialized = true;
		} catch (Throwable t) {
			RTLogger.warn(DrumDB.class, t);
		}
	}

	/** return index of kit in DB, or 0 */
	public static int indexOf(DrumPreset kit) {
		if (kit == null)
			return 0;
		for (int i = 0; i < kits.size(); i++)
			if (kits.get(i).equals(kit.getFolder().getName()))
				return i;
		return 0;
	}


}
