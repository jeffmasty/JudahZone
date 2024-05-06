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
import net.judah.gui.settable.PresetsHandler;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** An effects bus for input or output audio */
@Getter 
public class Channel {
	protected final int bufSize = Constants.bufSize();
    protected final FloatBuffer left = FloatBuffer.wrap(new float[bufSize]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[bufSize]);
    protected JackPort leftPort;
    protected JackPort rightPort;

	protected final String name;
	protected ImageIcon icon;
	protected final boolean isStereo;
	protected boolean onMute;
	protected final PresetsHandler presets = new PresetsHandler(this);
	protected Preset preset = JudahZone.getPresets().getDefault();
	protected boolean presetActive;
	
    protected final Gain gain = new Gain();
    protected final LFO lfo = new LFO(this);
    protected final Filter filter1;
    protected final Filter filter2;
    protected final EQ eq = new EQ();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb;
    protected final Compressor compression = new Compressor();
    protected final List<Effect> effects;
    protected EffectsRack gui;
    @Setter protected LFOKnobs lfoKnobs;
    
    public Channel(String name, boolean isStereo) {
        this.name = name;
        this.isStereo = isStereo;
        filter1 = new Filter(isStereo, Filter.Type.pArTy, 700);
        filter2 = new Filter(isStereo, Filter.Type.HiCut, 16000); 
        reverb = new Freeverb(isStereo);
        effects = Arrays.asList(new Effect[] {
        		reverb, delay, overdrive, chorus,
                eq, filter1, filter2, compression, lfo });
    }

    @Override public boolean equals(Object obj) {
    	if (obj == null || obj instanceof Channel == false)
    		return false;
    	return gain.equals( ((Channel)obj).getGain());
    }
    
    public EffectsRack getGui() {
    	if (gui == null) // lazy load
    		gui = new EffectsRack(this, JudahZone.getLooper());
    	return gui;
    }
    
    public LFOKnobs getLfoKnobs() {
    	if (lfoKnobs == null)
    		lfoKnobs = new LFOKnobs(this, JudahZone.getMixer());
    	return lfoKnobs;
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
    		preset = JudahZone.getPresets().getDefault();
        setting:
        for (Setting s : preset) {
            for (Effect e : effects) {
                if (e.getName().equals(s.getEffectName())) {
                	try {
	                    for (int i = 0; i < s.size(); i++)
	                        e.set(i, s.get(i));
                	} catch (Throwable t) { RTLogger.log(name, preset.getName() + " " + t.getMessage()); }
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
    	// if (p != preset) RTLogger.log(this, name + "->" + p.getName() );
        preset = p;
        applyPreset();
    }
    
    public void setDelay(Delay d) { // switch to slapback delay?
    	effects.remove(delay);
    	delay = d;
    	effects.add(delay);
    }
    
	public void setOnMute(boolean mute) {
		if (mute == onMute) 
			return;
		onMute = mute;
		MainFrame.update(this);
	}

    public Preset toPreset(String name) { 
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : effects) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e)); 
        }
        preset = new Preset(name, presets);
        return preset;
    }

	public void reset() {
		for (Effect fx : effects)
			fx.setActive(false);
		MainFrame.update(this);
	}

	/**@return 0 to 100*/
	public int getVolume() {
		return gain.get(Gain.VOLUME);
	}
	
	@Override public String toString() { return name; }

	public void toggleFx() {
		setPresetActive(!isPresetActive());
	}

	public void tempo(float tempo) {
    	float unit = Constants.millisPerBeat(tempo) / (float)JudahZone.getClock().getSubdivision();
		if (delay.isSync())
			delay.sync(unit);
		if (lfo.isSync())
			lfo.sync(unit);
		if (chorus.isSync())
			chorus.sync(unit);
	}

}

