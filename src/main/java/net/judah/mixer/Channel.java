package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.effects.*;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.api.Setting;
import net.judah.effects.gui.EffectsRack;
import net.judah.effects.gui.PresetsHandler;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** A mixer bus for either Input or Output audio */
@Getter @EqualsAndHashCode(callSuper = false)
public class Channel {

	protected final int bufSize = Constants.bufSize();
	protected final String name;
    protected final boolean isStereo;
	
	@Setter protected ImageIcon icon;
	protected boolean onMute;
	protected Preset preset;
	protected boolean presetActive;
	protected PresetsHandler presets;
	
    protected final Gain gain = new Gain();
    protected final LFO lfo = new LFO(this);
    protected final CutFilter cutFilter;
    protected final CutFilter hiCut;
    protected final EQ eq = new EQ();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb;
    protected Compressor compression = new Compressor();
    @Setter protected JackPort leftPort;
    @Setter protected JackPort rightPort;
    protected final List<Effect> effects;
    protected EffectsRack gui;
    protected LFOKnobs lfoKnobs;
    
    public Channel(String name, boolean isStereo) {
        this.name = name;
        this.isStereo = isStereo;
        cutFilter = new CutFilter(isStereo);
        hiCut = new CutFilter(isStereo);
        hiCut.setFilterType(CutFilter.Type.LP24);
		hiCut.setResonance(1);
		hiCut.setFrequency(16000);

        reverb = new Freeverb(isStereo);
        effects = Arrays.asList(new Effect[] {
                getLfo(), getHiCut(), getCutFilter(), getEq(),
                getChorus(), getOverdrive(),
                getDelay(), getReverb(), getCompression()});
        preset = JudahZone.getPresets().getFirst();
        presets = new PresetsHandler(this);
    }

    public void setPresetActive(boolean active) {
    	if (active == presetActive) 
    		return;
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
                    e.setActive(isPresetActive());
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

    public void toggleFx() {
		for (Effect fx : effects)
			if (fx.isActive() && fx != hiCut) {
				reset();
				return;
			}
		applyPreset();
	}

	public void reset() {
		for (Effect fx : effects)
			if (fx != hiCut)
				fx.setActive(false);
		gain.setPan(50);
		if (this instanceof Instrument == false)
			gain.setVol(50);
	}

	public final float getPan() { 
		return gain.getPan() * 0.01f;
	}

	/**@return 0 to 100*/
	public int getVolume() {
		return gain.getVol();
	}
	
	@Override
	public String toString() {
		return name;
	}

	public EffectsRack getGui() {
		if (gui == null)
			gui = new EffectsRack(this);
		return gui;
	}
	
	public LFOKnobs getLfoKnobs() {
		if (lfoKnobs == null)
			lfoKnobs = new LFOKnobs(this);
		return lfoKnobs;
	}
}

