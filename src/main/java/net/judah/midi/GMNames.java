package net.judah.midi;

import java.security.InvalidParameterException;

/*
 * jVSTwRapper - The Java way into VST world!
 *
 * jVSTwRapper is an easy and reliable Java Wrapper for the Steinberg VST interface.
 * It enables you to develop VST 2.3 compatible audio plugins and virtual instruments
 * plus user interfaces with the Java Programming Language. 3 Demo PluginLibrary(+src) are included!
 *
 * Copyright (C) 2006  Daniel Martin [daniel309@users.sourceforge.net]
 * 					   and many others, see CREDITS.txt
 *
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

public class GMNames {
  public static final int NUM_GM_CATEGORIES = 17;

  public static final String[] GM_CATEGORIES = new String[] {
    "Piano",
    "Percussion",
    "Organ",
    "Guitar",
    "Bass",
    "Strings",
    "Ensemble",
    "Brass",
    "Reed",
    "Pipe",
    "Synth Lead",
    "SynthPad",
    "Synth Effects",
    "Ethnic",
    "Percussive",
    "Effects",
    "DrumSets"
  };

  public static final int GM_CATEGORIES_FIRST_INDICES[] = new int[] {
    0, 7, 16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96, 104, 112, 120, 128
  };


  public static final String[] GM_NAMES = new String[] {
    // Piano
    "Grand Piano",
    "Bright Piano",
    "Elec. Rhodes",
    "Honkytonk",
    "E Piano 1",
    "E Piano 2",
    "Harpsichord",

    // Percussion
    "Clavinet",	                // 7
    "Celesta",
    "Glockenspiel",
    "Music Box",
    "Vibraphone",
    "Marimba",
    "Xylophone",
    "Tube Bells",
    "Dulcimer",

    // Organ
    "Draw Organ",                // 16
    "Perc Organ",
    "Rock Organ",
    "Church Organ",
    "Reed Organ",
    "Accordion",
    "Harmonica",
    "Tango Accordion",

    // Gitar
    "Nylon Gtr",      // 24
    "Steel Gtr",
    "Jazz Gtr",
    "Clean Gtr",
    "Muted Gtr",
    "Drive Gtr",
    "Dist Gtr",   
    "Harmonics",
    // Bass
    "Acoustic Bass",		// 32
    "Finger Bass",
    "Pick Bass",
    "Fretless",
    "Slap Bass 1",
    "Slap Bass 2",
    "Synth Bass 1",
    "Synth Bass 2",

    // strings
    "Violin",			// 40
    "Viola",
    "Cello",
    "Contrabass",
    "Tremolo ",
    "Pizzicato ",
    "Harp",
    "Timpani",

    // Ensemble
    "Strings",		// 48
    "Slow Strings",
    "Syn Str 1",
    "Syn Str 2",
    "Choir Aahs",
    "Voice Oohs",
    "Synth Voice",
    "Orchestra Hit",

    // Brass
    "Trumpet",			// 56
    "Trombone",
    "Tuba",
    "Mute Trumpet",
    "French Horn",
    "Brass Section",
    "SynthBrass 1",
    "SynthBrass 2",

    // Reed
    "Soprano Sax",			// 64
    "Alto Sax",
    "Tenor Sax",
    "Bari Sax",
    "Oboe",
    "English Horn",
    "Bassoon",
    "Clarinet",

    // Pipe
    "Piccolo",			// 72
    "Flute",
    "Recorder",
    "Pan Flute",
    "Bottle",
    "Shakuhachi",
    "Whistle",
    "Ocarina",

    // Synth Lead
    "L1 (square)",		// 80
    "L2 (sawtooth)",
    "L3 (calliope)",
    "L4 (chiff)",
    "L5 (charang)",
    "L6 (voice)",
    "L7 (fifths)",
    "L8 (bass+lead)",

    // Synth Pad
    "P1 (new age)",		// 88
    "P2 (warm)",
    "P3 (polysynth)",
    "P4 (choir)",
    "P5 (bowed)",
    "P6 (metallic)",
    "P7 (halo)",
    "P8 (sweep)",

    // Synth Fx
    "FX 1 (rain)",			// 96
    "FX 2 (soundtrack)",
    "FX 3 (crystal)",
    "FX 4 (atmosphere)",
    "FX 5 (brightness)",
    "FX 6 (goblins)",
    "FX 7 (echoes)",
    "FX 8 (sci-fi)",

    // Ethnic
    "Sitar",			// 104
    "Banjo",
    "Shamisen",
    "Koto",
    "Kalimba",
    "Bag pipe",
    "Fiddle",
    "Shanai",

    // Percussive
    "Tinkle Bell",			// 112
    "Agogo",
    "Steel Drums",
    "Woodblock",
    "Taiko Drum",
    "Melodic Tom",
    "Synth Drum",
    "Reverse Cymbal",

    // Effects
    "Guitar Fret Noise",		// 120
    "Breath Noise",
    "Seashore",
    "Bird Tweet",
    "Telephone Ring",
    "Helicopter",
    "Applause",
    "Gunshot"
  };

  public static final String[] GM_DRUMSETS = new String[] {
    "Standard",
    "Room",
    "Power",
    "Electronic",
    "Analog",
    "Jazz",
    "Brush",
    "Orchestra",
    "Clavinova",
    "RX",
    "C/M"
  };


  	public static int indexOf(String instrument) {
		for (int i = 0; i < GM_NAMES.length; i++)
			if (GM_NAMES[i].equals(instrument))
				return i;
		throw new InvalidParameterException(instrument);
	}

  
}


/* (subtract 1)
1	Standard Drum Kit
9	Room Drum Kit
17	Power Drum Kit
25	Electric Drum Kit
26	Rap TR808 Drums
33	Jazz Drum Kit
41	Brush Kit

Note #	Note Name	Drum Sound   // 27 lowest
35	B2	Acoustic Bass Drum
36	C3	Bass Drum 1
37	Db3/C#3	Side Stick
38	D3	Acoustic Snare
39	Eb3/D#3	Hand Clap
40	E3	Electric Snare
41	F3	Low Floor Tom
42	Gb3/F#3	Closed Hi-Hat
43	G3	High Floor Tom
44	Ab3/G#3	Pedal Hi-Hat
45	A3	Low Tom
46	Bb3/A#3	Open Hi-Hat
47	B3	Low-Mid Tom
48	C4	Hi-Mid Tom
49	Db4/C#4	Crash Cymbal 1
50	D4	High Tom
51	Eb4/D#4	Ride Cymbal 1
52	E4	Chinese Cymbal
53	F4	Ride Bell
54	Gb4/F#4	Tambourine
55	G4	Splash Cymbal
56	Ab4/G#4	Cowbell
57	A4	Crash Symbol 2
58	Bb4/A#4	Vibraslap
59	B4	Ride Cymbal 2
60	C5 (middle C)	Hi Bongo
61	Db5/C#5	Low Bongo
62	D5	Mute Hi Conga
63	Eb5/D#5	Open Hi Conga
64	E5	Low Conga
65	F5	High Timbale
66	Gb5/F#5	Low Timbale
67	G5	High Agogo
68	Ab5/G#5	Low Agogo
69	A5	Cabasa
70	Bb5/A#5	Maracas
71	B5	Short Whistle
72	C6	Long Whistle
73	Db6/D#6	Short Guiro
74	D6	Long Guiro
75	Eb6/D#6	Claves
76	E6	Hi Wood Block
77	F6	Low Wood Block
78	Gb6/F#6	Mute Cuica
79	G6	Open Cuica
80	Ab6/G#6	Mute Triangle
81	A6	Open Triangle
82	Bb6/A#6	Shaker

 */
