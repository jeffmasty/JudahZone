package net.judah.song;

import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ProcessAudio.Type;
import net.judah.fx.Fader;
import net.judah.fx.LFO.Target;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Songs;
import net.judah.gui.widgets.Btn;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.Signature;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Zone;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.util.RTLogger;

/** left: SongView, right: midi tracks list*/
@Getter
public class SongTab extends JPanel {
	
	private final Looper looper;
	private final DJJefe mixer;
	private final JudahClock clock;
	private final Zone instruments;
	private final Seq seq;
	
	private SongView songView;
	private Scene current;
	private Song song;
	private final ArrayList<SongTrack> midi = new ArrayList<>();
	private final JPanel holder = new JPanel();
	

	public SongTab(Seq  sequencer, Looper looper, DJJefe mixer, Zone lineIn) {
		this.looper = looper;
		this.clock = looper.getClock();
		this.mixer = mixer;
		this.instruments = lineIn;
		this.seq = sequencer;
		
		setPreferredSize(TAB_SIZE);
		Dimension listSz = new Dimension((int) (TAB_SIZE.width * 0.59f), TAB_SIZE.height - 130);
		Dimension songSz = new Dimension((int) (TAB_SIZE.width * 0.4f), TAB_SIZE.height);
		holder.setMinimumSize(songSz);
		final JPanel tracks = new JPanel();
		tracks.setPreferredSize(listSz);
		tracks.setMinimumSize(listSz);
		tracks.setOpaque(true);
		tracks.setLayout(new BoxLayout(tracks, BoxLayout.PAGE_AXIS));
		seq.getTracks().forEach(track-> midi.add(new SongTrack(track, seq)));
		
		JPanel title = new JPanel();
		title.add(new JLabel("Song"));
		title.add(new Songs()); 
		title.add(new Btn("Save", e->JudahZone.save()));
		tracks.add(title);
		midi.forEach(track->tracks.add(track));
		tracks.add(Box.createVerticalGlue());
		holder.setMinimumSize(songSz);

		add(tracks);
		add(holder);
		setName(JudahZone.JUDAHZONE);
		
	}
	
	public void update() {
		songView.update();
		midi.forEach(track->track.update());
	}

	public void setSong(Song next) {
		song = next;
		setName(song.getFile() == null ? JudahZone.JUDAHZONE : song.getFile().getName());
		holder.removeAll();
		songView = new SongView(song, this);
		holder.add(songView);
		if (song.getScenes().isEmpty())
			song.getScenes().add(new Scene(Seq.TRACKS));
		launchScene(song.getScenes().get(0));
	}

	public void launchScene(Scene s) {
		current = s;
		// tracks state
		for (int i = 0; i < current.getTracks().size(); i++)
			getTrack(i).setState(current.getTracks().get(i));
		// commands
		for (Param p : current.getCommands())
			execute(p);
		
		MainFrame.setFocus(current);
	}

	public void shift(boolean left) {
		List<Scene> scenes = song.getScenes();
		int old = scenes.indexOf(current);
		if (old == 0) { 
			RTLogger.log(this, "INIT Scene is fixed.");
			return; 
		}
		int idx = old + (left ? -1 : 1);
		if (idx == 0)
			idx = scenes.size() - 1;
		if (idx == scenes.size())
			idx = 1;
		Collections.swap(scenes, old, idx);
		songView.getLauncher().fill();
		MainFrame.setFocus(current);
	}
	
	public void addScene(Scene add) {
		current = add;
		song.getScenes().add(add);
		MainFrame.setFocus(songView.getLauncher());
		MainFrame.setFocus(add);
		songView.setOnDeck(null);
	}
	
	public void copy() {
		addScene(current.clone());
	}

	public void newScene() {
		addScene(new Scene(JudahZone.getSeq().state()));
	}

	public void delete() {
		if (current == song.getScenes().get(0)) return; // don't remove initial scene
		song.getScenes().remove(current);
		songView.getLauncher().fill();
		current = song.getScenes().get(0);
		MainFrame.setFocus(song.getScenes().get(0));
	}
	
	public SongTrack getTrack(int idx) {
		return midi.get(idx);
	}

	private Channel getChannel(int idx) {
		if (idx < 0)
			return JudahZone.getMains();
		return mixer.getChannels().get(idx);
	}
	
	public void trigger() {
		int next = 1 + song.getScenes().indexOf(current); 
		if (next < song.getScenes().size()) 
			songView.setOnDeck(song.getScenes().get(next));
		else 
			looper.verseChorus();
	}
	
	private void execute(Param p) {
		
		switch (p.cmd) {
			case Start:
				if (p.val == 0) clock.end();
				else  clock.begin();
				break;
			case Tempo:
				if (p.val == -1)
					clock.syncToLoop();
				else 
					clock.writeTempo(p.val); 
				break;
			case Length:
				clock.setLength(p.val);
				break;
			case TimeSig:
				clock.setTimeSig(Signature.values()[p.val]);
				break;
			case TimeCode:
				break;

			case Delete:
				looper.get(p.val).erase();
				break;
			case Dup:
				looper.get(p.val).duplicate();
				break;
			case Record:
				if (!clock.isActive())
					break; // ignore in edit mode
				Loop loop = looper.get(p.val);
				loop.record(true);
				if (loop.getType() != Type.FREE)
					clock.syncUp(loop, 0);
				MainFrame.update(loop);
				break;
			case RecEnd:
				looper.get(p.val).record(false);
				break;
			case Solo:
				looper.getSoloTrack().solo(p.val > 0);
				break;
			case Sync:
				looper.onDeck(getLooper().get(p.val));
				break;

			case FadeOut:
				Channel ch = getChannel(p.val);
				Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				break;
			case FadeIn:
				Fader.execute(new Fader(getChannel(p.val), Target.Gain, Fader.DEFAULT_FADE, 0, 51));
				break;
			case FX:
				getChannel(p.val).setPresetActive(!getChannel(p.val).isPresetActive());
				break;
			case Latch:
				instruments.get(p.val).getLatchEfx().latch();
				break;
			case Mute:
				getChannel(p.val).setOnMute(true);
				break;
			case Unmute: 
				getChannel(p.val).setOnMute(false);
				break;
			case OffTape:
				instruments.get(p.val).setMuteRecord(true);
				break;
			case OnTape:
				instruments.get(p.val).setMuteRecord(false);
				break;
			case SoloCh:
				looper.getSoloTrack().setSoloTrack(instruments.get(p.val));
				break;
			case MPK:
				JudahZone.getMidi().setKeyboardSynth(seq.getSynthTracks().get(p.val));
		}
	}

	public void update(MidiTrack t) {
		for (SongTrack track : midi) {
			if (track.getTrack() == t)
				track.update();
		}
	}

}
