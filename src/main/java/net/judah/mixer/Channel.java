package net.judah.mixer;

import javax.swing.ImageIcon;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.judah.mixer.bus.Compression;
import net.judah.mixer.bus.CutFilter;
import net.judah.mixer.bus.Delay;
import net.judah.mixer.bus.EQ;
import net.judah.mixer.bus.Freeverb;
import net.judah.mixer.bus.LFO;
import net.judah.mixer.bus.Reverb;

/** A mixer bus for both Input and Output processes*/
@Data
public abstract class Channel {

	/** volume at 50 (knob at midnight) means +/- no gain */
	protected int volume = 50;

	protected String name;
	protected ChannelGui gui;
	protected ImageIcon icon;

	@Setter protected Reverb reverb = new Freeverb();
	protected Compression compression = new Compression();
	protected LFO lfo = new LFO();
	protected CutFilter cutFilter = new CutFilter();
	protected Delay delay = new Delay();
	protected EQ eq = new EQ();

	public final ChannelGui getGui() { // lazy load
		if (gui == null) gui = ChannelGui.create(this);
		return gui;
	}

	@Getter protected boolean onMute;
	public void setOnMute(boolean mute) {
		onMute = mute;
		if (gui != null) gui.update();
	}

	public Channel(String name) {
		this.name = name;
	}

	public void setVolume(int vol) {
		this.volume = vol;
		SoloTrack.volume(this);
		if (gui != null) gui.update();
	}

}
