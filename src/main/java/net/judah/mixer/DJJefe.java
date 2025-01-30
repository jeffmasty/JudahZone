package net.judah.mixer;

import java.awt.Component;
import java.awt.GridLayout;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.Fader;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.seq.Trax;
import net.judah.seq.track.DrumTrack;
import net.judah.song.FxData;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.synth.taco.TacoTruck;
import net.judah.util.RTLogger;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel implements Cmdr, TimeListener {

	/** has GUI representation on the main mixer */
	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	/** Any channel available to LFO knobs, not necessarily a main mixer fader */
	@Getter private final ArrayList<Channel> all = new ArrayList<>();
	@Getter private final String[] keys;
	private final ArrayList<MixWidget> faders = new ArrayList<MixWidget>();
	private final Zone sources;
	@Getter private final Mains mains;
	private final int size;
	private int idx;

    public DJJefe(JudahClock clock, Mains mains, Looper looper, Zone sources, DrumMachine drums, LineIn ... bonus) {
		this.mains = mains;
    	// preload channel gui's
    	for (LineIn i : sources)
        	i.getGui();
		for (Loop l : looper)
			l.getGui();

    	this.sources = sources;
    	all.add(mains);
        all.addAll(looper);
		all.addAll(sources);
		for (LineIn extra : bonus)
			all.add(extra);
		for (DrumTrack track : drums.getTracks())
			all.add( track.getKit());
    	for (Loop loop : looper) {
    		channels.add(loop);
    		faders.add(loop.getDisplay());
    		add(loop.getDisplay());
    	}
        for (LineIn instrument : sources) {
        	channels.add(instrument);
    		MixWidget fader = new LineMix(instrument, looper);
    		faders.add(fader);
    		add(fader);
        }

        MixWidget fader = new MainsMix(mains, looper);
        faders.add(fader);
        add(fader);
        channels.add(mains);
        size = channels.size();

        setLayout(new GridLayout(1, size, 0, 0));

        keys = new String[size];
        for (int i = 0; i < keys.length; i++)
        	keys[i] = channels.get(i).getName();
        clock.addListener(this);
    }

    public void addChannel(Channel ch) {
    	for (Channel already : channels)
    		if (already == ch)
    			return;
	    channels.add(ch);
    }

    public void removeChannel(Channel ch) {
	    if (!channels.contains(ch))
		    return;
	    MixWidget fade = getFader(ch);
	    remove(fade);
	    faders.remove(fade);
	    channels.remove(ch);
    }

	public void update(Channel channel) {
		for (MixWidget ch : faders)
			if (ch.channel.equals(channel))
				ch.update();
	}

	public void updateAll() {
		for (MixWidget ch : faders)
			ch.update();
	}

	public void highlight(ArrayList<Channel> s) {
		for (MixWidget ch : faders) {
			ch.setBorder(s.contains(ch.channel) ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
		}
	}

	public void highlight(Channel o) {
		for (MixWidget ch : faders)
			ch.setBorder(ch.channel == o ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
	}

	public MixWidget getFader(Channel ch) {
		for (MixWidget fade : faders)
			if (fade.channel == ch)
				return fade;
		return null;
	}

	public Channel byName(String channel) {
		for (Channel ch : all)
			if (ch.getName().equals(channel))
				return ch;
		return null;
	}

	private boolean comboOverride;
	private JComboBox<Channel> combo;
	public Component getCombo(Channel selected) {
		if (combo == null) {
			ArrayList<Channel> chz = getAll();
	    	combo = new JComboBox<>(chz.toArray(new Channel[chz.size()]));
	    	combo.setFont(Gui.BOLD13);
	    	combo.addActionListener(e-> {
	    		if (!comboOverride)
	    			MainFrame.setFocus(((Channel)combo.getSelectedItem()).getLfoKnobs());});
		}
		comboOverride = true;
		combo.setSelectedItem(selected);
		comboOverride = false;
		return combo;
	}

	public void loadFx(List<FxData> data) {
		for (FxData fx : data) {
			String name = fx.getChannel();
			if (name.equals("MAIN")) // Legacy
				name = "Mains";
			Channel ch = byName(name);
			if (ch == null) { // Legacy
				ch = sources.byName(TacoTruck.NAME);
				RTLogger.warn(this, "remapped " + fx.getChannel() + " FX to " + ch.name);
				continue;
			}
			ch.setPreset(fx.getPreset());
		}
	}

	@Override
	public Channel resolve(String key) {
		for (Channel ch : channels)
			if (ch.getName().equals(key))
				return ch;
		for (Trax legacy : Trax.values())
			if (legacy.getName().equals(key))
				return resolve(legacy.toString());
		return null;
	}

	@Override
	public void execute(Param p) {
		Channel ch = resolve(p.val);
		if (ch == null)
			return;
		switch (p.cmd) {
			case FadeOut:
				Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				break;
			case FadeIn:
				Fader.execute(new Fader(ch, Fader.DEFAULT_FADE, 0, 51));
				break;
			case FX:
				ch.toggleFx();
				break;
			case Mute:
				ch.setOnMute(true);
				break;
			case Unmute:
				ch.setOnMute(false);
				break;
			default: throw new InvalidParameterException("" + p);
		}
	}

	public void mutes(List<String> record) {

		for (LineIn in : sources)
			in.setMuteRecord(true);
		for (Channel ch : channels)
			ch.setOnMute(false);
		for (String name : record) {
			LineIn ch = sources.byName(name);
			if (ch == null) {
				RTLogger.log(this, "Unknown channel: " + name);
				continue;
			}
			ch.setMuteRecord(false);
		}
	}

	@Override public void update(Property prop, Object value) {
		if (prop != Property.TEMPO) return;
		float tempo = (float)value;
		all.forEach(ch->ch.tempo(tempo));
	}

	/** process numChannels of RMS indicators in a daisychain*/
	public void process(int numChannels) {
		for (int i = 0; i < numChannels; i++)
			process();
	}

	private void process() {
		if (++idx == size)
			idx = 0;
		Channel ch = channels.get(idx);
		if (ch == mains) // mains doesn't preserve audio, manually make a copy
			mains.setCopy(true);
		MainFrame.update(getFader(ch).gain);
	}

}
