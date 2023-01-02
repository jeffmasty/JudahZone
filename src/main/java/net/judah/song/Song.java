package net.judah.song;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;
import net.judah.widgets.FileChooser;

/* Stages of a song.
 * save midi, schedule and params */
@Getter @Setter 
public class Song {
	@JsonIgnore private File file;

	private ArrayList<String> midi = new ArrayList<>();
	private ArrayList<Scene> scenes = new ArrayList<>();

	
	public Song() {
		scenes.add(new Scene(Trigger.INIT, Seq.TRACKS)); // init w/ 10 midi tracks 
	}
	
    public void saveSong() {
    	if (file == null)
    		file = FileChooser.choose(Folders.getSetlist());
    	if (file == null) return;
    	
    	// marshal midi track files
    	LinkedHashSet<Integer> tracks = new LinkedHashSet<>();
    	for (Scene s : scenes) 
    		for (int i = 0; i < s.getTracks().size(); i++)
    			if (s.getTracks().get(i).isActive())
    				tracks.add(i);
    	for (int i : tracks) 
    		JudahZone.getSeq().get(i).save();
    	midi.clear();
    	for (MidiTrack t : JudahZone.getSeq()) 
    		midi.add(t.getFile() == null ? null : t.getFile().getAbsolutePath());
    	
    	try {
			JsonUtil.writeJson(this, file);
			RTLogger.log(JudahZone.class, "Saved " + file.getAbsolutePath());
		} catch (Exception e) { RTLogger.warn(JudahZone.class, e); }
    }

	@Override
	public String toString() {
		if (file != null)
			return file.getName();
		return "_??";
	}


}
