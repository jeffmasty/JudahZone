package net.judah.song;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.midi.MidiPair;

@Data @AllArgsConstructor @NoArgsConstructor
public class Song {

	private HashMap<String,Object> props = new HashMap<String, Object>(); 

	private LinkedHashSet<Link> links = new LinkedHashSet<Link>();
	
	private List<Trigger> sequencer;
	
	private List<MidiPair> router;

}
