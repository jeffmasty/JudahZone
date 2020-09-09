package net.judah.util;

import java.io.File;

import javax.swing.JFileChooser;

import lombok.extern.log4j.Log4j;

@Log4j
public class FileChooser {
	
	
	public static File choose() {
		JFileChooser fc = new JFileChooser();
	    //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setCurrentDirectory(new File(System.getProperty("user.home")));
		int result = fc.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = fc.getSelectedFile();
		    log.debug("Selected file: " + selectedFile.getAbsolutePath());
		    return selectedFile;
		}
		return null;
	}
}
