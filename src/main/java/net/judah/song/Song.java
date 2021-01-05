package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.midi.MidiPair;
import net.judah.sequencer.Sequencer;
import net.judah.util.Constants;

@Data @AllArgsConstructor @NoArgsConstructor
public class Song {
	
	private static final HashMap<String, Object> defaultMap = new HashMap<>();
	static {
		defaultMap.put(Constants.Param.BPM, null);
		defaultMap.put(Constants.Param.MEASURE, null);
		defaultMap.put(Sequencer.PARAM_FLUID, null);
		defaultMap.put("notes", "");
		
	}
	
	private HashMap<String,Object> props = new HashMap<String, Object>(); 

	private LinkedHashSet<Link> links = new LinkedHashSet<Link>();
	
	private List<Trigger> sequencer = new ArrayList<>();
	
	private List<MidiPair> router = new ArrayList<>();

}
