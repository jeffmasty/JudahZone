package net.judah.song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.fx.PresetsDB;
import net.judah.gui.widgets.FileChooser;
import net.judah.midi.Signature;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.seq.Seq;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.chords.Key;
import net.judah.seq.chords.Scale;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

/* Stages of a song.
 * midi track files, fx, scenes, params and chordPro file*/
@Getter @Setter @NoArgsConstructor
public class Song {
	@JsonIgnore private File file;

	private List<TrackInfo> tracks = new ArrayList<>();
	private List<Scene> scenes = new ArrayList<>();
	private List<FxData> fx = new ArrayList<>();
	@JsonInclude(Include.NON_NULL)
	private Signature timeSig;
	@JsonInclude(Include.NON_NULL)
	private File chordpro;
	@JsonInclude(Include.NON_NULL)
	private Key key;
	@JsonInclude(Include.NON_NULL)
	private Scale scale;
	
	public Song(Seq seq, int tempo) {
		Scene fresh = new Scene(seq);
		fresh.getCommands().add(new Param(Cmd.Tempo, "" + tempo));
		scenes.add(fresh); // init w/ 10 midi tracks 
	}
	
	/** @param scene the current scene to save Efx.*/
    public void saveSong(DJJefe mixer, Seq seq, Scene scene, ChordTrack chordTrack) {
    	if (file == null)
    		file = FileChooser.choose(Folders.getSetlist());
    	if (file == null) return;
    	
    	// populate top level track name, file(null), and progChange for each MidiTrack 
    	tracks.clear();
		seq.forEach(track->tracks.add(new TrackInfo(track)));
		fx.clear();
		scene.getFx().clear();
		for (Channel ch : mixer.getChannels()) {
			if (false == ch.getPreset().getName().equals(PresetsDB.DEFAULT)) 
				fx.add(new FxData(ch.getName(), ch.getPreset().getName()));
			if (ch.isPresetActive()) 
				scene.getFx().add(ch.getName());
		}
    	try {
			JsonUtil.writeJson(this, file);
			RTLogger.log(this, "Saved " + file.getName());
		} catch (Exception e) { RTLogger.warn(JudahZone.class, e); }
    }

    public void setTimeSig(Signature sig) {
    	this.timeSig = sig;
    	JudahZone.getClock().setTimeSig(sig);
    }
    
	@Override
	public String toString() {
		if (file != null)
			return file.getName();
		return "_??";
	}


}
