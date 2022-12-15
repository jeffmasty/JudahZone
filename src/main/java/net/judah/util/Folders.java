package net.judah.util;

import java.io.File;

import lombok.Getter;

public class Folders {

	public static interface Location {
		File getFile();
		File getFolder();
		void clearFile();
		void setFile(File f);
	}
	
	// TODO user.dir settings file/gui
	static final File _home = new File(System.getProperty("user.home"), "zone");

	@Getter static final File Tracks = new File(_home, "tracks");
	@Getter static final File Samples = new File(_home, "samples");
	@Getter static final File Kits = new File(_home, "kits");
	@Getter static final File SheetMusic = new File(_home, "sheets");
	@Getter static final File Midi = new File(_home, "midi");
	@Getter static final File PresetsFile = new File(_home, "presets.zone");
	@Getter static final File SynthPresets = new File(_home, "synths.zone");
	
    static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
    public static final File ICONS = new File(ROOT, "icons");
	
}
