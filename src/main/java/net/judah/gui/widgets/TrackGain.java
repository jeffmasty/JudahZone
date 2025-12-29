package net.judah.gui.widgets;

import net.judah.fx.Gain;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.mixer.Channel;
import net.judah.seq.track.MidiTrack;
import net.judahzone.gui.Gui;

public class TrackGain extends Slider {

	Channel ch;

	public TrackGain(MidiTrack track) {
		super(null);
		this.ch = track.getChannel();
		Gui.resize(this, Size.MODE_SIZE);
		setValue(ch.getVolume());
		addChangeListener(e->{
			if (ch.getVolume() != getValue()) {
				ch.getGain().set(Gain.VOLUME, getValue());
				MainFrame.update(ch);
//				MainFrame.update(new TrackUpdate(Update.GAIN, track));
			}});
	}

	public TrackGain(Channel ch) {
		super(null);
		this.ch = ch;
		Gui.resize(this, Size.MODE_SIZE);
		setValue(ch.getVolume());
		addChangeListener(e->{
			if (ch.getVolume() != getValue()) {
				ch.getGain().set(Gain.VOLUME, getValue());
				MainFrame.update(ch);
//				MainFrame.update(new TrackUpdate(Update.GAIN, track));
			}});
	}

	public void update() {
		if (getValue() != ch.getVolume())
			setValue(ch.getVolume());
	}

}
