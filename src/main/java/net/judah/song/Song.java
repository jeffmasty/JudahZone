package net.judah.song;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.api.Signature;
import net.judah.drumkit.KitSetup;
import net.judah.fx.PresetsDB;
import net.judah.mixer.Channel;
import net.judah.omni.JsonUtil;
import net.judah.seq.Seq;
import net.judah.seq.chords.Scale;
import net.judah.seq.track.MidiFile;
import net.judah.seq.track.TrackInfo;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Param;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/* Stages of a song.
 * midi track files, fx, scenes(params, bar cycle, progChange, arp) and chordPro file*/
@Getter @Setter @NoArgsConstructor
public class Song {
	@JsonIgnore private File file;

	private Signature timeSig = Signature.FOURFOUR;
	private String bundle;
	@JsonInclude(Include.NON_NULL)
	private String chordpro;
	@JsonInclude(Include.NON_NULL)
	private Key key;
	@JsonInclude(Include.NON_NULL)
	private Scale scale;
	@JsonInclude(Include.NON_NULL)
	private KitSetup kit;

	private List<TrackInfo> tracks = new ArrayList<>();
	private List<Scene> scenes = new ArrayList<>();
	private List<FxData> fx = new ArrayList<>();
	private List<String> capture = new ArrayList<>();

	public Song(Seq seq, int tempo) {
		seq.newSong();
		Scene created = new Scene(seq); // init w/ 10 midi tracks
 		created.getCommands().add(new Param(Cmd.Tempo, "" + tempo));
		scenes.add(created);
	}

	/** @param scene the current scene to save Efx.
	 * @throws IOException
	 * @throws InvalidMidiDataException */
    public void saveSong(Scene scene) throws IOException, InvalidMidiDataException {

    	if (file == null)
    		file = Folders.choose(JudahZone.getSetlists().getDefault());
    	if (file == null) return;

    	// populate top level track name, file(null), and progChange for each MidiTrack
    	tracks.clear();

    	if (bundle == null || bundle.isBlank())
    		JudahZone.getSeq().forEach(track->tracks.add(new TrackInfo(track)));
    	else
			MidiSystem.write(JudahZone.getSeq().bundle(file.getName()), MidiFile.TYPE_1,
					new FileOutputStream(new File(Folders.getMidi(), bundle)));

		fx.clear();
		scene.getFx().clear();
		for (Channel ch : JudahZone.getMixer().getChannels()) {
			if (false == ch.getPreset().getName().equals(PresetsDB.DEFAULT))
				fx.add(new FxData(ch.getName(), ch.getPreset().getName()));
			if (ch.isPresetActive())
				scene.getFx().add(ch.getName());
		}

		int idx = scenes.indexOf(scene);
		if (idx == -1)
			RTLogger.warn(this, "Unknown scene: " + scene);
		if (idx == 0) { // save mutes
			capture.clear();
			JudahZone.getInstruments().forEach(line ->{
				if (!line.isMuteRecord())capture.add(line.getName());});
		}
		JsonUtil.writeJson(this, file);
		RTLogger.log(this, "Saved " + file.getName() + " (" + scenes.indexOf(scene) + ")");
    }

    public void setTimeSig(Signature sig) {
    	this.timeSig = sig;
    	JudahZone.getClock().setTimeSig(sig);
    }

	@Override public String toString() {
		if (file != null)
			return file.getName();
		return "_??";
	}


}
