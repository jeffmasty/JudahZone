package net.judah.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;

public class Folders {

	// TODO not hardcoded
	static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
    public static final File ICONS = new File(ROOT, "icons");
    public static final File DRUM_IMPORT = new File("/home/judah/tracks/beatbuddy/");
    public static final File PIANO_IMPORT = new File("/home/judah/tracks/midi");
    
	// TODO user.dir settings gui / mkdir
	static final File _home = new File(System.getProperty("user.home"), "Setlist");

	@Getter static final File Tracks = new File(_home, "tracks");
	@Getter static final File Samples = new File(_home, "samples");
	@Getter static final File Kits = new File(_home, "kits");
	@Getter static final File SheetMusic = new File(_home, "sheets");
	@Getter static final File Midi = new File(_home, "midi");
	@Getter static final File PresetsFile = new File(_home, "presets.zone");
	@Getter static final File SynthPresets = new File(_home, "synths.zone");
	static final File SetlistHome = new File(_home, "songs");
    public static File[] getSetlists() {
    	return SetlistHome.listFiles(new FileFilter() {
			@Override public boolean accept(File pathname) {
				return pathname.isDirectory();
			}});}
    
    public static File getImport(boolean isDrums) {
    	return isDrums ? DRUM_IMPORT : PIANO_IMPORT;
    }
    	
    @Setter @Getter private static File setlist = getSetlists()[0];
    public static File[] sortSetlist() {
    	File[] result = setlist.listFiles(); 
    	Arrays.sort(result);
    	return result;
    }

    
    @Getter static final File Log4j = new File(ROOT, "log4j.xml");

}
