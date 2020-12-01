package net.judah.mixer;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.judah.looper.Sample;
import net.judah.mixer.widget.VolumeWidget;
import net.judah.mixer.widget.Widget;

@Data @AllArgsConstructor 
public final class Channel {

	public static enum Type {
		SYS, SYNTH, CARLA, LOOPER, OTHER
	}

	@NonNull String name;
	final Instrument instrument;
	Integer carlaIndex;
	

	ArrayList<Widget> widgets = new ArrayList<>();
	VolumeWidget volume;
	
	@Getter private float gain = 1f;
	@Getter private float pan = 0f;

	public Channel(Instrument instrument, VolumeWidget volume) {
		this.instrument = instrument;
		name = instrument.getName();
		this.volume = volume;
	}
	
//	public Channel(Instrument instrument, Integer carlaIndex) {
//		this.instrument = instrument;
//		name = instrument.name;
//		this.carlaIndex = carlaIndex;
//	}

	
	
//	public Channel(Instrument instrument) {
//		
//	}
	
	public Channel(Sample loop) {
		name = loop.getName();
		instrument = new Instrument(name, Channel.Type.LOOPER, null, null);
		carlaIndex = null;
	}

	public void setGain(float gain) {
		this.gain = gain; 
		if (volume != null)
			volume.setVolume(gain);
	}
	
	
}





//public float getLeftVolume() { return gain; } // TODO adjust for Pan
//public float getRightVolume() { return gain; } // TODO 
