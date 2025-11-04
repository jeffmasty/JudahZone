package net.judah.song.cmd;

import static net.judah.JudahZone.getMidiGui;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Cmd {

	Start, Tempo, Length, Swing,
	Bars, Beats, Steps, // Scene sequencer
	MPK,
	Jump,// TimeCode, Absolute/Relative scene cues
	Part, Chords, // ChordTrack
	Record, RecEnd, Sync, Dup, Delete, Solo, SoloCh, // Looper
	FX, Latch, FadeOut, FadeIn, Mute, Unmute, // Channel
	OffTape, OnTape; // LineIn

	public static Cmdr getCmdr(Cmd cmd) {
		switch (cmd) {

		case MPK:		return getMidiGui();
		case Length:	return ClockCmd.getLength();
		case Start:		return ClockCmd.getStart();
		case Swing: 	return ClockCmd.getSwing();
		case Tempo:		return ClockCmd.getTempo();
		case Jump:		return SceneCmd.getInstance();
		case Bars: 		return IntProvider.instance();  // scene sequencer
		case Beats: 	return IntProvider.instance();  // scene sequencer
		case Part: 		return ChordCmd.getInstance();
		case Chords: 	return ChordCmd.getPlayer();
		case Solo:		return LoopCmd.getSolo();
		case Sync:		return LoopCmd.getSync();
		case Record:
		case RecEnd:
		case Delete:
		case Dup:	 	return LoopCmd.getInstance();
		case OffTape:
		case OnTape:
		case SoloCh:	return MixCmd.getLine();
		case Latch:
		case Mute:
		case Unmute:
		case FadeIn:
		case FadeOut:
		case FX:		return MixCmd.getInstance();

		default: return null;
		}
	}

}
