package net.judah.mixer;

import static net.judah.util.Constants.STEREO;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Effect;
import net.judah.fx.Filter;
import net.judah.fx.Freeverb;
import net.judah.fx.LFO;
import net.judah.fx.Overdrive;
import net.judah.fx.Preset;
import net.judah.fx.Reverb;
import net.judah.fx.Setting;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.settable.Presets;
import net.judah.gui.settable.PresetsHandler;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** An effects bus for input or output audio */
@Getter
public abstract class Channel extends FxChain implements Presets {

	protected final PresetsHandler presets = new PresetsHandler(this);
	protected Preset preset = JudahZone.getPresets().getDefault();
	protected boolean presetActive;

    protected final Filter filter1 = new Filter(STEREO, Filter.Type.pArTy, 700);
    protected final Filter filter2 = new Filter(STEREO, Filter.Type.HiCut, 16000);
    protected final EQ eq = new EQ();
    protected final Compressor compression = new Compressor();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
    protected Reverb reverb = new Freeverb();
	protected final Delay delay = new Delay();
    protected final LFO lfo = new LFO(this);

    protected EffectsRack gui;
    protected LFOKnobs lfoKnobs;

    public Channel(String name, boolean isStereo) {
    	super(name, isStereo);
        Effect[] order = new Effect[] {
        		filter1, filter2, eq, compression, overdrive, chorus, reverb, delay, lfo};
        for (Effect fx : order)
        	add(fx);
    }

	public void replace(Reverb r) {
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

    public final LFOKnobs getLfoKnobs() {
    	if (lfoKnobs == null)
    		lfoKnobs = new LFOKnobs(this, JudahZone.getMixer());
    	return lfoKnobs;
    }

    public final void setPresetActive(boolean active) {
    	presetActive = active;
        applyPreset();
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
            RTLogger.warn(this, "Preset Error. not found: " + s.getEffectName());
        }
        MainFrame.update(this);
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
		MainFrame.update(this);
	}

    @Override
	public final Preset toPreset(String name) {
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : this) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e));
        }
        preset = new Preset(name, presets);
        return preset;
    }

	public final void toggleFx() {
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
		if (delay.isSync() || lfo.isSync() || chorus.isSync())
			MainFrame.update(this);
	}

}

