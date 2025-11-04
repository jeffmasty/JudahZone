package net.judah.seq;

import javax.sound.midi.MetaMessage;

import lombok.RequiredArgsConstructor;

public interface MidiConstants {

	@RequiredArgsConstructor
	enum Meta {
		SEQ_NUM(0),
		TEXT(1),
		COPYRIGHT(2),
		TRACK_NAME(3),
		INSTRUMENT(4),
		LYRICS(5), // MARKER(6), CUE(7),
		DEVICE(9),
		CHANNEL(0x20), // 0..255
		PORT(0x21), // 0..255
		EOT(0x2f),
		SET_TEMPO(0x51),	//bytes:3	The number of microseconds per beat	Anywhere, but usually in the first track
		SMPTE_OFFSET(0x54),	//bytes:5	SMPTE time to denote playback offset from the beginning	beginning of a track/first track of MIDI format type 1
		TIME_SIG(0x58),	//bytes:4	Time signature, metronome clicks, and size of a beat in 32nd notes	Anywhere
		KEY_SIG(0x59)   //bytes:2  "C"  "F#m"
		;

		public static Meta getType(MetaMessage m) {
			int target = m.getType();
			for (Meta met : Meta.values())
				if (met.type == target)
					return met;
			return null;
		}

		final int type;
	}


	int NOTE_ON = 0x90;
	int NOTE_OFF = 0x80;
	//int NAME_STATUS = 73;

	int MIDDLE_C = 60;
	int DRUM_CH = 9;

}

/* CC List
Hex Decimal Controller	            Values	Required by
0x00	0	Bank select (coarse)	0-127	GM21
0x01	1	Modulation wheel (coarse)	0-127	GM1, GM2
0x02	2	Breath controller (coarse)	0-127
0x04	4	Foot controller (coarse)	0-127
0x05	5	Portamento time (coarse)	0-127	GM2
0x06	6	Data entry (coarse)	0-127	GM1, GM2
0x07	7	Channel volume (coarse) (formerly main volume)	0-127	GM1, GM2
0x08	8	Balance (coarse)	0-127
0x0A	10	Pan (coarse)	0-127	GM1, GM2
0x0B	11	Expression (coarse)2	0-127	GM1, GM2
0x0C	12	Effect control 1 (coarse)	0-127
0x0D	13	Effect control 2 (coarse)	0-127
0x10	16	General purpose controller 1 (coarse)	0-127
0x11	17	General purpose controller 2 (coarse)	0-127
0x12	18	General purpose controller 3 (coarse)	0-127
0x13	19	General purpose controller 4 (coarse)	0-127
0x20	32	Bank select (fine)	0-127	GM2
0x21	33	Modulation wheel (fine)	0-127
0x22	34	Breath controller (fine)	0-127
0x24	36	Foot controller (fine)	0-127
0x25	37	Portamento time (fine)	0-127
0x26	38	Data entry (fine)	0-127	GM1, GM2
0x27	39	Channel volume (fine) (formerly main volume)	0-127
0x28	40	Balance (fine)	0-127
0x2A	42	Pan (fine)	0-127
0x2B	43	Expression (fine)2	0-127
0x2C	44	Effect control 1 (fine)	0-127
0x2D	45	Effect control 2 (fine)	0-127
0x40	64	Hold (damper, sustain) pedal 1 (on/off)	< 63 is off, >= 64 is on	GM1, GM2
0x41	65	Portamento pedal (on/off)	< 63 is off, >= 64 is on	GM2
0x42	66	Sostenuto pedal (on/off)	< 63 is off, >= 64 is on	GM2
0x43	67	Soft pedal (on/off)	< 63 is off, >= 64 is on	GM2
0x44	68	legato pedal (on/off)	< 63 is off, >= 64 is on
0x45	69	Hold pedal 2 (on//off)	< 63 is off, >= 64 is on
0x46	70	Sound controller 1 (default is sound variation)	0-127
0x47	71	Sound controller 2 (default is timbre / harmonic intensity / filter resonance)	0-127	GM2
0x48	72	Sound controller 3 (default is release time)	0-127	GM2
0x49	73	Sound controller 4 (default is attack time)	0-127	GM2
0x4A	74	Sound controller 5 (default is brightness or cutoff frequency)	0-127	GM2
0x4B	75	Sound controller 6 (default is decay time)	0-127	GM2
0x4C	76	Sound controller 7 (default is vibrato rate)	0-127	GM2
0x4D	77	Sound controller 8 (default is vibrato depth)	0-127	GM2
0x4E	78	Sound controller 9 (default is vibrato delay)	0-127	GM2
0x4F	79	Sound controller 10 (default is undefined)	0-127
0x50	80	General purpose controller 5	0-127
0x51	81	General purpose controller 6	0-127
0x52	82	General purpose controller 7	0-127
0x53	83	General purpose controller 8	0-127
0x54	84	Portamento control	0-127
0x58	88	High resolution velocity prefix	0-127
0x5B	91	Effect 1 depth (default is reverb send level, formerly external effect depth)	0-127	GM2
0x5C	92	Effect 2 depth (formerly tremolo depth)	0-127
0x5D	93	Effect 3 depth (default is chorus send level, formerly chorus depth)	0-127	GM2
0x5E	94	Effect 4 depth (formerly celeste depth)	0-127
0x5F	95	Effect 5 depth (formerly phaser level)	0-127
0x60	96	Data button increment
0x61	97	Data button decrement
0x62	98	Non-registered parameter (coarse)	0-127
0x63	99	Non-registered parameter (fine)	0-127
0x64	100	Registered parameter (coarse)	0-127	GM1, GM2
0x65	101	Registered parameter (fine)	0-127	GM1, GM2

MIDI CC 120 to 127 are “Channel Mode Messages.”
0x78	120	All sound off	0
0x79	121	All controllers off	0	GM1, GM2
0x7A	122	Local control (on/off)	0 off, 127 on
0x7B	123	All notes off	0	GM1, GM2
0x7C	124	Omni mode off	0
0x7D	125	Omni mode on	0
0x7E	126	Mono operation and all notes off
0x7F	127	Poly operation and all notes off	0

Undefined CCs:
CC 3
CC 9
CC 14-15
CC 20-31
CC 85-87
CC 89-90
CC 102-119
 */


// Per Track, Per Scene (with tick)
// Pattern_Start 0-127
// PATTERN_END   0-127
