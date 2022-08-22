package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.*;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.api.Setting;
import net.judah.effects.gui.EffectsRack;
import net.judah.util.Console;
import net.judah.util.RTLogger;

/** A mixer bus for either Input or Output audio */
@Data @EqualsAndHashCode(callSuper = false)
public abstract class Channel extends ArrayList<Effect> {

	protected final String name;
	protected ChannelFader fader;
	protected EffectsRack effects;
	
	protected ImageIcon icon;

	protected Preset preset = JudahZone.getPresets().getFirst();
	protected boolean presetActive;

	protected Gain gain = new Gain();
    protected LFO lfo = new LFO();
    protected CutFilter cutFilter;
    protected EQ eq = new EQ();
	//protected Compression compression = new Compression();
    protected Overdrive overdrive = new Overdrive();
    protected Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb;

    @Getter protected final boolean isStereo;
    
    public Channel(String name, boolean isStereo) {
        this.name = name;
        this.isStereo = isStereo;
        cutFilter = new CutFilter(isStereo);
        reverb = new Freeverb(isStereo);
        addAll(Arrays.asList(new Effect[] {
                getLfo(), getCutFilter(), getEq(),
                getChorus(), getOverdrive(),
                getDelay(), getReverb()}));
        
    }

    public void setPresetActive(boolean active) {
    	if (active == presetActive) 
    		return;
    	presetActive = active;
        applyPreset();
        MainFrame.update(this);
    }

    private void applyPreset() {
    	reset();
        setting:
        for (Setting s : preset) {
            for (Effect e : this) {
                if (e.getName().equals(s.getEffectName())) {
                    for (int i = 0; i < s.size(); i++)
                        e.set(i, s.get(i));
                    e.setActive(isPresetActive());
                    continue setting;
                }
            }
            Console.info("Preset Error. not found: " + s.getEffectName());
        }
    }
    
    public void setPreset(Preset p) {
    	if (p != preset)
    		RTLogger.log(this, name + "->" + p.getName() );
        preset = p;
        applyPreset();
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
