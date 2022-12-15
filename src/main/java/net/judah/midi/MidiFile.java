package net.judah.midi;

import java.io.File;
import java.util.HashMap;

import javax.sound.midi.*;
// https://stackoverflow.com/questions/3850688/reading-midi-files-in-java

public class MidiFile {
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final String[] NOTE_NAMES = {"C", "C#", "D", "Db", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"};

    
    public static void main(String[] args) throws Exception {
    	HashMap<Integer, Integer> count = new HashMap<>();

    	File folder = new File("/home/judah/tracks/midi/");
        for (File f : folder.listFiles())
        	if (f.isFile() && f.getName().endsWith(".mid")) {
        		try {
        			Sequence sequence = MidiSystem.getSequence(f);
        			int rez = sequence.getResolution();
        			// System.out.println(f.getName() + " tracks:" + sequence.getTracks().length + " rez:" + rez);
        			if (count.get(rez) == null)
        				count.put(rez, 1);
        			else 
        				count.put(rez, count.get(rez) + 1);
        		} catch (Exception e) {
        			System.err.println(f.getName() + " " + e.getMessage());
        		}
        	}
        for (int i : count.keySet())
        	System.out.println("rez: " + i + " count: " + count.get(i));
    }

    public static void outputSeq(Sequence sequence) {
    	int trackNumber = 0;
        for (Track track :  sequence.getTracks()) {
            trackNumber++;
            System.out.println("Track " + trackNumber + ": size = " + track.size());
            System.out.println();
            for (int i=0; i < track.size(); i++) { 
                MidiEvent event = track.get(i);
                System.out.print("@" + event.getTick() + " ");
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    System.out.print("Channel: " + sm.getChannel() + " ");
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note on, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else if (sm.getCommand() == NOTE_OFF) {
                        int key = sm.getData1();
                        int octave = (key / 12)-1;
                        int note = key % 12;
                        String noteName = NOTE_NAMES[note];
                        int velocity = sm.getData2();
                        System.out.println("Note off, " + noteName + octave + " key=" + key + " velocity: " + velocity);
                    } else {
                        System.out.println("Command:" + sm.getCommand());
                    }
                } else {
                    System.out.println("Other message: " + message.getClass());
                }
            }

            System.out.println();
        }
    }
    
}