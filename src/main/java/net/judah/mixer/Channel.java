package net.judah.mixer;

import java.util.ArrayList;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

@Data 
public class Channel {

	final Instrument instrument;
	//boolean isSynth;
	//ArrayList<Widget> widgets;
	
	@NonNull String name;
	private final ArrayList<MixerPort> in;
	private final ArrayList<MixerPort> out;
	@Getter private float gain = 1f;
	@Getter private float pan = 0f;
	
	public float getLeftVolume() {
		return gain; // TODO adjust for Pan
	}
	public float getRightVolume() {
		return gain; // TODO adjust for Pan
	}
	
}
