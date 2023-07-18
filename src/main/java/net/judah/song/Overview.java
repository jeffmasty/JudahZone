package net.judah.song;

import static net.judah.JudahZone.*;
import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

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
import net.judah.seq.Seq;
import net.judah.seq.chords.ChordView;
import net.judah.seq.track.MidiTrack;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.song.setlist.Setlists;

/** left: SongView, right: midi tracks list*/
public class Overview extends JPanel implements TimeListener, Cmdr {

	private static final Dimension SCENE_SZ = new Dimension((int) (TAB_SIZE.width * 0.379), TAB_SIZE.height - 14);
	private static final Dimension LIST_SZ = new Dimension((int) (TAB_SIZE.width * 0.62), SCENE_SZ.height - 14);
	private static final Dimension props = new Dimension((int)(Size.WIDTH_TAB * 0.33), (int)(SCENE_SZ.height * 0.21));
	private static final Dimension BTNS = new Dimension((int)(Size.WIDTH_TAB * 0.365), (int)(SCENE_SZ.height * 0.66));

	private final JudahClock clock;
	private final Seq seq;
	private final Looper looper;
	private Song song;
	@Getter private int count;
	@Getter private String[] keys;
	@Getter private SongView songView;
	private final SongTitle songTitle;
	@Getter private final ArrayList<SongTrack> tracks = new ArrayList<>();
	private final JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
	private final JPanel trackPnl = new JPanel();

	public Overview(JudahClock clock, ChordView chords, Setlists setlists, Seq seq, Looper looper) {
		this.clock = clock;
		this.seq = seq;
		this.looper = looper;
		songTitle = new SongTitle(clock, setlists);
		trackPnl.setOpaque(true);
		seq.getTracks().forEach(track-> tracks.add(new SongTrack(track)));
		trackPnl.setLayout(new GridLayout(tracks.size() + 2, 1));
		
		Gui.resize(this, TAB_SIZE);
		Gui.resize(trackPnl, LIST_SZ);
		Gui.resize(holder, SCENE_SZ);
		holder.setBorder(BorderFactory.createLineBorder(Pastels.FADED));
		holder.setBackground(Pastels.BUTTONS);
		trackPnl.add(songTitle);
		for (int i = 0; i < tracks.size(); i++)
			trackPnl.add(tracks.get(i));
		trackPnl.add(chords);

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(trackPnl);
		add(holder);
		setName(JudahZone.JUDAHZONE);
		clock.addListener(this);
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
		keys();
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
		Scene next = getOnDeck();
		setScene(next);
		if (clock.isActive())
			peek(next);
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
			setOnDeck(peek);
			MainFrame.update(peek);
		}
		else if (peek.getType() == Trigger.ABS) 
			setOnDeck(peek);
		else setOnDeck(null);
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

