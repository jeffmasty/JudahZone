package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Preset;
import net.judah.fx.PresetsDB;
import net.judah.midi.ChannelCC;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.util.RTLogger;

@Getter
public class ChannelTrack extends MidiTrack {

	private static final PresetsDB presets = JudahZone.getPresets();

	private final Channel channel;
	private final ChannelCC cc;

	public ChannelTrack(Channel line, int midiCh, String trackName) throws InvalidMidiDataException {
		super(trackName, midiCh);
		this.channel = line;
		cc = new ChannelCC(channel);
		if (presets == null)
			RTLogger.warn(this, "null presets");
	}

	@Override public boolean capture(Midi midi) {
		// TODO Automation!
		return false;
	}

	@Override protected void processNote(ShortMessage m) {
		cc.process(m);
	}

	@Override protected void parse(Track incoming) {
		clear();

		for (int i= 0; i < incoming.size(); i++) {
			MidiEvent e = incoming.get(i);
			if (Midi.isCC(e.getMessage()) || Midi.isProgChange(e.getMessage())) {
				ShortMessage orig = (ShortMessage)e.getMessage();
				t.add(new MidiEvent(Midi.create(
						orig.getCommand(), ch, orig.getData1(), orig.getData2()), e.getTick()));
			}
		}
//			if (Midi.isNoteOn(e.getMessage())) {
//				ShortMessage orig = (ShortMessage)e.getMessage();
//				int data1 = orig.getData1();
//				t.add(new MidiEvent(Midi.create(
//						orig.getCommand(), ch, data1, orig.getData2()), e.getTick()));
//			}
//		}

		// TODO Meta
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
