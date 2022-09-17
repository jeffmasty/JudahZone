package net.judah.mixer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.effects.*;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Gain;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.api.Setting;
import net.judah.effects.gui.EffectsRack;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.GuitarTuner;
import net.judah.util.RTLogger;

/** A mixer bus for either Input or Output audio */
@Getter @EqualsAndHashCode(callSuper = false)
public abstract class Channel extends ArrayList<Effect> {

	protected final String name;
	protected ChannelFader fader;
	protected EffectsRack gui;
    protected final boolean isStereo;
	
	@Setter protected ImageIcon icon;
	protected boolean onMute;
	protected Preset preset;
	protected boolean presetActive;
    protected Gain gain = new Gain();
    protected LFO lfo = new LFO();
    protected CutFilter cutFilter;
    protected EQ eq = new EQ();
    protected Overdrive overdrive = new Overdrive();
    protected Chorus chorus = new Chorus();
	protected Delay delay = new Delay();
    protected Reverb reverb;
    @Setter protected JackPort leftPort;
    @Setter protected JackPort rightPort;

    protected FloatBuffer toJackLeft, toJackRight;
    
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
    	if (preset == null)
    		preset = JudahZone.getPresets().getFirst();
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
            RTLogger.warn(this, "Preset Error. not found: " + s.getEffectName());
        }
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
    
    public void setReverb(Reverb r) {
        remove(reverb);
        reverb = r;
        add(reverb);
    }

    public void setDelay(Delay d) {
    	remove(delay);
    	delay = d;
    	add(delay);
    }
    
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
        RTLogger.log(this, "Saving " + getName() + " to preset " + name +
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
		if (this instanceof Instrument == false)
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
	
	public EffectsRack getGui() {
		if (gui == null)
			gui = new EffectsRack(this);
		return gui;
	}
	
	public void processFx(FloatBuffer mono) {
		mono.rewind();
		if (this == GuitarTuner.getChannel()){
			MainFrame.update(AudioTools.copy(mono));
			mono.rewind();
		}
		float gain = getVolume() * 0.5f;
		for (int z = 0; z < Constants.bufSize(); z++)
			mono.put(mono.get(z) * gain);

		if (eq.isActive()) {
			eq.process(mono, true);
		}
		if (chorus.isActive()) {
			chorus.processMono(mono);
		}
		if (overdrive.isActive()) {
			overdrive.processAdd(mono);
		}

		if (delay.isActive()) {
			delay.processAdd(mono, mono, true);
		}
		if (reverb.isActive() && reverb.isInternal()) {
			reverb.process(mono);
		}
		if (cutFilter.isActive()) {
			cutFilter.process(mono);
		}

	}
	
	public void processFx(FloatBuffer left, FloatBuffer right) {
		left.rewind();
		right.rewind();
		
		if (this == GuitarTuner.getChannel()) {
			MainFrame.update(AudioTools.copy(left));
			left.rewind();
		}
		
		float gain = getVolume() * 0.5f;
		for (int z = 0; z < Constants.bufSize(); z++) {
			left.put(left.get(z) * gain);
			right.put(right.get(z) * gain);
		}

		if (eq.isActive()) {
			eq.process(left, true);
			eq.process(right, false);
		}
		if (chorus.isActive()) {
			chorus.processStereo(left, right);
		}
		if (overdrive.isActive()) {
			overdrive.processAdd(left);
			overdrive.processAdd(right);
		}

		if (delay.isActive()) {
			delay.processAdd(left, left, true);
			delay.processAdd(right, right, false);
		}
		if (reverb.isActive() && reverb.isInternal()) {
			((Freeverb)reverb).process(left, right);
		}
		if (cutFilter.isActive()) {
			cutFilter.process(left, right, 1);
		}

	}
	
}
