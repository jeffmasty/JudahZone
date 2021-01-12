package net.judah.util;

import javax.swing.ImageIcon;

import lombok.Getter;

/* https://www.elharrakfonts.com/2019/04/font-bottons-music-pro.html */
public class Icons {

	private static ClassLoader loader = Icons.class.getClassLoader();
	
	public static final Pair MUTE = new Pair("Mute");
	public static final Pair MUTE_RECORD = new Pair("MuteRecord");
	public static final Pair PLAY = new Pair("Play");
	public static final Pair MICROPHONE = new Pair("Microphone");
	
	public static ImageIcon load(String filename) {
		return new ImageIcon(loader.getResource("icons/" + filename));
	}
	
	public static class Pair {
		 
		@Getter private final ImageIcon active;
		@Getter private final ImageIcon inactive;
		@Getter private final String name;
		
		public Pair(String name) {
			active = new ImageIcon(loader.getResource("icons/" + name + "Active.png"));
			inactive = new ImageIcon(loader.getResource("icons/" + name + "Inactive.png"));
			this.name = name;
		}
		
		@Override
			public boolean equals(Object obj) {
				return name.equals("" + obj);
			}
		
		@Override public String toString() {
			return name;
		}
	}
}
