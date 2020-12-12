package net.judah.mixer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.judah.looper.Sample;
import net.judah.mixer.plugin.VolumeWidget;

@Data @AllArgsConstructor 
public final class Channel {

	@NonNull String name;
	final Instrument instrument;
	Integer carlaIndex;
	
	VolumeWidget volume;
	
	@Getter private float gain = 1f;
	@Getter private float pan = 0f;

	public Channel(Instrument instrument, VolumeWidget volume) {
		this.instrument = instrument;
		name = instrument.getName();
		this.volume = volume;
	}
	
	public Channel(Sample loop) {
		name = loop.getName();
		instrument = new Instrument(name, LineType.LOOPER, null, null);
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
