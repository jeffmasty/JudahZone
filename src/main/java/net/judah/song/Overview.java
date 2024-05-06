package net.judah.song;



import static net.judah.JudahZone.*;
import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.seq.Seq;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Param;
import net.judah.song.cmd.SceneProvider;
import net.judah.song.setlist.Setlists;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

/** left: SongView, right: midi tracks list*/
public class Overview extends JPanel implements TimeListener {

	private static final Dimension SCENE_SZ = new Dimension((int) (TAB_SIZE.width * 0.379), TAB_SIZE.height - 14);
	private static final Dimension LIST_SZ = new Dimension((int) (TAB_SIZE.width * 0.62), SCENE_SZ.height);
	private static final Dimension props = new Dimension((int)(Size.WIDTH_TAB * 0.275), (int)(SCENE_SZ.height * 0.21));
	private static final Dimension BTNS = new Dimension((int)(Size.WIDTH_TAB * 0.365), (int)(SCENE_SZ.height * 0.66));

	private final JudahClock clock;
	private final Seq seq;
	private final Looper looper;
	private final DJJefe mixer;
	private final ChordTrack chords;

	@Getter private Song song;
	@Getter private Scene scene;
	@Getter private Scene onDeck;
	@Getter private int count;
	@Getter private String[] keys;
	@Getter private SongView songView;
	@Getter private final ArrayList<SongTrack> tracks = new ArrayList<>();
	private final SongTitle songTitle;
	private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
	private final JPanel trackPnl = new JPanel();

	public Overview(JudahClock clock, ChordTrack chords, Setlists setlists, Seq seq, Looper looper, DJJefe mixer) {
		this.clock = clock;
		this.seq = seq;
		this.looper = looper;
		this.mixer = mixer;
		this.chords = chords;
		songTitle = new SongTitle(clock, setlists, this);
		trackPnl.setOpaque(true);
		seq.getTracks().forEach(track-> tracks.add(new SongTrack(track)));
		trackPnl.setLayout(new GridLayout(tracks.size() + 2, 1, 1, 1)); // +2 = title and chordTrack
		
		Gui.resize(this, TAB_SIZE);
		Gui.resize(trackPnl, LIST_SZ);
		Gui.resize(holder, SCENE_SZ);
		holder.setBorder(BorderFactory.createLineBorder(Pastels.FADED));
		holder.setBackground(Pastels.BUTTONS);
		trackPnl.add(songTitle);
		for (int i = 0; i < tracks.size(); i++)
			trackPnl.add(tracks.get(i));
		trackPnl.add(chords.getView());

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(trackPnl);
		add(holder);
		setName(JudahZone.JUDAHZONE);
		clock.addListener(this);
	}
	
	public SongTrack getTrack(int idx) {
		return tracks.get(idx);
	}

	public void trigger() {
		int next = 1 + song.getScenes().indexOf(getScene()); 
		if (next < song.getScenes().size()) 
			setOnDeck(song.getScenes().get(next));
		else 
			looper.verseChorus();
	}
	
	/** @return true on valid Jump cmd */
	public boolean runParam(Param p) {
		
		if (p.cmd == Cmd.Jump) {
			int idx = Integer.parseInt(p.val);
			if (song.getScenes().size() > idx && clock.isActive()) {
				Scene next = song.getScenes().get(idx);
				setScene(next);
				peek(next);
			}
			return true;
		}
		Cmd.getCmdr(p.cmd).execute(p);
		return false;
	}			

	private void go() { 
		Scene next = onDeck;
		setScene(onDeck);
		if (clock.isActive())
			peek(next);
	}
	
	private void peek(Scene current) {
		int next = 1 + song.getScenes().indexOf(current); 
		if (next >= song.getScenes().size()) {
			onDeck = null;
			return;
		}
		Scene peek = song.getScenes().get(next);
		if (peek.getType() == Trigger.REL) {
			count = 0;
			setOnDeck(peek);
			// MainFrame.update(peek);
		}
		else if (peek.getType() == Trigger.ABS) 
			setOnDeck(peek);
		else onDeck = null; 
	}

	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			songTitle.updateBar((int)value);

		if (getOnDeck() == null) 
			return;
		Scene onDeck = getOnDeck();
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
		songView.getLauncher().update();
		tracks.forEach(track->track.update());
	}


	public void update(MidiTrack t) {
		for (SongTrack track : tracks) 
			if (track.getTrack() == t)
				track.update();
	}

    public void setScene(Scene s) {
    	Scene old = scene;
    	scene = s;
		clock.reset();

    	// commands
		for (Param p : s.getCommands())
			if (runParam(p) && clock.isActive()) // true = Jump Cmd
				return;

		// track state (bar, progChange, arp)
		List<Sched> schedule = scene.getTracks();
		for (int idx = 0; idx < schedule.size(); idx++) 
			seq.get(idx).setState(schedule.get(idx));
		
		Constants.execute(()->{
			List<String> fx = scene.getFx();
			for (Channel ch : mixer.getChannels()) {
				if (fx.contains(ch.getName())) {
					if (!ch.isPresetActive())
						ch.setPresetActive(true);
				}
				else if (ch.isPresetActive())
					ch.setPresetActive(false);
			}
		});
		
    	onDeck = null;
		if (old != null)
    		MainFrame.update(old);
		MainFrame.update(scene);
    	
    }
    
    public void setOnDeck(Scene scene) {
		if (scene == null)  
			onDeck = null;
		else if (scene.getType() == Trigger.HOT)
			setScene(scene);
		else if (onDeck == scene || !clock.isActive())  // force
			setScene(scene);
		else {
			onDeck = scene;
			MainFrame.update(scene);
		}
	}
    
    public void setSong(Song smashHit) {
    	looper.clear();
    	clock.reset();
    	
    	getDrumMachine().reset();

    	song = smashHit;
    	clock.reset(song.getTimeSig());
    	seq.loadSong(song.getTracks());
    	mixer.loadFx(song.getFx());
    	mixer.mutes(song.getRecord());
    	chords.load(song);

		setName(song.getFile() == null ? JudahZone.JUDAHZONE : song.getFile().getName());
		songTitle.setSong(song);
		songTitle.update();
		
		holder.removeAll();
		songView = new SongView(song, this, props, BTNS);
		holder.add(songView);
		if (song.getScenes().isEmpty())
			song.getScenes().add(new Scene(seq));
		SceneProvider.generate();
    	setScene(song.getScenes().get(0));

    	// load sheet music if song name matches an available sheet music file
    	if (song.getFile() != null) {
    		String name = song.getFile().getName();
    		for (File f : Folders.getSheetMusic().listFiles())
    			if (f.getName().startsWith(name))
    				getFrame().sheetMusic(f);
    	}
    	getFrame().getTabs().title(this);
    	SongCombo.refresh();
    }
    
    public void save(File f) {
    	song.setFile(f);
    	save();
    }
    
    public void save() {
    	if (song.getFile() == null) {
    		song.setFile(FileChooser.choose(getSetlists().getDefault()));
    		if (song.getFile() == null)
    			SongCombo.refill();
    			return;
    	}
    	song.saveSong(mixer, seq, scene, chords);
    	getFrame().getTabs().title(this);
    }

    /** reload from disk, re-set current scene */
    public void reload() {
		int idx = song.getScenes().indexOf(scene);
		Song newSong = loadSong(song.getFile());
		if (newSong == null)
			return;
		setScene(song.getScenes().get(0)); // home tempo, etc.
		if (song.getScenes().size() < idx)
			setScene(song.getScenes().get(idx));
    }
    
    public Song loadSong(File f) {
    	if (f == null)
    		f = FileChooser.choose(getSetlists().getDefault());
    	if (f == null) return null;
    	Song result = null;
		try {
			result = (Song)JsonUtil.readJson(f, Song.class);
			result.setFile(f);
			setSong(result);
		} catch (Exception e) {
			RTLogger.warn(JudahZone.class, e);
		}
		return result;
    }

    public void nextSong() {
    	JComboBox<?> setlist = getMidiGui().getSongsCombo();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong((File)setlist.getSelectedItem());
	}


}

