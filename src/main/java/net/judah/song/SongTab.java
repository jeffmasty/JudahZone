package net.judah.song;

import static net.judah.JudahZone.*;
import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ProcessAudio.Type;
import net.judah.effects.Fader;
import net.judah.effects.LFO.Target;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;

/** left: SongView, right: midi tracks list*/
@Getter
public class SongTab extends JPanel {
	
	private SongView songView;
	private Scene current;
	private Song song;
	private final ArrayList<SongTrack> midi = new ArrayList<>();
	private boolean trigger; 
	private final JPanel holder = new JPanel();

	public SongTab(Seq  seq) {
		setPreferredSize(TAB_SIZE);
		Dimension listSz = new Dimension((int) (TAB_SIZE.width * 0.5f), TAB_SIZE.height - 130);
		final JPanel tracks = new JPanel();
		tracks.setPreferredSize(listSz);
		tracks.setMinimumSize(listSz);
		tracks.setOpaque(true);
		tracks.setLayout(new BoxLayout(tracks, BoxLayout.PAGE_AXIS));
		
		seq.getTracks().forEach(track-> midi.add(new SongTrack(track, seq)));
		midi.forEach(track->tracks.add(track));
		tracks.add(Box.createVerticalGlue());
		add(holder);
		add(tracks);
		setName(JudahZone.JUDAHZONE);
		
	}

	public void update() {
		songView.update();
		midi.forEach(track->track.update());
	}

	public void setSong(Song next) {
		song = next;
		holder.removeAll();
		songView = new SongView(song, this);
		holder.add(songView);
		setName(song.getFile() == null ? JudahZone.JUDAHZONE : song.getFile().getName());
		JudahZone.getFrame().getTabs().title(this);
		MainFrame.setFocus(song.getScenes().get(0));
	}

	public void launchScene(Scene s) {
		current = s;
		for (int i = 0; i < current.getTracks().size(); i++)
			getTrack(i).setState(current.getTracks().get(i));
		execute(current.getParams());
		MainFrame.setFocus(s);
	}

	public void addScene(Scene add) {
		song.getScenes().add(add);
		songView.getLauncher().fill();
		current = add;
		MainFrame.setFocus(add);
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
		return getMixer().getChannels().get(idx);
	}
	
	public void trigger() {
		trigger = !trigger;
	}

	
	private void execute(List<Param> params) {
		
		for (Param p : params)
			switch (p.cmd) {
			case Start:
				if (p.val == 0) getClock().end();
				else  getClock().begin();
				break;
			case Tempo:
				getClock().writeTempo(p.val); // TODO -1 = sync to looper
				break;
			case Length:
				getClock().setLength(p.val);
				break;
			case TimeSig:
				//getClock().setTimeSig(p.val);
				break;
			case TimeCode:
				break;

			case Delete:
				getLooper().get(p.val).erase();
				break;
			case Dup:
				getLooper().get(p.val).duplicate();
				break;
			case Record:
				Loop loop = getLooper().get(p.val);
				if (loop.getType().equals(Type.FREE))
					loop.record(true);
				else 
					loop.getSync().syncUp();
				MainFrame.update(loop);
				break;
			case RecEnd:
				getLooper().get(p.val).record(false);
				break;
			case Solo:
				getLooper().getSoloTrack().solo(p.val > 0);
				break;
			case Sync:
				getLooper().get(p.val).getSync().syncUp();
				break;

			case FadeOut:
				Fader.execute(new Fader(getChannel(p.val), Target.Gain, Fader.DEFAULT_FADE, getChannel(p.val).getVolume(), 0));
				break;
			case FadeIn:
				Fader.execute(new Fader(getChannel(p.val), Target.Gain, Fader.DEFAULT_FADE, 0, 51));
				break;
			case FX:
				getChannel(p.val).toggleFx();
				break;
			case Latch:
				getInstruments().get(p.val).getLatchEfx().latch(getLooper().toArray(new Loop[getLooper().size()]));
				break;
			case Mute:
				getChannel(p.val).setOnMute(true);
				break;
			case Unmute: 
				getChannel(p.val).setOnMute(false);
				break;
			case OffTape:
				getInstruments().get(p.val).setMuteRecord(true);
				break;
			case OnTape:
				getInstruments().get(p.val).setMuteRecord(false);
				break;
			case SoloCh:
				getLooper().getSoloTrack().setSoloTrack(getInstruments().get(p.val));
				MainFrame.update(JudahZone.getMixer());
				break;
			}
	}

	public void update(MidiTrack t) {
		for (SongTrack track : midi) {
			if (track.getTrack() == t)
				track.update();
		}
	}
	
}
