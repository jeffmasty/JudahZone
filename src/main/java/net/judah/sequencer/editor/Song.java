package net.judah.sequencer.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.judah.midi.MidiRule;

@Data @AllArgsConstructor @NoArgsConstructor
public class Song {

	private HashMap<String,Object> props = new HashMap<>();
	private LinkedHashSet<Link> links = new LinkedHashSet<>();
	private List<Trigger> sequencer = new ArrayList<>();
	private List<MidiRule> router = new ArrayList<>();
	
}
