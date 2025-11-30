package net.judah.song;

import static net.judah.JudahZone.*;
import static net.judah.gui.Size.HEIGHT_TAB;
import static net.judah.gui.Size.TAB_SIZE;
import static net.judah.gui.Size.WIDTH_TAB;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import lombok.Getter;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.SongCombo;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.omni.JsonUtil;
import net.judah.omni.Threads;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.chords.ChordView;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlist;
import net.judah.song.setlist.Setlists;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** left: SongView, right: midi tracks list*/
public class Overview extends Box implements TimeListener {

	public static final Dimension LIST_SZ = new Dimension(WIDTH_TAB - 388, HEIGHT_TAB - 14);
	private static final Dimension SCENE_SZ = new Dimension(WIDTH_TAB - LIST_SZ.width - 1, LIST_SZ.height);
	private static final Dimension PROPS = new Dimension(SCENE_SZ.width - 83, (int)(SCENE_SZ.height * 0.21));
	private static final Dimension BTNS = new Dimension((int)(Size.WIDTH_TAB * 0.365), (int)(SCENE_SZ.height * 0.66));
	private static final Dimension CHORDS = new Dimension(LIST_SZ.width - 8, ChordView.HEIGHT + 5);
	private static final Dimension TRACK = new Dimension(CHORDS.width, 40);

	private final JudahClock clock = getClock();
	private final Seq seq = getSeq();
	private final Looper looper = getLooper();
	private final DJJefe mixer = getMixer();
	private final ChordTrack chords = getChords();
	private final Setlists setlists = getSetlists();
	private final DrumMachine drums = getDrumMachine();

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
	private final Box trackPnl = new Box(BoxLayout.Y_AXIS);
	private final JScrollPane scroll = new JScrollPane(trackPnl);

	public Overview(String title, Seq seq) {
		super(BoxLayout.X_AXIS);
		setName(title);
		songTitle = new SongTitle(clock, this, seq);
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

		refill();
	}

	public void refill() { // track has been added or removed
		trackPnl.removeAll();
		if (!getChords().isEmpty())
			trackPnl.add(Gui.resize(chords.getView(), CHORDS));
		for (DrumTrack drum : drums.getTracks()) {
			trackPnl.add(Gui.resize(new SongTrack(drum), TRACK));
		}
		for (PianoTrack piano : SynthRack.getSynthTracks()) {
			trackPnl.add(Gui.resize(new SongTrack(piano), TRACK));
		}
		trackPnl.invalidate();
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

	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			songTitle.updateBar((int)value);

		if (prop== Property.BEAT) {
			if (steps > 0 && clock.isActive())
				countUp++;
			if (countUp > steps) {
				trigger();
				go();
			} else if (scene != null)
				MainFrame.update(scene);
			return;
		}

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

		for (int idx = 0; idx < schedule.size(); idx++)
			seq.get(idx).setState(schedule.get(idx));

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


    public void setSong(Song smashHit) {

    	song = smashHit;
    	looper.delete();
    	drums.reset();
    	clock.setTimeSig(song.getTimeSig());
    	clock.reset();

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
			result = (Song)JsonUtil.readJson(choose, Song.class);
			result.setFile(choose);
			setSong(result);
		} catch (Exception e) { RTLogger.warn(this, e); }
		return result;
    }

    public void newSong() {
    	setSong(new Song(seq, (int) clock.getTempo()));
    	getInstruments().mutes();
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
    	song.saveSong(mixer, seq, scene, chords);
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
		// TODO
	}

}

//trackPnl.addMouseListener(new MouseAdapter() {
//	@Override public void mouseClicked(MouseEvent e) {
//		if (trackPnl.getComponentAt(e.getPoint()) == trackPnl)
//			new NewTrack(seq); }});
