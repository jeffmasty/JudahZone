package net.judah.song;

import static net.judah.gui.Size.HEIGHT_TAB;
import static net.judah.gui.Size.TAB_SIZE;
import static net.judah.gui.Size.WIDTH_TAB;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import judahzone.api.FX;
import judahzone.api.Notification.Property;
import judahzone.gui.Gui;
import judahzone.gui.Nimbus;
import judahzone.gui.Pastels;
import judahzone.api.Signature;
import judahzone.api.TimeListener;
import judahzone.util.Folders;
import judahzone.util.JsonUtil;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.SongCombo;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.mixer.DJJefe;
import net.judah.seq.Seq;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.MidiTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlist;
import net.judah.song.setlist.Setlists;

/** left: SongView, right: midi tracks list*/
public class Overview extends Box implements TimeListener {

	public static final int TRACK_HEIGHT = 35;
	public static final Dimension LIST_SZ = new Dimension(WIDTH_TAB - 388, HEIGHT_TAB - 14);
	public static final Dimension TRACK = new Dimension(LIST_SZ.width - Nimbus.SCROLL_BTN, TRACK_HEIGHT);
	public static final Dimension CHORDS = new Dimension(TRACK.width, ChordTrack.HEIGHT + 5);

	private static final Dimension SCENE_SZ = new Dimension(WIDTH_TAB - LIST_SZ.width - 1, LIST_SZ.height);
	private static final Dimension PROPS = new Dimension(SCENE_SZ.width - 83, (int)(SCENE_SZ.height * 0.21));
	private static final Dimension BTNS = new Dimension((int)(Size.WIDTH_TAB * 0.365), (int)(SCENE_SZ.height * 0.66));

	private final JudahClock clock;
	private final Seq seq;
	private final Looper looper;
	private final DJJefe mixer;
	private final Setlists setlists;
	private final DrumMachine drums;

	/** The Current SmashHit */
	@Getter private Song song;
	/** current scene */
	@Getter private Scene scene;
	/** scene to goto */
	@Getter private Scene onDeck;
	@Getter private int steps;
	@Getter private int countUp;
	@Getter private SongView songView;
	private final SongTitle songTitle;
	private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
	private final TrackPanel trackPnl;
	private final JScrollPane scroll;
	private final JudahZone zone;

	public Overview(String title, JudahZone judahZone) {
		super(BoxLayout.X_AXIS);
		setName(title);
		this.zone = judahZone;
		trackPnl = new TrackPanel(zone.getSeq().getAutomation());
		scroll = new JScrollPane(trackPnl);
		clock = JudahMidi.getClock();
		seq = zone.getSeq();
		looper = zone.getLooper();
		mixer = zone.getMixer();
		setlists = zone.getSetlists();
		drums = zone.getDrumMachine();

		songTitle = new SongTitle(this, zone);
		Gui.resize(this, TAB_SIZE);
		Gui.resize(trackPnl, LIST_SZ);
		Gui.resize(holder, SCENE_SZ);

		holder.setBorder(BorderFactory.createLineBorder(Pastels.FADED));
		holder.setBackground(Pastels.BUTTONS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		Box left = new Box(BoxLayout.Y_AXIS);
		left.add(songTitle);
		left.add(scroll);

		add(left);
		add(holder);
		clock.addListener(this);

	}

	public void refill() { // track has been added or removed
		songTitle.update();
		trackPnl.refill(seq.getTracks());
		repaint();
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
			}
			return true;
		}
		Cmd.getCmdr(p.cmd).execute(p);
		return false;
	}

	private void go() {
		setScene(onDeck);
	}

    public void setScene(Scene s) {
    	Scene old = scene;
    	scene = s;
		countUp = 0;

    	// commands
		for (Param p : s.getCommands())
			if (runParam(p) && clock.isActive()) // true = Jump Cmd
				return;

		steps = scene.getCommands().getBeats();

		// track state (bar, progChange, arp)
		List<Sched> schedule = scene.getTracks();

		for (int idx = 0; idx < schedule.size() && idx < seq.numTracks() ; idx++) {
			if (idx >= seq.numTracks())
				RTLogger.warn("Overview overflow", idx + " track not found in Seq:" + seq.numTracks() );
			else
				seq.get(idx).setState(schedule.get(idx));
		}
		Threads.execute(()->{
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

		MainFrame.setFocus(scene);
		if (clock.isActive())
			clock.reset(); // TODO new SCENE notification ?
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

    private void setName() {
		setName(song.getFile() == null ? getName() : song.getFile().getName());
    	TabZone.instance.title(this);
    }


    private void setSong(Song smashHit) {

    	song = smashHit;
    	looper.delete();
    	drums.reset();
    	clock.setTimeSig(song.getTimeSig());
    	clock.reset();

		zone.getChords().load(song);

    	seq.loadSong(song); // + chords

    	mixer.loadFx(song.getFx());
    	mixer.mutes(song.getCapture());

		songTitle.update();

		holder.removeAll();
		songView = new SongView(song, this, PROPS, BTNS);
		holder.add(songView);
		if (song.getScenes().isEmpty())
			song.getScenes().add(new Scene(seq));
		if (song.getKit() != null)
			drums.getSettings().clone(song.getKit());
		else
			drums.getSettings().reset();

    	setScene(song.getScenes().get(0));
    	setName();
    	SongCombo.refresh();

    	// load sheet music if song name matches an available sheet music file
    	TabZone.instance.sheetMusic(song);
    }

    public Song loadSong(final File input) {
		File choose = input;
    	if (choose == null)
    		choose = Folders.choose(setlists.getDefault());
    	if (choose == null)
    		return null;

    	Song result = null;
    	try {
			result = JsonUtil.readJson(choose, Song.class);
			result.attachRuntime(zone);
			result.setFile(choose);
			setSong(result);
		} catch (Exception e) { RTLogger.warn(this, e); }
		return result;
    }

    public void newSong() {
    	setSong(new Song(zone, (int) clock.getTempo()));
    	zone.getInstruments().mutes(zone);
    	setName();
    }

    /** reload from disk, re-set current scene */
    public void reload() {
		if (song.getFile() == null)
			return;
		final int idx = song.getScenes().indexOf(scene);
		Song result = loadSong(song.getFile());
		if (result != null && result.getScenes().size() > idx)
			setScene(result.getScenes().get(idx));
    }

    public void nextSong() {
    	JComboBox<?> setlist = songTitle.getSongs();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong((File)setlist.getSelectedItem());
	}

    public void save(File f) {
    	song.setFile(f);
    	save();
    }

    public void save() {
    	if (song.getFile() == null) {
    		saveAs();
    		return;
    	}
    	try {
    		song.saveSong(scene);
    	} catch (Exception e)  { RTLogger.warn("Song", e); }

    	setName();
    }

	public void saveAs() {
		Setlist current = setlists.getCurrent();
		File f = Folders.choose(current.getSource());
		if (f == null)
			return;
		save(f);
		SongCombo.refresh(current.array(), f);
	}

	public void bundle() {
		if (seq.confirmBundle()) {
			String random = new String("" + UUID.randomUUID().getLeastSignificantBits()).substring(1);
			song.setBundle(random);
			save();
		}
	}

	public ChordTrack getChords() {
		return songTitle.getChordTrack();
	}

	public void update(MidiTrack t) {
		trackPnl.update(t);
	}

	@Override public void update(Property prop, Object value) {

		// CC cascade
		if (prop == Property.STEP) {
			if (clock.isActive() == false)
				return;
			for (SongTrack s : trackPnl)
				s.step((int)value);
			getMains().step((int)value);
		}
		else if (prop == Property.SIGNATURE) {
			for (SongTrack s : trackPnl)
				if (s.getCcTrack() != null)
					s.getCcTrack().timeSig((Signature)value);
		}

		else if (prop == Property.BEAT) {
			if (steps > 0 && clock.isActive())
				countUp++;
			if (countUp > steps) {
				trigger();
				go();
			} else if (scene != null)
				MainFrame.update(scene);
			return;
		}

		// scene
		if (onDeck == null)
			return;
		if (prop == Property.BARS && onDeck.type == Trigger.BAR)
			go();
		else if (prop == Property.BOUNDARY && onDeck.type == Trigger.LOOP)
			go();

	}

	public void update() {
		songTitle.update();
		songView.getLauncher().update();
	}

	public void update(Update type, Computer c) {
		SongTrack target = trackPnl.getTrack(c);
		if (target != null)
			target.update(type);
		else if (c == seq.getMains() && songTitle.isMainsShowing())
			songTitle.getMains().update(type);
	}

	public void update(Channel ch) {
		if (ch == seq.getMains().getChannel()) {
			if (songTitle.isMainsShowing())
				songTitle.getMains().update();
		}
		else {
			SongTrack s = trackPnl.getTrack(ch);
			if (s != null)
				s.update();
		}
	}

	public CCTrack getMains() {
		return songTitle.getMains().getCcTrack();
	}

	public void update(Channel ch, FX fx) {
		SongTrack s = trackPnl.getTrack(ch);
		if (s != null)
			s.update();
	}


}
