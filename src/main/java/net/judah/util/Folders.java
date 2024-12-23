package net.judah.util;

import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import lombok.Getter;
import net.judah.JudahZone;

public class Folders {

	// TODO user settings gui/mkdir
	static final File _home = new File(System.getProperty("user.home"), "Setlist");

	@Getter static final File SetlistHome = new File(_home, "songs");
	@Getter static final File Samples = new File(_home, "samples");
	@Getter static final File Kits = new File(_home, "kits");
	@Getter static final File Synths = new File(_home, "synths");
	@Getter static final File Beats = new File(_home, "beats");
	@Getter static final File Bass = new File(_home, "bass");
	@Getter static final File SheetMusic = new File(_home, "sheets");
	@Getter static final File ChordPro = new File(_home, "chords");
	@Getter static final File Loops = new File(_home, "loops");
	@Getter static final File PresetsFile = new File(_home, "presets.zone");
	@Getter static final File SynthPresets = new File(_home, "synths.zone");
    // TODO not hardcoded
	static final File ROOT = new File("/home/judah/git/JudahZone/resources/");
    @Getter static final File Log4j = new File(ROOT, "log4j.xml");
    @Getter static final File ICONS = new File(ROOT, "icons");
	@Getter static final File ImportDrums = new File("/home/judah/tracks/beatbuddy/");
	@Getter static final File ImportMidi= new File("/home/judah/tracks/midi/");

    public static File midi(File parent) {
    	return new File(parent, JudahZone.getClock().getTimeSig().name());
    }

	public static File[] sort(File folder) {
		File[] result = folder.listFiles();
		Arrays.sort(result);
		return result;
	}

	static File currentDir = new File(System.getProperty("user.dir"));

	public static void setCurrentDir(File folder) {
		currentDir = folder;
	}
	public static void setCurrentFile(File file) {
		currentDir = file;
	}

	public static File choose(int selectionMode, final String extension, final String description) {
		JFileChooser fc = new JFileChooser();
		if (selectionMode >= 0)
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (extension != null)
			fc.setFileFilter(new FileFilter() {
				@Override public String getDescription() {
					return description; }
				@Override public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(extension); }
			});

		fc.setCurrentDirectory(new File(System.getProperty("user.home")));
		if (currentDir != null && currentDir.isDirectory())
			fc.setCurrentDirectory(currentDir);
		else if (currentDir != null && currentDir.isFile())
			fc.setSelectedFile(currentDir);
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = fc.getSelectedFile();
		    currentDir = fc.getCurrentDirectory();
		    return selectedFile == null ? null :
		    	new File(selectedFile.getAbsolutePath()); // JSON doesn't like File subclass
		}
		return null;
	}

	public static File choose() {
		return choose(0, null, null);
	}
	public static File choose(File folder) {
		setCurrentDir(folder);
		return choose();
	}


}
