package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.*;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.api.Setting;
import net.judah.effects.gui.EffectsRack;
import net.judah.settings.Channels;
import net.judah.util.Console;

/** A mixer bus for either Input or Output audio */
@Data @EqualsAndHashCode(callSuper = false)
public abstract class Channel extends ArrayList<Effect> {

	protected final String name;
	protected ChannelFader fader;
	protected EffectsRack effects;
	
	protected ImageIcon icon;

	protected Preset preset;
	protected boolean presetActive;

	protected Gain gain = new Gain();
    protected LFO lfo = new LFO();
    protected CutFilter cutFilter = new CutFilter();
    protected EQ eq = new EQ();
	protected Compression compression = new Compression();
    protected Overdrive overdrive = new Overdrive();
    protected Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb = new Freeverb();

    public Channel(String name) {
        this.name = name;
        addAll(Arrays.asList(new Effect[] {
                getLfo(), getCutFilter(), getEq(), getCompression(),
                getChorus(), getOverdrive(),
                getDelay(), getReverb()}));
    }

    public void setReverb(Reverb r) {
        remove(reverb);
        reverb = r;
        add(reverb);
    }

	@Getter protected boolean onMute;
	public void setOnMute(boolean mute) {
		onMute = mute;
		MainFrame.update(this);
	}

    public Preset toPreset(String name) { 
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : this) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e)); // saving gain?
        }
        Console.info("Saving " + getName() + " to preset " + name +
                " with " + presets.size() + " effect settings");
        preset = new Preset(name, presets);
        return preset;
    }

	public void reset() {
		getEq().setActive(false); 
		if (false == this.name.equals(Channels.CALF)) // compression for drums stays on
			getCompression().setActive(false);
		getReverb().setActive(false);
		getChorus().setActive(false);
		getCutFilter().setActive(false);
		getDelay().setActive(false);
		getLfo().setActive(false);
		getOverdrive().setActive(false);
		getGain().setPan(50);
		getGain().setVol(50);
	}

	public float getPan() { 
		return gain.getPan() * 0.01f;
	}

	/**@return 0 to 100*/
	public int getVolume() {
		return gain.getVol();
	}
	
	public ChannelFader getFader() {
		if (fader == null) 
			fader = new ChannelFader(this);
		return fader;
	}
}
