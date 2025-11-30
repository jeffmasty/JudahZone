package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.JudahZone;
import net.judah.fx.Preset;
import net.judah.fx.PresetsDB;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;

public class ChannelTrack extends MidiTrack {

	Channel channel;
	PresetsDB presets = JudahZone.getPresets();

	public ChannelTrack(Channel line, int midiCh) throws InvalidMidiDataException {
		super(line.getName(), midiCh);
		this.channel = line;
	}

	@Override public boolean capture(Midi midi) {
		// TODO Automation!
		return false;
	}

	@Override protected void processNote(ShortMessage m) {
		// TODO Auto-generated method stub

	}

	@Override protected void parse(Track incoming) {
		// TODO Meta && CC
	}

	@Override public String[] getPatches() {
		return presets.getPatches();
	}

	@Override public String progChange(int data1) {
		if (presets.size() > data1)
			channel.setPreset(presets.get(data1));
		return channel.getPreset().getName();
	}

	@Override public boolean progChange(String name) {
		Preset p = presets.byName(name);
		if (p == null)
			return false;
		channel.setPreset(p);
		return true;
	}

}
