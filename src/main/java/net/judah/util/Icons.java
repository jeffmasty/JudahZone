package net.judah.util;

import java.util.HashMap;

import javax.swing.ImageIcon;

/* https://www.elharrakfonts.com/2019/04/font-bottons-music-pro.html */
public class Icons {

	private static final HashMap<String, ImageIcon> map = new HashMap<>();
	
    public static ImageIcon load(String name) {
    	if (map.get(name) == null) {
    		String path = Constants.ICONS + name;
    		map.put(name, new ImageIcon(path));
    	}
        return map.get(name);
    }

}
