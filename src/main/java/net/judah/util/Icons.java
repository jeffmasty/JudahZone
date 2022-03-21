package net.judah.util;

import java.util.Objects;

import javax.swing.ImageIcon;

import lombok.Getter;

/* https://www.elharrakfonts.com/2019/04/font-bottons-music-pro.html */
public class Icons {

    public static final Pair MUTE = new Pair("Mute");
    public static final Pair MUTE_RECORD = new Pair("MuteRecord");
    public static final Pair PLAY = new Pair("Play");
    public static final Pair MICROPHONE = new Pair("Recording");
    public static final Pair FADE = new Pair("Fade");
    // public static final Pair RECORDING = new Pair("Recording");

    public static ImageIcon load(String filename) {
        return new ImageIcon("/home/judah/git/JudahZone/resources/icons/" + filename);
    }

    public static class Pair {

        @Getter private final ImageIcon active;
        @Getter private final ImageIcon inactive;
        @Getter private final String name;

        public Pair(String name) {
            active = new ImageIcon("/home/judah/git/JudahZone/resources/icons/" + name + "Active.png");
            inactive = new ImageIcon("/home/judah/git/JudahZone/resources/icons/" + name + "Inactive.png");
            this.name = name;
        }

        @Override
		public int hashCode() {
			return Objects.hash(active, name);
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
