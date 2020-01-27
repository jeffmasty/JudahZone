package net.judah.looper.old;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.metronome.Quantization;

@AllArgsConstructor @RequiredArgsConstructor @EqualsAndHashCode
public class LoopSettings implements Serializable {
	private static final long serialVersionUID = 597188651109196197L;
	
	public static enum Kickoff {
		PressRecord, JackTransport, MidiTransport
	}
	
	public static enum PostRecordAction {
		NOTHING, PLAY, OVERDUB, DUB_AND_PLAY
	}
	
	@Getter private final String name;
	@Getter private final Class<?> type;
	/** beats per minute, 0 indicates unmanaged */
	@Getter float bpm = 0f;
	/** notes per measure, 0 indicates unmanaged */
	@Getter int npm = 0;
	/** bars per loop, 0 indicates unmanaged */
	@Getter int bpl = 0;
	@Getter Quantization quantization;
	/** cause metronome to play 1 measure before recording */
	@Getter boolean kickstart;
	/** how will recording begin? */
	@Getter Kickoff kickoff = Kickoff.PressRecord;
	/** after first recording, start to immediately record another loop? */
	@Getter PostRecordAction postRecordAction = PostRecordAction.PLAY;
	/** should this loop process audio inputs to outputs by default? */
	@Getter boolean playLiveMic = true;
	/** should playback switch between loops automatically and at the loop boundary? */
	@Getter boolean verseChorusLooper;
	@Getter boolean alwaysPlayFromStart = true;
	
}
