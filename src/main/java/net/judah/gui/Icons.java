package net.judah.gui;

import java.io.File;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import net.judah.util.Folders;
import net.judah.util.RTLogger;

/* https://www.elharrakfonts.com/2019/04/font-bottons-music-pro.html */
public class Icons {

	public static final Icon SAVE = UIManager.getIcon("FileView.floppyDriveIcon"); 
	public static final Icon NEW_FILE = UIManager.getIcon("FileView.fileIcon");
	public static final Icon DETAILS_VEW = UIManager.getIcon("FileChooser.detailsViewIcon");
	public static final Icon HOME = UIManager.getIcon("FileChooser.homeFolderIcon");
	
	private static final HashMap<String, ImageIcon> map = new HashMap<>();
	
	/** load or retrieve from cache */
    public static ImageIcon get(String name) {
    	if (map.get(name) == null) {
    		try {
    			map.put(name, new ImageIcon(new File(Folders.ICONS, name).toURI().toURL()));
    		} catch (Exception e) {
    			RTLogger.warn("Icons " + name, e);
    		}
    	}
        return map.get(name);
    }

}
