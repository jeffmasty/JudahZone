package net.judah.song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.fx.PresetsDB;
import net.judah.gui.widgets.FileChooser;
import net.judah.mixer.Channel;
import net.judah.seq.Seq;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

/* Stages of a song.
 * save midi track files, fx, schedule and params */
@Getter @Setter 
public class Song {
	@JsonIgnore private File file;

	private List<TrackInfo> tracks = new ArrayList<>();
	private List<Scene> scenes = new ArrayList<>();
	private List<FxData> fx = new ArrayList<>();
	
	public Song() {
		scenes.add(new Scene(Seq.TRACKS, Trigger.REL)); // init w/ 10 midi tracks 
	}
	
    public void saveSong() {
    	if (file == null)
    		file = FileChooser.choose(Folders.getSetlist());
    	if (file == null) return;
    	fx();
    	tracks();
    	try {
			JsonUtil.writeJson(this, file);
			RTLogger.log(this, "Saved " + file.getName());
		} catch (Exception e) { RTLogger.warn(JudahZone.class, e); }
    }

    	private void tracks() {
    	tracks.clear();
    	JudahZone.getSeq().forEach(track-> {
   			tracks.add(new TrackInfo(track.getName(), 
   					track.getFile() == null ? "" : track.getFile().getAbsolutePath(), 
   							track.getMidiOut().getProg(track.getCh())));});
 	}
	
	private void fx() {
		fx.clear();
		for (Channel ch : JudahZone.getMixer().getAll())
			if (false == ch.getPreset().getName().equals(PresetsDB.DEFAULT)) {
				fx.add(new FxData(ch.getName(), ch.getPreset().getName(), ch.isPresetActive()));
			}
	}

	@Override
	public String toString() {
		if (file != null)
			return file.getName();
		return "_??";
	}


}
