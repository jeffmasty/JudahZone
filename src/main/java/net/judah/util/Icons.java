package net.judah.util;

import java.io.File;
import java.util.HashMap;

import javax.swing.ImageIcon;

/* https://www.elharrakfonts.com/2019/04/font-bottons-music-pro.html */
public class Icons {

	private static final HashMap<String, ImageIcon> map = new HashMap<>();
	
    public static ImageIcon load(String name) {
    	if (map.get(name) == null) {
    		try {
    			map.put(name, new ImageIcon(new File(Constants.ICONS, name).toURI().toURL()));
    		} catch (Exception e) {
    			RTLogger.warn("Icons " + name, e);
    		}
    	}
        return map.get(name);
    }

}
