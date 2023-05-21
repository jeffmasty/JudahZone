package net.judah.song;

import static net.judah.JudahZone.*;
import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Songs;
import net.judah.gui.widgets.Btn;
import net.judah.mixer.Channel;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.util.RTLogger;

/** left: SongView, right: midi tracks list*/
@Getter
public class SongTab extends JPanel implements TimeListener, Cmdr {
	
	private Song song;
	private Scene current;
	private Scene onDeck;
	private int count;
	private String[] keys;
	private SongView songView;
	private final ArrayList<SongTrack> tracks = new ArrayList<>();
	private final JPanel holder = new JPanel();

	public SongTab() {
		getClock().addListener(this);
		setPreferredSize(TAB_SIZE);
		Dimension listSz = new Dimension((int) (TAB_SIZE.width * 0.59f), TAB_SIZE.height - 100);
		Dimension songSz = new Dimension((int) (TAB_SIZE.width * 0.4f), TAB_SIZE.height);
		holder.setMinimumSize(songSz);
		final JPanel trackPnl = new JPanel();
		trackPnl.setPreferredSize(listSz);
		trackPnl.setMinimumSize(listSz);
		trackPnl.setOpaque(true);
		trackPnl.setLayout(new BoxLayout(trackPnl, BoxLayout.PAGE_AXIS));
		
		getSeq().getTracks().forEach(track-> tracks.add(new SongTrack(track)));
		
		JPanel title = new JPanel();
		title.add(new JLabel("Song"));
		title.add(new Songs()); 
		title.add(new Btn("Save", e->JudahZone.save()));
		title.add(new Btn("Reload", e->JudahZone.reload()));
		trackPnl.add(title);
		trackPnl.add(labels(listSz));

		tracks.forEach(track->trackPnl.add(track));
		trackPnl.add(Box.createVerticalGlue());

		holder.setMinimumSize(songSz);
		
		add(trackPnl);
		add(holder);
		setName(JudahZone.JUDAHZONE);
		
	}
	
	private JPanel labels(Dimension sz) {
		ArrayList<String> lbls = new ArrayList<>(Arrays.asList(new String[]
			{"Track", "    File", "", "    Preset", "", "Cycle", "Init", "Edit", " Current", "Vol"}));
		JPanel result = new JPanel(new GridLayout(0, lbls.size()));
		lbls.forEach(name->result.add(new JLabel(name, JLabel.CENTER)));
		return result;
	}
	
	public void update() {
		songView.update();
		tracks.forEach(track->track.update());
	}

	public void setSong(Song next) {
		song = next;
		setName(song.getFile() == null ? JudahZone.JUDAHZONE : song.getFile().getName());
		holder.removeAll();

		songView = new SongView(song, this);
		holder.add(songView);
		if (song.getScenes().isEmpty())
			song.getScenes().add(new Scene(getSeq()));
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
			getLooper().verseChorus();
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
		else if (onDeck == scene)  // force
			launchScene(scene);
		else {
			onDeck = scene;
			MainFrame.update(onDeck);
		}
		
	}
	
	public void launchScene(Scene s) {
		onDeck = null;
		// commands
		for (Param p : s.getCommands())
			if (runParam(p)) // true = Jump Cmd
				return;
		current = s;
		// track state
		Seq seq = getSeq();
		TrackList tracks = seq.getTracks();
		for (int i = 0; i < current.getTracks().size(); i++)
			if (tracks.size() > i)
				seq.getTracks().get(i).setState(current.getTracks().get(i));
		List<String> fx = current.getFx();
		for (Channel ch : getMixer().getChannels()) {
			if (fx.contains(ch.getName())) {
				if (!ch.isPresetActive())
					ch.setPresetActive(true);
			}
			else {
				if (ch.isPresetActive())
					ch.setPresetActive(false);
			}
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
	public String lookup(int value) {
		if (value >= 0 && value < song.getScenes().size())
			return "" + value;
		return "null"; // fail
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
