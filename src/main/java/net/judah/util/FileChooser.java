package net.judah.util;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileChooser {
	
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
		    return selectedFile;
		}
		return null;
	}
	
	public static File choose() {
		return choose(0, null, null);
	}

}
