package net.judah.util;

import java.io.File;

import javax.swing.JFileChooser;

import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;

@Log4j
public class FileChooser {
	
	static File currentDir = JudahZone.defaultFolder;
	
	public static void setCurrentDir(File folder) {
		currentDir = folder;
	}
	
	public static File choose() {
		JFileChooser fc = new JFileChooser();
	    //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (currentDir != null && currentDir.isDirectory())
			fc.setCurrentDirectory(currentDir);
		else fc.setCurrentDirectory(new File(System.getProperty("user.home")));

		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = fc.getSelectedFile();
		    log.debug("Selected file: " + selectedFile.getAbsolutePath());
		    return selectedFile;
		}
		return null;
	}
}
