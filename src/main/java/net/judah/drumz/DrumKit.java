package net.judah.drumz;

import java.io.File;

import lombok.Getter;
import net.judah.looper.WavFile;
import net.judah.util.Constants;

@Getter
public class DrumKit {
	
	private final int SIZE = DrumType.values().length;
	WavFile[] samples = new WavFile[SIZE];
	private final File folder;
	
	public DrumKit(File folder) throws Exception {
		for (File file : folder.listFiles()) {
			for (DrumType d : DrumType.values())
			if (file.getName().startsWith(d.name())) {
				samples[d.ordinal()] = WavFile.load(file);
			}
		}
		this.folder = folder;
	}
	
	/** blocks while reading from disk */
	public DrumKit (String kit) throws Exception {
		this(new File(Constants.KITS, kit));
	}
	
	public WavFile get(int i) {
		return samples[i];
	}
	
	public WavFile get(DrumType type) {
		return samples[type.ordinal()];
	}
	
	public static DrumKit save(String[] names, File f) throws Exception {
		StringBuffer buf = new StringBuffer();
		for (String name : names)
			buf.append(name).append(Constants.NL);
        Constants.writeToFile(f, buf.toString());
        return new DrumKit(f);
	}
	
	
}
