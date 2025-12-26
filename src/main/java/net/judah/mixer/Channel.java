package net.judah.mixer;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Effect;
import net.judah.fx.Filter;
import net.judah.fx.Freeverb;
import net.judah.fx.LFO;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.fx.TimeEffect;
import net.judah.gui.MainFrame;
import net.judah.gui.MainFrame.FxChange;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.settable.Presets;
import net.judah.omni.AudioTools;
import net.judah.omni.Threads;
import net.judah.util.RTLogger;

/** An effects bus for input or output audio */
@Getter // TODO  split <Effects> <Actives> <Presets>
public abstract class Channel extends FxChain implements Presets {

    protected final EQ eq = new EQ();
	protected final Filter hiCut = new Filter(true);
	protected final Filter loCut = new Filter(false);
	protected final DJFilter djFilter = new DJFilter(this, hiCut, loCut);
    protected final Compressor compression = new Compressor();
	protected final Delay delay = new Delay();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
    protected Reverb reverb = new Freeverb();
    protected final LFO lfo = new LFO(this, LFO.class.getSimpleName());
    protected final LFO lfo2 = new LFO(this, "LFO2");
	protected Preset preset = JudahZone.getPresets().getDefault();
	protected final Convolution IR;
	protected boolean presetActive;

    protected EffectsRack gui;
    protected LFOKnobs lfoKnobs;

    public Channel(String name, boolean isStereo) {
    	super(name, isStereo);
    	Effect[] order;
    	if (isStereo) {
        	IR = new Convolution.Stereo();
	        order = new Effect[] {
	        		eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb, IR}; // cabSim here

    	} else { // TODO presets
        	IR = new Convolution.Mono();
            order = new Effect[] {
            		eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb}; // cabSim handled in Instrument
    	}

        for (Effect fx : order)
        	add(fx);
    }

	public final void replace(Reverb r) {
		set(indexOf(reverb), r);
		reverb = r;
	}

    @Override public boolean equals(Object obj) {
    	if (obj == null || obj instanceof Channel == false)
    		return false;
    	return gain.equals( ((Channel)obj).getGain());
    }

    @Override public int hashCode() {
    	return gain.hashCode();
    }

    public final EffectsRack getGui() {
    	if (gui == null) // lazy load
    		gui = new EffectsRack(this, JudahZone.getLooper());
    	return gui;
    }

    public final LFOKnobs getLfoKnobs() { // lazy
    	if (lfoKnobs == null)
    		lfoKnobs = new LFOKnobs(this, JudahZone.getMixer());
    	return lfoKnobs;
    }

    public final void setPresetActive(boolean active) {
    	presetActive = active;
        applyPreset();
    }

    public final void setPreset(String name, boolean active) {
    	setPreset(JudahZone.getPresets().byName(name));
    	setPresetActive(active);
    }

    public final void setPreset(String name) {
    	setPreset(JudahZone.getPresets().byName(name));
    }

    @Override
	public final void setPreset(Preset p) {
        preset = p;
        applyPreset();
    }

    public final void toggleMute() {
    	setOnMute(!isOnMute());
    }

	public final void setOnMute(boolean mute) {
		if (mute == onMute)
			return;
		onMute = mute;
		if (onMute)
			Threads.execute(()->{ // for gain indicators
				AudioTools.silence(left);
				AudioTools.silence(right);
			});
		MainFrame.update(this);
	}


    private final void applyPreset() {
    	reset();
    	if (preset == null)
    		preset = JudahZone.getPresets().getDefault();
        setting:
        for (Setting s : preset) {
            for (Effect e : this) {
                if (e.getName().equals(s.getEffectName())) {
                	try {
	                    for (int i = 0; i < s.size(); i++)
	                        e.set(i, s.get(i));
                	} catch (Throwable t) { RTLogger.log(name, preset.getName() + " " + t.getMessage()); }
                    e.setActive(presetActive);
                    continue setting;
                }
            }
            if (lfo.getName().equals(s.getEffectName())) {// better than LFOs in channel's list of RT effects
                for (int i = 0; i < s.size(); i++)
                    lfo.set(i, s.get(i));
                lfo.setActive(presetActive);
            }
            else if (s.getEffectName().equals(lfo2.getName())) {
                for (int i = 0; i < s.size(); i++)
                    lfo2.set(i, s.get(i));
                lfo2.setActive(presetActive);
            }
            else if (s.getEffectName().equals(IR.getName()) && !isStereo) {
                for (int i = 0; i < s.size(); i++)
                    IR.set(i, s.get(i));
                IR.setActive(presetActive);
            }

            else
            	RTLogger.warn(this, "Preset Error. not found: " + s.getEffectName());
        }
        MainFrame.update(this);
    }
    @Override
	public final Preset toPreset(String name) {
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : this) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e));
        }
        if (lfo.isActive()) // better than listing LFOs in RT effects
        	presets.add(new Setting(lfo));
        if (lfo2.isActive())
        	presets.add(new Setting(lfo2));
        if (IR.isActive() && !isStereo)
        	presets.add(new Setting(IR));
        preset = new Preset(name, presets);
        return preset;
    }

	public final void toggleFx() {
		setPresetActive(!isPresetActive());
	}

	// update time-sync'd fx
	public void tempo(float tempo) {
    	float unit = TimeEffect.unit();
		if (delay.isSync()) {
			delay.sync(unit);
			MainFrame.update(this);
		}
		if (lfo.isSync()) {
			lfo.sync(unit);
			MainFrame.update(new FxChange(this, lfo));
		}
		if (lfo2.isSync()) {
			lfo2.sync(unit);
			MainFrame.update(new FxChange(this, lfo2));
		}
		if (chorus.isSync()) {
			chorus.sync(unit);
			MainFrame.update(this);
		}
	}
}

