package net.judah.drums.oldschool;

import java.io.File;

import judahzone.data.Recording;
import net.judah.drums.DrumType;


@Deprecated
public record DrumPreset (Recording[] samples, File folder) {

//	private static final int SIZE = DrumType.values().length;
//
//	private final Recording[] samples = new Recording[SIZE];
//	private final File folder;
//
//	public DrumPreset(File folder) throws Exception {
//		for (File file : folder.listFiles())
//			for (DrumType d : DrumType.values())
//				if (file.getName().startsWith(d.name()))
//					samples[d.ordinal()] = DrumDB.get(file);
//		this.folder = folder;
//	}
//
//	/** blocks while reading from disk */
//	public DrumPreset (String kit) throws Exception {
//		this(new File(Folders.getKits(), kit));
//	}

	@Deprecated
	public Recording get(int i) {
		return samples[i];
	}

	@Deprecated
	public Recording get(DrumType type) {
		return samples[type.ordinal()];
	}

//	public static DrumPreset save(String[] names, File f) throws Exception {
//		StringBuffer buf = new StringBuffer();
//		for (String name : names)
//			buf.append(name).append(Constants.NL);
//        Threads.writeToFile(f, buf.toString());
//        return new DrumPreset(f);
//	}


}
