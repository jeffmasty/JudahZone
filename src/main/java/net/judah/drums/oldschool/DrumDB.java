package net.judah.drums.oldschool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import judahzone.data.Recording;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.WavConstants;
import lombok.Getter;
import net.judah.drums.DrumType;


public class DrumDB {

	/** drum sample cache */
	private static HashMap<File, Recording> db = new HashMap<>();
	@Getter private static final HashMap<String, Recording[]> kits = new HashMap<>();

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
		ArrayList<String> names = new ArrayList<>();
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
			if (count - LENGTH == oldDB) {
				names.add(folder.getName());
			}
			else {
				RTLogger.log(DrumDB.class, "Skipping " + folder.getAbsolutePath());
			}
		}
		Collections.sort(names);
		// PRE-CACHE
		for (ArrayList<File> list : samples)
			for (File f : list)
				try {
					db.put(f, Recording.loadInternal(f, WavConstants.FILE_LEVEL));
				} catch (Throwable t) {
					RTLogger.warn(DrumDB.class, "Pre-cache failed: " + t.getMessage());
				}
		RTLogger.debug(DrumDB.class, "Loaded " + count + " samples in " + (System.currentTimeMillis() - startTime)
				+ " msec. " + Arrays.toString(names.toArray()));

		// build kits
		for (String name : names) {
			Recording[] kit = new Recording[LENGTH];
			for (int i = 0; i < LENGTH; i++) {
				File f = new File(Folders.getKits(), name + "/" + DrumType.values()[i].name() + ".wav");
				if (f.exists())
					kit[i] = db.get(f);
			}
			kits.put(name, kit);
		}


		initialized = true;
		} catch (Throwable t) {
			RTLogger.warn(DrumDB.class, t);
		}
	}

//	/** return index of kit in DB, or 0 */
//	public static int indexOf(DrumPreset kit) {
//		// Fixed:
//		int index = 0;
//		for (String key : kits.keySet()) {
//		    if (key.equals(kit.folder().getName()))
//		        return index;
//		    index++;
//		}
//		return 0;
//	}


	public static String[] getPatches() {
		return kits.keySet().toArray(String[]::new);
	}


}
