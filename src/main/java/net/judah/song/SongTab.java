package net.judah.song;

import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.chords.ChordView;
import net.judah.seq.track.MidiTrack;
import net.judah.song.setlist.Setlists;
import net.judah.util.RTLogger;

/** left: SongView, right: midi tracks list*/
@Getter
public class SongTab extends JPanel implements TimeListener, Cmdr {

	private static final Dimension SCENE_SZ = new Dimension((int) (TAB_SIZE.width * 0.379), TAB_SIZE.height - 14);
	private static final Dimension LIST_SZ = new Dimension((int) (TAB_SIZE.width * 0.62), SCENE_SZ.height - 14);
	private static final Dimension props = new Dimension((int)(Size.WIDTH_TAB * 0.33), (int)(SCENE_SZ.height * 0.2));
	private static final Dimension BTNS = new Dimension((int)(Size.WIDTH_TAB * 0.365), (int)(SCENE_SZ.height * 0.66));

	private final JudahClock clock;
	private final Seq seq;
	private final Looper looper;
	private final DJJefe mixer;
	private Song song;
	private Scene current;
	private Scene onDeck;
	private int count;
	private String[] keys;
	private SongView songView;
	private final SongTitle songTitle;
	private final ArrayList<SongTrack> tracks = new ArrayList<>();
	private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
	private final JPanel trackPnl = new JPanel();

	public SongTab(JudahClock clock, ChordView chords, Setlists setlists, Seq seq, Looper looper, DJJefe mixer) {
		this.clock = clock;
		this.seq = seq;
		this.looper = looper;
		this.mixer = mixer;
		clock.addListener(this);
		songTitle = new SongTitle(clock, setlists);
		trackPnl.setOpaque(true);
		seq.getTracks().forEach(track-> tracks.add(new SongTrack(track)));
		trackPnl.setLayout(new GridLayout(tracks.size() + 2, 1));
		
		Gui.resize(this, TAB_SIZE);
		Gui.resize(trackPnl, LIST_SZ);
		Gui.resize(holder, SCENE_SZ);
		holder.setBorder(BorderFactory.createLineBorder(Pastels.FADED));
		
		trackPnl.add(songTitle);
		for (int i = 0; i < tracks.size(); i++)
			trackPnl.add(tracks.get(i));
		trackPnl.add(chords);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(trackPnl);
		add(holder);
		setName(JudahZone.JUDAHZONE);
	}
	
	public void setSong(Song next) {
		song = next;
		setName(song.getFile() == null ? JudahZone.JUDAHZONE : song.getFile().getName());
		songTitle.setSong(next);
		songTitle.update();
		
		holder.removeAll();
		songView = new SongView(song, this, props, BTNS);
		holder.add(songView);
		if (song.getScenes().isEmpty())
			song.getScenes().add(new Scene(seq));
		repaint();
		launchScene(song.getScenes().get(0));
		keys();
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
		setOnDeck(null);
	}
	
	public void copy() {
		addScene(current.clone());
	}

	public void newScene() {
		addScene(new Scene(current.getTracks()));
	}

	public void delete() {
		if (current == song.getScenes().get(0)) return; // don't remove initial scene
		song.getScenes().remove(current);
		songView.getLauncher().fill();
		current = song.getScenes().get(0);
		MainFrame.setFocus(song.getScenes().get(0));
	}
	
	public SongTrack getTrack(int idx) {
		return tracks.get(idx);
	}

	public void trigger() {
		int next = 1 + song.getScenes().indexOf(current); 
		if (next < song.getScenes().size()) 
			setOnDeck(song.getScenes().get(next));
		else 
			looper.verseChorus();
	}
	
	/** @return true on valid Jump cmd */
	public boolean runParam(Param p) {
		
		if (p.cmd == Cmd.Jump) {
			int idx = Integer.parseInt(p.val);
			if (song.getScenes().size() > idx && getClock().isActive()) {
				Scene next = song.getScenes().get(idx);
				launchScene(next);
				peek(next);
			}
			return true;
		}
		Cmd.getCmdr(p.cmd).execute(p);
		return false;
	}			

	private void go() { 
		if (onDeck != null) {
			Scene next = onDeck;
			launchScene(next);
			if (getClock().isActive() && onDeck == null)
				peek(next);
		}
		
	}
	
	private void peek(Scene current) {
		int next = 1 + song.getScenes().indexOf(current); 
		if (next >= song.getScenes().size()) {
			setOnDeck(null);
			return;
		}
		Scene peek = song.getScenes().get(next);
		if (peek.getType() == Trigger.REL) {
			count = 0;
			onDeck = peek;
			MainFrame.update(onDeck);
		}
		else if (peek.getType() == Trigger.ABS) 
			setOnDeck(peek);
		else setOnDeck(null);
	}

	
	public void setOnDeck(Scene scene) {
		if (scene == null) 
			onDeck = null;
		else if (scene.getType() == Trigger.HOT)
			launchScene(scene);
		else if (onDeck == scene || !clock.isActive())  // force
			launchScene(scene);
		else {
			onDeck = scene;
			MainFrame.update(onDeck);
		}
		
	}
	
	public void launchScene(Scene s) {
		onDeck = null;
		clock.reset();
		// commands
		for (Param p : s.getCommands())
			if (runParam(p) && clock.isActive()) // true = Jump Cmd
				return;
		current = s;
		// track state
		TrackList tracks = seq.getTracks();
		for (int i = 0; i < current.getTracks().size(); i++)
			if (tracks.size() > i)
				seq.getTracks().get(i).setState(current.getTracks().get(i));
		List<String> fx = current.getFx();
		for (Channel ch : mixer.getChannels()) {
			if (fx.contains(ch.getName())) {
				if (!ch.isPresetActive())
					ch.setPresetActive(true);
			}
			else if (ch.isPresetActive())
					ch.setPresetActive(false);
		}
		MainFrame.setFocus(current);
	}
	
	@Override public void update(Property prop, Object value) {
		if (onDeck == null) 
			return;
		if (prop == Property.BARS && onDeck.type == Trigger.BAR)	
			go();
		else if (prop == Property.LOOP && onDeck.type == Trigger.LOOP) 
			go();
		else if (onDeck.type == Trigger.ABS && prop == Property.BEAT) {
			if ((int)value >= onDeck.getCommands().getTimeCode())
				go();
		}
		else if (onDeck.type == Trigger.REL && prop == Property.BEAT) {
			if (++count >= onDeck.getCommands().getTimeCode()) 
				go();
			else 
				MainFrame.update(onDeck);
		}
		
	}

	public void update() {
		songTitle.update();
		songView.update();
		tracks.forEach(track->track.update());
	}


	public void update(MidiTrack t) {
		for (SongTrack track : tracks) 
			if (track.getTrack() == t)
				track.update();
	}

	private void keys() {
		keys = new String[song.getScenes().size()];
		for (int i = 0; i < keys.length; i++)
			keys[i] = "" + i;
	}
	
	@Override
	public Object resolve(String key) {
		return song.getScenes().get(Integer.parseInt(key));
	}

	@Override
	public void execute(Param p) {
		// no-op
	}

}

