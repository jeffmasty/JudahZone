package net.judah.mixer;

import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Effect;
import net.judah.api.Effect.RTEffect;
import net.judah.fx.Chorus;
import net.judah.fx.Compressor;
import net.judah.fx.Convolution;
import net.judah.fx.Delay;
import net.judah.fx.EQ;
import net.judah.fx.Filter;
import net.judah.fx.Freeverb;
import net.judah.fx.Gain;
import net.judah.fx.LFO;
import net.judah.fx.Overdrive;
import net.judah.fx.Reverb;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.settable.Presets;
import net.judah.gui.settable.PresetsHandler;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.util.Threads;

/** An effects bus for input or output audio */
@Getter
public abstract class Channel implements Presets {
	protected static final int N_FRAMES = Constants.bufSize();
	protected static final int S_RATE = Constants.sampleRate();
	protected final FloatBuffer left = FloatBuffer.wrap(new float[N_FRAMES]);
    protected final FloatBuffer right = FloatBuffer.wrap(new float[N_FRAMES]);

	protected final String name;
	protected ImageIcon icon;
	protected final boolean isStereo;
	protected boolean onMute;
	protected final Gain gain = new Gain(); // RT but handles separately
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
	protected Preset preset = PresetsHandler.getPresets().getDefault();
	protected final Convolution IR; // RT if stereo
	protected boolean presetActive;

    protected EffectsRack gui;
    protected LFOKnobs lfoKnobs;

    // RT effects known to the channel
    private final List<RTEffect> rt = new ArrayList<>();

    // The list used by the RT thread while processing
    protected ArrayList<RTEffect> active = new ArrayList<>();

    // The list modified by GUI / presets, then swapped in at a safe point
    private final ArrayList<RTEffect> pendingActive = new ArrayList<>();

    // Offline-active effects (not iterated in RT loop)
    private final List<Effect> offline = new ArrayList<>();

    // All effects known to this channel (RT + offline + LFOs etc.)
    protected final List<Effect> effects = new ArrayList<>();

    // fx activate/deactivate flag
    private volatile boolean activeDirty = false;

    public Channel(String name, boolean isStereo) {
    	this.name = name;
    	this.isStereo = isStereo;
    	RTEffect[] order;
    	if (isStereo) {
        	IR = new Convolution.Stereo();
	        order = new RTEffect[] {
	        		eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb, (Convolution.Stereo)IR}; // IR here

    	} else { // TODO presets
        	IR = new Convolution.Mono();
            order = new RTEffect[] {
            		eq, hiCut, loCut, compression, delay, overdrive, chorus, reverb}; // IR handled in Instrument
    	}

    	rt.addAll(List.of(order));
    	effects.addAll(rt);
    	if (!effects.contains(IR))
    		effects.add(IR);
    	effects.add(lfo);
    	effects.add(lfo2);
    	pendingActive.addAll(active); // both empty, but keeps invariants clear
    }

	abstract protected void processImpl();

	public void process() {
		hotSwap();
		processImpl();
	}

	// pass gui changes to the rt thread
	protected void hotSwap() {
	    if (activeDirty) {
	    	active.clear();
	    	active.addAll(pendingActive);
	        activeDirty = false;
	    }
	}

	public final void mix(FloatBuffer outLeft, FloatBuffer outRight) {
		if (isOnMute())
			return;
		// add to output
		AudioTools.mix(left, outLeft);
		AudioTools.mix(right, outRight);
	}

    /** activate/deactive effect (hotswap gatekeeper) */
    public void toggle(Effect effect) {
        boolean wasOn = isActive(effect);

        // Determine new "on" state
        boolean nowOn;
        if (!wasOn) {
            // turning on
            nowOn = true;
            effect.activate();
        } else {
            // turning off
            nowOn = false;
            effect.reset();
        }

        if (rt.contains(effect)) {
            // RT effect: operate on pendingActive; swap will occur on RT thread
            if (nowOn) {
                if (!pendingActive.contains(effect))
                    pendingActive.add((RTEffect)effect);
            } else {
                pendingActive.remove(effect);
            }
            activeDirty = true;
        } else if (effects.contains(effect)) {
            // offline effect: just track in offline list
            if (nowOn) {
                if (!offline.contains(effect))
                    offline.add(effect);
            } else {
                offline.remove(effect);
            }
        } else {
            throw new InvalidParameterException(effect.toString());
        }

        MainFrame.updateChannel(this, effect);
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
    		gui = new EffectsRack(this, JudahZone.getInstance().getBass());
    	return gui;
    }

    public final LFOKnobs getLfoKnobs() { // lazy
    	if (lfoKnobs == null)
    		lfoKnobs = new LFOKnobs(this, JudahZone.getInstance().getMixer());
    	return lfoKnobs;
    }

    public final void setPresetActive(boolean active) {
    	presetActive = active;
        applyPreset();
		MainFrame.updateChannel(this, null /* = preset */);
    }

    public final void setPreset(String name, boolean active) {
    	setPreset(PresetsHandler.getPresets().byName(name));
    	setPresetActive(active);
    }

    public final void setPreset(String name) {
    	setPreset(PresetsHandler.getPresets().byName(name));
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
    		preset = PresetsHandler.getPresets().getDefault();
        setting:
        for (Setting s : preset) {
            for (Effect fx : effects) {
                if (fx.getName().equals(s.getEffectName())) {
                	try {
	                    for (int i = 0; i < s.size(); i++) {
	                        fx.set(i, s.get(i));
	                        MainFrame.updateChannel(this, fx);
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
        for (Effect e : effects) {
        	if (isActive(e))
                presets.add(new Setting(e));
        }
        preset = new Preset(name, presets);
        return preset;
    }

	public final void toggleFx() {
		setPresetActive(!isPresetActive());
	}

	// update time-sync'd fx
	public void tempo(float tempo, float syncUnit) {
		if (delay.isSync()) {
			delay.sync(syncUnit);
			MainFrame.updateChannel(this, delay);
		}
		if (lfo.isSync()) {
			lfo.sync(syncUnit);
			MainFrame.updateChannel(this, lfo);
		}
		if (lfo2.isSync()) {
			lfo2.sync(syncUnit);
			MainFrame.updateChannel(this, lfo2);
		}
		if (chorus.isSync()) {
			chorus.sync(syncUnit);
			MainFrame.updateChannel(this, chorus);
		}
	}

	public final void reset() {
	    // deactivate everything through the same path as toggle()
	    // but we can do it directly to avoid spamming MainFrame.update for each effect

	    for (RTEffect rte : rt) {
	        if (pendingActive.contains(rte)) {
	            rte.reset();
	        }
	    }
	    // turn off RT effects
	    pendingActive.clear();
	    activeDirty = true;;  // RT thread will pick up empty active list

	    // turn off offline effects
	    for (Effect fx : offline) {
	        fx.reset();
	    }
	    offline.clear();

	    // reset all effect internals (regardless of whether they were active)
	    for (Effect fx : effects) {
	        fx.reset();
	    }
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

	public void setActive(Effect fx, boolean on) {
	    boolean currentlyOn = isActive(fx);
	    if (on == currentlyOn) return;
	    toggle(fx);
	}

	public boolean isActive(Effect effect) {
	    if (rt.contains(effect))
	    	return pendingActive.contains(effect);
	    return offline.contains(effect);
	}

}

