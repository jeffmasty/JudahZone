package net.judah.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import lombok.Getter;
import net.judah.api.Signature;

public class Folders { // TODO Folder/Settings Service not hardcoded

    static final File HOME = new File(System.getProperty("user.home"));
	static final File LIVE = new File(HOME, "Setlist");

	@Getter static final File SetlistHome = new File(LIVE, "songs");

	@Getter static final File Beats = new File(LIVE, "beats");
	@Getter static final File ChordPro = new File(LIVE, "chords");
	@Getter static final File IR = new File(LIVE, "IR");
	@Getter static final File Kits = new File(LIVE, "kits");
	@Getter static final File Loops = new File(LIVE, "loops");
	@Getter static final File Midi = new File(LIVE, "midi");
	@Getter static final File PresetsFile = new File(LIVE, "presets.zone");
	@Getter static final File Samples = new File(LIVE, "samples");
	@Getter static final File Synths = new File(LIVE, "synths");
	@Getter static final File SheetMusic = new File(LIVE, "sheets");
	@Getter static final File SynthPresets = new File(LIVE, "synths.zone");
//	@Getter static final File Bass = new File(LIVE, "bass"); // monosynth

	@Getter static final File ImportDrums = new File(HOME, "/tracks/beatbuddy/");
	@Getter static final File ImportMidi = new File(HOME, "/tracks/midi/");

	static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
	@Getter static final File Log4j = new File(ROOT, "log4j.xml");

    public static File midi(File parent, Signature time) {
    	return new File(parent, time.name());
    }

	public static File[] sort(File folder) {
		File[] result = folder.listFiles();
		Arrays.sort(result);
		return result;
	}

	static HashMap<File, File> memory = new HashMap<File, File>();

	public static File choose(int selectionMode, final String extension, final String description) {
		return choose(selectionMode, extension, description, new File(System.getProperty("user.home")));
	}

	public static File choose(String ext, String desc, File folder) {
		return choose(JFileChooser.FILES_AND_DIRECTORIES, ext, desc, folder);
	}

	public static File choose(int selectionMode, final String extension, final String description, File folder) {
		JFileChooser fc = new JFileChooser();
		if (selectionMode >= 0 && selectionMode <= 2)
			fc.setFileSelectionMode(selectionMode);
		if (extension != null)
			fc.setFileFilter(new FileFilter() {
				@Override public String getDescription() {
					return description; }
				@Override public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(extension); }
			});

		if (memory.containsKey(folder))
			fc.setCurrentDirectory(memory.get(folder));
		else
			fc.setCurrentDirectory(folder);

		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = fc.getSelectedFile();
		    memory.put(folder, fc.getCurrentDirectory());
		    return selectedFile == null ? null :
		    	new File(selectedFile.getAbsolutePath()); // JSON doesn't like File subclass
		}
		return null;
	}

	public static File choose() {
		return choose(0, null, null);
	}
	public static File choose(File folder) {
		return choose(0, null, null, folder);
	}


}
