package net.judah.song;

import static net.judah.JudahZone.*;
import static net.judah.song.Param.Type.*;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.song.Param.Type;

// TODO ProgChange/CC in Midi file
@RequiredArgsConstructor
public enum Cmd {

	Start(CLOCK), Tempo(CLOCK), Length(CLOCK), MPK(CLOCK), 
	TimeSig(CLOCK), TimeCode(CLOCK), Jump(CLOCK),// Absolute/Relative scene cues
	Record(LOOP), RecEnd(LOOP), Sync(LOOP), Dup(LOOP), Delete(LOOP), Solo(LOOP), SoloCh(LOOP), // looper
	// FXOFF
	FX(CH), Latch(CH), FadeOut(CH), FadeIn(CH), Mute(CH), Unmute(CH), 
	OffTape(CH), OnTape(CH) // fx/channel 
	;  
	
	@Getter private final Type type;
	
	public static List<Cmd> get(Type type) {
		ArrayList<Cmd> result = new ArrayList<Cmd>();
		for (Cmd cmd : values())
			if (cmd.type == type)
				result.add(cmd);
		return result;
	}
	
	public static Cmdr getCmdr(Cmd cmd) {
		switch (cmd) {

		case MPK:		return getSeq().getSynthTracks();
		case Length:	return getSeq();
		case TimeCode:	return IntProvider.instance();
		case TimeSig:	return SigProvider.instance;
		case Start:		return BooleanProvider.instance;
		case Tempo:		return getClock().getMidiClock(); 
		case Jump:		return getSongs();

		case OffTape:	return getInstruments();
		case OnTape:	return getInstruments();
		case SoloCh:	return getInstruments();
		case Latch:		return getInstruments();

		case Record:	return getLooper();
		case RecEnd:	return getLooper();
		case Sync:		return getLooper();
		case Delete: 	return getLooper();
		case Dup:	 	return getLooper();
		case Solo:		return getLooper().getSoloTrack();

		case Mute:		return getMixer();
		case Unmute:	return getMixer();
		case FadeIn:	return getMixer();
		case FadeOut:	return getMixer();
		case FX:		return getMixer();
		
		default: return null;
		}
	}

}
