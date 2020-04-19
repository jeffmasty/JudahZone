package net.judah.mixer;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.judah.looper.Loop;
import net.judah.mixer.instrument.InstType;
import net.judah.mixer.widget.VolumeWidget;

@Data @AllArgsConstructor 
public final class Channel {

	@NonNull String name;
	final Instrument instrument;
	Integer carlaIndex;
	

	ArrayList<Widget> widgets;
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
	
	public Channel(Loop loop) {
		name = loop.getName();
		instrument = new Instrument(name, InstType.Looper, null, null);
		carlaIndex = null;
	}

	public void setVolume(float gain) {
		if (volume != null)
		volume.setVolume(gain);
	}
	
	
}





//public float getLeftVolume() { return gain; } // TODO adjust for Pan
//public float getRightVolume() { return gain; } // TODO 
