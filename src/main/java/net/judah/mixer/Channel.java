package net.judah.mixer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.fx.*;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** A mixer bus for either Input or Output audio */
@Getter 
public class Channel {
	protected final int bufSize = Constants.bufSize();
    protected final FloatBuffer left = FloatBuffer.allocate(bufSize);
    protected final FloatBuffer right = FloatBuffer.allocate(bufSize);

	protected final String name;
    protected final boolean isStereo;
	@Setter protected ImageIcon icon;
	protected boolean onMute;
	protected Preset preset;
	protected boolean presetActive;
	
    protected final Gain gain = new Gain();
    protected final LFO lfo = new LFO(this);
    protected final CutFilter party;
    protected final CutFilter filter;
    protected final EQ eq = new EQ();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb;
    protected Compressor compression = new Compressor();
    @Setter protected JackPort leftPort;
    @Setter protected JackPort rightPort;
    protected final List<Effect> effects;
    protected final EffectsRack gui;
    protected final LFOKnobs lfoKnobs;
    
    public Channel(String name, boolean isStereo) {
        this.name = name;
        this.isStereo = isStereo;
        party = new CutFilter(isStereo);
        filter = new CutFilter(isStereo, CutFilter.Type.LP24, 16000); // hicut
        reverb = new Freeverb(isStereo);
        effects = Arrays.asList(new Effect[] {
                getLfo(), getFilter(), getParty(), getEq(),
                getChorus(), getOverdrive(),
                getDelay(), getReverb(), getCompression()});
        preset = JudahZone.getPresets().getFirst();
        gui = new EffectsRack(this);
        lfoKnobs = new LFOKnobs(this);
    }

    @Override public boolean equals(Object obj) {
    	if (obj == null || obj instanceof Channel == false)
    		return false;
    	return gain.equals( ((Channel)obj).getGain());
    }
    
    @Override public int hashCode() {
    	return gain.hashCode();
    }
    
    
    public void setPresetActive(boolean active) {
    	presetActive = active;
        applyPreset();
    }

    private void applyPreset() {
    	reset();
    	if (preset == null)
    		preset = JudahZone.getPresets().getFirst();
        setting:
        for (Setting s : preset) {
            for (Effect e : effects) {
                if (e.getName().equals(s.getEffectName())) {
                    for (int i = 0; i < s.size(); i++)
                        e.set(i, s.get(i));
                    e.setActive(presetActive);
                    continue setting;
                }
            }
            RTLogger.warn(this, "Preset Error. not found: " + s.getEffectName());
        }
        MainFrame.update(this);
    }
    
    public void setPreset(String name, boolean active) {
    	setPreset(JudahZone.getPresets().byName(name));
    	setPresetActive(active);
    }
    
    public void setPreset(String name) {
    	setPreset(JudahZone.getPresets().byName(name));
    }
    
    public void setPreset(Preset p) {
    	if (p != preset)
    		RTLogger.log(this, name + "->" + p.getName() );
        preset = p;
        applyPreset();
    }
    
    public void setDelay(Delay d) {
    	effects.remove(delay);
    	delay = d;
    	effects.add(delay);
    }
    
	public void setOnMute(boolean mute) {
		onMute = mute;
		MainFrame.update(this);
	}

    public Preset toPreset(String name) { 
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : effects) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e)); // saving gain?
        }
        RTLogger.log(this, "Saving " + getName() + " to preset " + name +
                " with " + presets.size() + " effect settings");
        preset = new Preset(name, presets);
        return preset;
    }

	public void reset() {
		for (Effect fx : effects)
			if (fx != filter)
				fx.setActive(false);
		gain.set(Gain.PAN, 50);
		if (this instanceof Instrument == false)
			gain.set(Gain.VOLUME, 50);
		MainFrame.update(this);
	}

	/**@return 0 to 100*/
	public int getVolume() {
		return gain.get(Gain.VOLUME);
	}
	
	/**@return 0 to 100*/
	public int getPan() {
		return gain.get(Gain.PAN);
	}
	
	@Override public String toString() { return name; }

}

