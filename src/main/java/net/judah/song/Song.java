package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.midi.MidiPair;
import net.judah.util.Constants;

@Data @AllArgsConstructor @NoArgsConstructor
public class Song {

    public static final String PARAM_FLUID = "fluid";
	private static final HashMap<String, Object> defaultMap = new HashMap<>();
	static {
		defaultMap.put(Constants.Param.BPM, null);
		defaultMap.put(Constants.Param.MEASURE, null);
		defaultMap.put(PARAM_FLUID, null);
		defaultMap.put("notes", "");

	}

	private HashMap<String,Object> props = new HashMap<>();

	private LinkedHashSet<Link> links = new LinkedHashSet<>();

	private List<Trigger> sequencer = new ArrayList<>();

	private List<MidiPair> router = new ArrayList<>();

}
