package net.judah.midi;

import java.util.ArrayList;

public class Channels {
	
	private final ArrayList<Actives> channels = new ArrayList<Actives>();
	
	public Actives getCh(int ch) {
		for (int i = 0; i < channels.size(); i++)
			if (channels.get(i).getChannel() == ch)
				return channels.get(i);
		return null;
	}

}
