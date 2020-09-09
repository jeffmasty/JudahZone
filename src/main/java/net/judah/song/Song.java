package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class Song {

//	private String name;
//	private String setupNotes = ""; // html capable?
//	private String teardownNotes = "";
	private HashMap<String,Object> props = new HashMap<String, Object>(); 

	private ArrayList<Link> links = new ArrayList<Link>();
	
	//	private Sequencer sequencer;

//	public Song(File file) {
//		name = FilenameUtils.removeExtension(file.getName());
//	}
	
}
