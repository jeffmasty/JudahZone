package net.judah.song.cmd;

import static net.judah.JudahZone.*;

import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.looper.LoopCmdr;
import net.judah.looper.Looper;

@RequiredArgsConstructor
public enum Cmd {

	Start, Tempo, Length, MPK, // Clock
	TimeCode, Jump,// Absolute/Relative scene cues
	Record, RecEnd, Sync, Dup, Delete, Solo, SoloCh, // Looper
	FX, Latch, FadeOut, FadeIn, Mute, Unmute, // Channel
	OffTape, OnTape, // LineIn
	Part, Chords // ChordTrack
	;

	public static Cmdr getCmdr(Cmd cmd) {
		switch (cmd) {

		case MPK:		return getSeq().getSynthTracks();
		case Length:	return getSeq();
		case TimeCode:	return IntProvider.instance();
		case Start:		return BooleanProvider.instance;
		case Tempo:		return getClock();
		case Jump:		return SceneProvider.getInstance();

		case OffTape:	return getInstruments();
		case OnTape:	return getInstruments();
		case SoloCh:	return getInstruments();
		case Latch:		return getInstruments();

		case Record:	return getLoops();
		case RecEnd:	return getLoops();
		case Sync:		if (sinker == null) sinker = new Sinker(getLooper()); return sinker;
		case Delete: 	return getLoops();
		case Dup:	 	return getLoops();
		case Solo:		return getLooper().getSoloTrack();

		case Mute:		return getMixer();
		case Unmute:	return getMixer();
		case FadeIn:	return getMixer();
		case FadeOut:	return getMixer();
		case FX:		return getMixer();

		case Part: 		return getChords();
		case Chords: 	return getChords().getPlayer();

		default: return null;
		}
	}

	private static LoopCmdr loops;
	private static LoopCmdr getLoops() {
		if (loops == null)
			loops = new LoopCmdr(getLooper());
		return loops;
	}

	private static Sinker sinker;
	@RequiredArgsConstructor
	private static class Sinker implements Cmdr {
    	private String[] keys;
    	private final Looper looper;
    	@Override public Object resolve(String key) { return getLoops().resolve(key); }
		@Override public String[] getKeys() {
			if (keys == null) {
				keys = new String[getLoops().getKeys().length + 1];
				keys[keys.length - 1] = "Tempo";
				for (int i = 0; i < getLoops().getKeys().length; i++)
					keys[i] = getLoops().getKeys()[i];
			}
			return keys;
		}

		@Override public void execute(Param p) {
			if (resolve(p.val) == null && p.cmd == Cmd.Sync)
				JudahZone.getClock().syncTempo(looper.getPrimary());
			else
				getLoops().execute(p);
		}
	}



}
