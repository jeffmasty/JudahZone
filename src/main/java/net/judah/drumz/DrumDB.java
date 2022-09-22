package net.judah.drumz;

import java.io.File;
import java.util.ArrayList;

import lombok.Getter;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class DrumDB {
	private static final int LENGTH = DrumType.values().length;

	@Getter private static final ArrayList<ArrayList<File>> samples = new ArrayList<>();
	@Getter private static final ArrayList<String> kits = new ArrayList<>();
	static {
		for (int i = 0; i < DrumType.values().length; i++) 
			samples.add(new ArrayList<File>());
		int count = 0;
		for (File folder : Constants.KITS.listFiles()) {
			if (folder.isDirectory() == false) 
				continue;
			int oldDB = count;
			for (File drum : folder.listFiles()) {
				String type  = drum.getName().split("[.]")[0];
				try {
					DrumType t = DrumType.valueOf(type);
					if (t != null) {
						samples.get(t.ordinal()).add(drum);
						count++;
					}
				} catch (Throwable t) {
					RTLogger.log("Drum DB", t.getMessage() + " - " + drum.getAbsolutePath());
				}
				if (count - LENGTH == oldDB)
					kits.add(folder.getName());
					
			}
		}
		RTLogger.log(DrumDB.class, count + " samples in DB. " + kits.size() + " Kits");
	}

	
}
