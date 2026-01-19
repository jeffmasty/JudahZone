package net.judah.seq.track;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Midi;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.channel.Preset;
import net.judah.channel.PresetsDB;
import net.judah.midi.ChannelCC;

@Getter
public class ChannelTrack extends MidiTrack {


	private final Channel channel;
	private final ChannelCC cc;

	public ChannelTrack(Channel line, int midiCh, String trackName) throws InvalidMidiDataException {
		super(trackName, midiCh);
		this.channel = line;
		cc = new ChannelCC(channel);
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
		return PresetsDB.getPatches();
	}

	@Override public String progChange(int data1) {
		if (PresetsDB.size() > data1)
			channel.setPreset(PresetsDB.get(data1));
		return channel.getPreset().getName();
	}

	@Override public boolean progChange(String name) {
		Preset p = PresetsDB.byName(name);
		if (p == null)
			return false;
		channel.setPreset(p);
		return true;
	}

}
