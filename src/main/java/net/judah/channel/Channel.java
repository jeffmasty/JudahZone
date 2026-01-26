package net.judah.channel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import judahzone.api.Custom;
import judahzone.api.FX;
import judahzone.api.FX.RTFX;
import judahzone.fx.Chorus;
import judahzone.fx.Compressor;
import judahzone.fx.Convolution;
import judahzone.fx.Delay;
import judahzone.fx.EQ;
import judahzone.fx.Filter;
import judahzone.fx.Freeverb;
import judahzone.fx.Gain;
import judahzone.fx.Overdrive;
import judahzone.fx.Reverb;
import judahzone.fx.StereoBus;
import judahzone.util.AudioMetrics;
import judahzone.util.AudioTools;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.midi.LFO;
import net.judah.mixer.DJFilter;

/** A Gui focused effects bus for input or output audio.
 * RMS scalars published from RT thread. {@link #getLastRmsLeft()} / {@link #getLastRmsRight()} */
@Getter
public abstract class Channel extends StereoBus implements Presets {

    public enum Update {MUTE, MUTE_RECORD, PRESET, RMS, LOOP, SOLO} // TODO updateCh

	protected final String name;
    protected final boolean isStereo;
    /** Copy of json definition, if any */
    protected Custom user;
    @Setter protected boolean onMixer;

    protected final Gain gain = new Gain(); // RT but handles separately
    protected final EQ eq = new EQ();
    protected final Filter hiCut = new Filter(true);
    protected final Filter loCut = new Filter(false);
    protected final DJFilter djFilter = new DJFilter(this, hiCut, loCut);
    protected final Compressor compression = new Compressor();
    protected final Delay delay = new Delay();
    protected final Overdrive overdrive = new Overdrive();
    protected final Chorus chorus = new Chorus();
    protected final LFO lfo = new LFO(this, LFO.class.getSimpleName());
    protected final LFO lfo2 = new LFO(this, "LFO2");
    protected final Convolution IR; // RT if stereo
    protected Reverb reverb = new Freeverb();

    protected ImageIcon icon;
    protected boolean onMute;
    protected Preset preset = PresetsDB.getDefault();
    protected boolean presetActive;
    protected EffectsRack gui;
    protected LFOKnobs lfoKnobs;
    /** Last RMS values computed on the RT thread (0..1 approximate). */
	private /* volatile */ float lastRmsLeft = 0f;
	private /* volatile */ float lastRmsRight = 0f;

    public Channel(String name, boolean isStereo) {
        this.name = name;
        this.isStereo = isStereo;
        RTFX[] order;
        if (isStereo) {
            IR = new Convolution.Stereo();
            order = new RTFX[] {
                    eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb, (Convolution.Stereo)IR}; // IR here
        } else {
            IR = new Convolution.Mono();
            order = new RTFX[] {
                    eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb}; // IR handled elsewhere for mono
        }

        // Add the RT effects into the parent-managed list
        rt.addAll(List.of(order));
        effects.addAll(rt);
        if (!effects.contains(IR))
            effects.add(IR);
        effects.add(lfo);
        effects.add(lfo2);
    }

    abstract protected void processImpl();

	public final void process() {
        hotSwap();
        processImpl();
        // apply normalization if requested (if target > 0)
        // AudioMetrics.normalizeToRms(left, right, normalizeTargetRms, normalizeMaxGain);
        computeRMS(left, right); // meters now see normalized level
    }

    public final void mix(float[] outLeft, float[] outRight) {
        if (isOnMute())
            return;
        AudioTools.mix(left, outLeft);
        AudioTools.mix(right, outRight);
    }

    public final void replace(Reverb r) {
        boolean wasActive = isActive(reverb);
        setActive(reverb, false);
        rt.set(rt.indexOf(reverb), r);
        reverb = r;
        if (wasActive)
            setActive(reverb, true);
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
        if (gui == null) // lazy
            gui = new EffectsRack(this, JudahZone.getInstance());
        return gui;
    }

    public final LFOKnobs getLfoKnobs() { // lazy
        if (lfoKnobs == null)
            lfoKnobs = new LFOKnobs(this, JudahZone.getInstance().getChannels().getAll().getCombo(this));
        return lfoKnobs;
    }

    public final void setPresetActive(boolean active) {
    	if (presetActive == active)
			return;
        presetActive = active;
        applyPreset();
        MainFrame.update(this);
    }

    public final void setPreset(String name, boolean active) {
        setPreset(PresetsDB.byName(name));
        setPresetActive(active);
    }

    public final void setPreset(String name) {
        setPreset(PresetsDB.byName(name));
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
            AudioTools.silence(left);
            AudioTools.silence(right);
        MainFrame.update(this);
    }

    private final void applyPreset() {
        reset(); // inherited reset() will clear pendingActive etc
        if (preset == null)
            preset = PresetsDB.getDefault();
        setting:
        for (Setting s : preset) {
            for (FX fx : effects) {
                if (fx.getName().equals(s.getEffectName())) {
                    try {
                        for (int i = 0; i < s.size(); i++) {
                            fx.set(i, s.get(i));
                            MainFrame.updateFx(this, fx);
                        }
                    } catch (Throwable t) { RTLogger.log(name, preset.getName() + " " + t.getMessage()); }

                    setActive(fx, presetActive);
                    continue setting;
                }
            }
        }
    }

    @Override
    public final Preset toPreset(String name) {
        ArrayList<Setting> presets = new ArrayList<>();
        for (FX e : effects) {
            if (isActive(e))
                presets.add(new Setting(e));
        }
        preset = new Preset(name, presets);
        return preset;
    }

    @Override
    public void toggle(FX effect) {
        super.toggle(effect);
        MainFrame.updateFx(this, effect);
    }

    public final void toggleFx() {
        setPresetActive(!isPresetActive());
    }

    // update time-sync'd fx
    public void tempo(float tempo, float syncUnit) {
        if (delay.isSync()) {
            delay.sync(syncUnit);
            MainFrame.updateFx(this, delay);
        }
        if (lfo.isSync()) {
            lfo.sync(syncUnit);
            MainFrame.updateFx(this, lfo);
        }
        if (lfo2.isSync()) {
            lfo2.sync(syncUnit);
            MainFrame.updateFx(this, lfo2);
        }
        if (chorus.isSync()) {
            chorus.sync(syncUnit);
            MainFrame.updateFx(this, chorus);
        }
    }

    @Override
    public final void reset() {
        super.reset(); // does RT/offline clearing and effect resets
        MainFrame.update(this);
    }

    /**@return 0 to 100*/
    public final int getVolume() {
        return gain.get(Gain.VOLUME);
    }

    /**@return 0 to 100*/
    public final int getPan() {
        return gain.get(Gain.PAN);
    }

    @Override public final String toString() { return name; }

    public void setUser(Custom json) {
    	this.user = json;
    	this.onMixer = json.onMixer();
    }
	public boolean isSys() {
		return user == null;
	}

    /**
     * Compute RMS for arbitrary buffers and publish to the channel's volatile fields.
     * Call from the RT thread with the same buffers that were processed.
     */
    protected void computeRMS(float[] l, float[] r) {
        lastRmsLeft = AudioMetrics.rms(l);
        lastRmsRight = AudioMetrics.rms(r);
    }



}