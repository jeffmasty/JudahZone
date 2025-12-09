package net.judah.mixer;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.sampler.Sampler;
import net.judah.seq.TrackList;
import net.judah.seq.track.DrumTrack;
import net.judah.song.FxData;
import net.judah.util.RTLogger;

/** Graphical representation of the Mixer*/
public class DJJefe extends JPanel implements TimeListener {

	/** has GUI representation on the main mixer */
	@Getter private final ArrayList<Channel> channels = new ArrayList<>();
	/** Any channel available to LFO knobs, not necessarily a main mixer fader */
	@Getter private final ArrayList<Channel> all = new ArrayList<>();
	private final ArrayList<MixWidget> faders = new ArrayList<MixWidget>();
	@Getter private final Mains mains;
	private final int size;
	private int idx;

	public DJJefe(JudahClock clock, Mains mains, Looper looper, Collection<LineIn> fades, TrackList<DrumTrack> drums, Sampler samples) {
		this.mains = mains;
    	all.add(mains);
        all.addAll(looper);
		all.addAll(fades);
		drums.forEach(track -> all.add(track.getKit()));
		all.add(samples);

    	for (Loop loop : looper) {
    		channels.add(loop);
    		faders.add(loop.getDisplay());
    		add(loop.getDisplay());
    	}
        for (LineIn instrument : fades) {
        	channels.add(instrument);
    		MixWidget fader = new LineMix(instrument, looper.getSoloTrack());
    		faders.add(fader);
    		add(fader);
        }
        MixWidget fader3 = new MainsMix(mains);
        faders.add(fader3);
        add(fader3);
        channels.add(mains);
        size = channels.size();
        setLayout(new GridLayout(1, size, 0, 0));
        clock.addListener(this);
	}

    public DJJefe(JudahClock clock, Mains mains, Looper looper, Collection<LineIn> sources, DrumMachine drums, LineIn ... bonus) {
		this.mains = mains;
    	all.add(mains);
        all.addAll(looper);
		all.addAll(sources);
		all.add(drums);

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
    		MixWidget fader = new LineMix(instrument, looper.getSoloTrack());
    		faders.add(fader);
    		add(fader);
        }
    	channels.add(drums);
		MixWidget fader2 = new LineMix(drums, looper.getSoloTrack());
		faders.add(fader2);
		add(fader2);


        MixWidget fader3 = new MainsMix(mains);
        faders.add(fader3);
        add(fader3);
        channels.add(mains);
        size = channels.size();
        setLayout(new GridLayout(1, size, 0, 0));
        clock.addListener(this);
    }

	public void addChannel(LineIn line) {
		all.add(line);
		if (combo == null)
			return;
		comboOverride = true;
		combo.removeAllItems();
		all.forEach(ch -> combo.addItem(ch));
		combo.setSelectedItem(JudahZone.getFxRack().getSelected().getFirst());
		comboOverride = false;
	}

    public void removeChannel(Channel ch) {
	    if (!channels.contains(ch))
		    return;
	    MixWidget fade = getFader(ch);
	    remove(fade);
	    faders.remove(fade);
	    channels.remove(ch);
	    if (combo == null)
	    	return;
	    // TODO combos
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
	    	combo = new JComboBox<>(all.toArray(new Channel[all.size()]));
	    	combo.setFont(Gui.BOLD13);
	    	combo.addActionListener(e-> {
	    		if (!comboOverride)
	    			MainFrame.setFocus(combo.getSelectedItem());});
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
				for (Channel c : all) {
					if (c.name == name) {
						ch = c;
						RTLogger.warn(this, "remapped " + fx.getChannel() + " FX to " + ch.name);
						break;
					}
				}
				if (ch == null) {
					ch = JudahZone.getTaco();
					RTLogger.warn(this, "forced " + fx.getChannel() + " FX to " + ch.name);
				}
				continue;
			}
			ch.setPreset(fx.getPreset());
		}
	}

	public void mutes(List<String> record) {
		for (Channel ch : channels)
			if (ch instanceof LineIn in)
				in.setMuteRecord(true);
		JudahZone.getGuitar().setMuteRecord(false);
		JudahZone.getTaco().setMuteRecord(false);
	}

	@Override public void update(Property prop, Object value) {
		if (prop != Property.TEMPO) return;
		float tempo = (float)value;
		all.forEach(ch->ch.tempo(tempo));
	}

	/** process numChannels of RMS indicators in a daisychain*/
	public void monitor(int numChannels) {
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
