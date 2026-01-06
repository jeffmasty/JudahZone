package net.judah.drumkit;

import java.io.File;

import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.Recording;
import judahzone.util.Threads;
import lombok.Getter;

@Getter
public class DrumPreset {

	private final int SIZE = DrumType.values().length;
	Recording[] samples = new Recording[SIZE];
	private final File folder;

	public DrumPreset(File folder) throws Exception {
		for (File file : folder.listFiles())
			for (DrumType d : DrumType.values())
				if (file.getName().startsWith(d.name()))
					samples[d.ordinal()] = DrumDB.get(file);
		this.folder = folder;
	}

	/** blocks while reading from disk */
	public DrumPreset (String kit) throws Exception {
		this(new File(Folders.getKits(), kit));
	}

	public Recording get(int i) {
		return samples[i];
	}

	public Recording get(DrumType type) {
		return samples[type.ordinal()];
	}

	public static DrumPreset save(String[] names, File f) throws Exception {
		StringBuffer buf = new StringBuffer();
		for (String name : names)
			buf.append(name).append(Constants.NL);
        Threads.writeToFile(f, buf.toString());
        return new DrumPreset(f);
	}


}
