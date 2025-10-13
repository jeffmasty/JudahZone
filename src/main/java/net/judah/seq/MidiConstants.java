package net.judah.seq;

public interface MidiConstants {

	int NOTE_ON = 0x90;
	int NOTE_OFF = 0x80;
	int NAME_STATUS = 73;

	int VELOCITY = 99;
	int RATCHET = 1;
	int MIDDLE_C = 60;

	int DRUM_CH = 9;

	//// META_MESSAGE Types
	///
	int SEQUENCE_NUM = 0;


	int TEXT = 1;
	int TRACK_NAME = 3;
	int INSTRUMENT = 4;
	int SET_TEMPO = 0x51;	//bytes: 3	The number of microseconds per beat	Anywhere, but usually in the first track
	int SMPTE_OFFSET = 0x54;	//bytes: 5	SMPTE time to denote playback offset from the beginning	At the beginning of a track and in the first track of files with MIDI format type 1
	int TIME_SIG = 0x58;	//bytes:  4	Time signature, metronome clicks, and size of a beat in 32nd notes	Anywhere
	int KEY_SIG = 0x59;   //bytes:  2
}
