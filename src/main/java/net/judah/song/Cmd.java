package net.judah.song;

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
	
}
