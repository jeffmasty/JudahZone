package net.judah.drumkit;

import java.io.File;

import lombok.Getter;
import net.judah.api.Recording;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.Threads;

@Getter
public class DrumPreset {

	private final int SIZE = DrumType.values().length;
	Recording[] samples = new Recording[SIZE];
	private final File folder;

	public DrumPreset(File folder) throws Exception {
		for (File file : folder.listFiles()) {
			for (DrumType d : DrumType.values())
			if (file.getName().startsWith(d.name())) {
				samples[d.ordinal()] = Recording.loadInternal(file);
			}
		}
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
