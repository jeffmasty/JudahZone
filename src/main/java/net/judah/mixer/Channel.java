package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.judah.MixerPane;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.Delay;
import net.judah.effects.EQ;
import net.judah.effects.Freeverb;
import net.judah.effects.LFO;
import net.judah.effects.LFO.Target;
import net.judah.effects.Overdrive;
import net.judah.effects.api.Effect;
import net.judah.effects.api.Preset;
import net.judah.effects.api.Reverb;
import net.judah.effects.api.Setting;
import net.judah.settings.Channels;
import net.judah.util.Console;

/** A mixer bus for both Input and Output processes*/
@Data @EqualsAndHashCode(callSuper = false)
public abstract class Channel extends ArrayList<Effect> {

	/** volume at 50 (knob at midnight) means +/- no gain */
	protected int volume = 50;
    @Setter @Getter protected float pan = 0.5f;

	protected String name;
	protected ChannelGui gui;
	protected ImageIcon icon;

	protected Preset preset;
	protected boolean presetActive;

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

    public final ChannelGui getGui() { // lazy load
		if (gui == null) gui = ChannelGui.create(this);
		return gui;
	}

	@Getter protected boolean onMute;
	public void setOnMute(boolean mute) {
		onMute = mute;
		if (gui != null) gui.update();
	}

	public void setVolume(int vol) {
		this.volume = vol;
		if (lfo.isActive() && lfo.getTarget() == Target.Gain)
		    return; // well, let's not overload GUI and audio
		if (gui != null) gui.update();
		MixerPane.volume(this);
	}

    public Preset toPreset(String name) {
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : this) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e));
        }
        Console.info("Saving " + getName() + " to preset " + name +
                " with " + presets.size() + " effect settings");
        preset = new Preset(name, presets);
        return preset;
    }

	public void reset() {
		// getEq().setActive(false); // EQ stays active
		if (false == this.name.equals(Channels.DRUMS)) // compression for drums stays on
			getCompression().setActive(false);
		getReverb().setActive(false);
		getChorus().setActive(false);
		getCutFilter().setActive(false);
		getDelay().setActive(false);
		getLfo().setActive(false);
		getOverdrive().setActive(false);
		setPan(0.5f);
		
	}		
	

}
