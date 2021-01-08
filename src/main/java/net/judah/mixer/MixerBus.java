package net.judah.mixer;

import lombok.Data;
import net.judah.plugin.LFO;

/** shared Channel and Sample processes */
@Data
public abstract class MixerBus {

	protected String name;
	
	protected Compression compression = new Compression();
	protected Reverb reverb = new Reverb();
	protected LFO lfo = new LFO();
	protected EQ eq = new EQ();
	
	public MixerBus(String name) {
		this.name = name;
	}
	
    public abstract int getVolume();
    public abstract void setVolume(int vol);
    
    public abstract void setOnMute(boolean mute);
    public abstract boolean isOnMute();
    public abstract MixerGui getGui();
	
}
