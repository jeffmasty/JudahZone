package net.judah.mixer;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

@Data 
public class Channel {

	//final Instrument instrument;
	//boolean isSynth;
	//ArrayList<Widget> widgets;
	
	@NonNull String name;
	private final MixerPort inLeft;
	private final MixerPort inRight;
	private final MixerPort outLeft;
	private final MixerPort outRight;
	@Getter private float gain = 1f;
	@Getter private float pan = 0f;
	
	public float getLeftVolume() {
		return gain; // TODO adjust for Pan
	}
	public float getRightVolume() {
		return gain; // TODO adjust for Pan
	}
	
}
