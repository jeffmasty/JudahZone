package net.judah.util;

import java.io.File;
import java.util.Arrays;

import lombok.Getter;
import net.judah.JudahZone;

public class Folders {

	// TODO not hardcoded
	static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
    public static final File ICONS = new File(ROOT, "icons");
    public static final File DRUM_IMPORT = new File("/home/judah/tracks/beatbuddy/");
    public static final File PIANO_IMPORT = new File("/home/judah/tracks/midi");
    
	// TODO user.dir settings gui / mkdir
	static final File _home = new File(System.getProperty("user.home"), "Setlist");

	@Getter static final File Samples = new File(_home, "samples");
	@Getter static final File Kits = new File(_home, "kits");
	@Getter static final File SheetMusic = new File(_home, "sheets");
	@Getter static final File Synths = new File(_home, "synths");
	@Getter static final File Beats = new File(_home, "beats");
	@Getter static final File Bass = new File(_home, "bass");
	
	@Getter static final File PresetsFile = new File(_home, "presets.zone");
	@Getter static final File SynthPresets = new File(_home, "synths.zone");
	@Getter static final File ChordPro = new File(_home, "chordpro");

	@Getter static final File SetlistHome = new File(_home, "songs");
    
    public static File getImport(boolean isDrums) {
    	return isDrums ? DRUM_IMPORT : PIANO_IMPORT;
    }
    	
    public static File midi(File parent) {
    	return new File(parent, JudahZone.getClock().getTimeSig().name());
    }
    
    @Getter static final File Log4j = new File(ROOT, "log4j.xml");

    public static File[] sort(File folder) {
    	File[] result = folder.listFiles(); 
    	Arrays.sort(result);
    	return result;
	}

}
