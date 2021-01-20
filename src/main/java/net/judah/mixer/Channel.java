package net.judah.mixer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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
import net.judah.effects.gui.EffectsRack;

/** A mixer bus for both Input and Output processes*/
@Data
public abstract class Channel {

	/** volume at 50 (knob at midnight) means +/- no gain */
	protected int volume = 50;
    @Setter @Getter protected float pan = 0.5f;

	protected String name;
	protected ChannelGui gui;
	protected ImageIcon icon;

	@Setter protected Reverb reverb = new Freeverb();
	protected Compression compression = new Compression();
	protected LFO lfo = new LFO();
	protected CutFilter cutFilter = new CutFilter();
	protected Delay delay = new Delay();
	protected EQ eq = new EQ();
    protected Overdrive overdrive = new Overdrive();
    protected Chorus chorus = new Chorus();

    protected List<Effect> effects = Arrays.asList(new Effect[] {
            reverb, compression
    });

	public final ChannelGui getGui() { // lazy load
		if (gui == null) gui = ChannelGui.create(this);
		return gui;
	}


	@Getter protected boolean onMute;
	public void setOnMute(boolean mute) {
		onMute = mute;
		if (gui != null) gui.update();
	}

	public Channel(String name) {
		this.name = name;
	}

	public void setVolume(int vol) {
		this.volume = vol;
		EffectsRack.volume(this);
		if (lfo.isActive() && lfo.getTarget() == Target.Gain)
		    return; // well, let's not overload GUI and audio
		if (gui != null) gui.update();
	}

    public Preset toPreset(String name) {
        ArrayList<Setting> presets = new ArrayList<>();
        for (Effect e : effects) {
            if (!e.isActive()) continue;
            presets.add(new Setting(e));
        }
        return new Preset(name, presets);
    }

}
