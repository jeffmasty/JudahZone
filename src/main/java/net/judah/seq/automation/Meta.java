package net.judah.seq.automation;

import javax.sound.midi.MetaMessage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public
enum Meta {
	SEQ_NUM(0),
	TEXT(1),
	COPYRIGHT(2),
	TRACK_NAME(3),
	INSTRUMENT(4),
	LYRICS(5),
	MARKER(6), // i.e., verse, chorus intro
	CUE(7), // i.e., Solo... (red)
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