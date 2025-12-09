package net.judah.seq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;

import net.judah.util.RTLogger;

public class MetaMap extends HashMap<Meta, List<MidiEvent>> {

	public final Track t;

	public MetaMap(Track t) {
		this.t = t;
		for (int i = 0; i < t.size(); i++)
			if (t.get(i).getMessage() instanceof MetaMessage m)
				incoming(m, t.get(i));
	}

	public void incoming(MetaMessage m, MidiEvent source) {
		Meta key = Meta.getType(m);
		if (key == null)
			return; // not handled
		List<MidiEvent> msgs = get(key);
		if (msgs == null)
			put(key, list(source));
		else
			msgs.add(source);
	}

	private List<MidiEvent> list(MidiEvent source) {
		ArrayList<MidiEvent> result = new ArrayList<MidiEvent>();
		result.add(source);
		return result;
	}

	public void publish(Track t) {
		for (Entry<Meta, List<MidiEvent>> item :entrySet()) {
			for (MidiEvent e : item.getValue())
				t.add(e);
		}
	}

	public String getString(Meta type) {
		if (get(type) == null)
			return null;
		return new String(((MetaMessage)get(type).getFirst().getMessage()).getData());

	}

	public void setString(Meta type, String value) {
		List<MidiEvent> list = get(type);
		byte[] data = value.getBytes();
		try {
			MetaMessage m = new MetaMessage(type.type, data, data.length);
			if (list == null || list.isEmpty())
				put(type, list(new MidiEvent(m,0)));

			else
				list.set(0, new MidiEvent(m, 0));
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
	}

	// port, channel
	public int getInt(Meta type) {
		if (!containsKey(type))
			return 0;
		byte[] dat = ((MetaMessage)get(type).getFirst().getMessage()).getData();
		if (dat.length == 0)
			return 0;
		if (dat.length == 1)
			return dat[0];
		else
			return dat[1] * Byte.MAX_VALUE + dat[0];
	}

	public void setInt(Meta type, int val) {
		List<MidiEvent> list = get(type);
		byte[] payload = new byte[2];

		payload[0] = 0; // high byte
		payload[1] = (byte) val; // low byte
		try {
			MetaMessage m = new MetaMessage(type.type, payload, payload.length);
			if (list == null)
				put(type, list(new MidiEvent(m,0)));
			else
				list.set(0, new MidiEvent(m, 0));
		} catch (InvalidMidiDataException e) { RTLogger.warn(this, e); }
	}


}
