package net.judah.gui;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.LengthCombo;
import net.judah.gui.widgets.StartBtn;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.seq.MidiConstants;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.SongTab;

public class HQ extends JPanel implements TimeListener {
	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			metro.setIcon(JudahClock.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
	}
	
	private final JudahClock clock;
	private final Looper looper;
	private final SongTab songs;
	private final Btn scene = new Btn("OpenMic", e->trigger());
	private final Btn record = new Btn("Record", e->record());
	private final Btn tape = new Btn(" ⏺️ ", e->JudahZone.getMains().tape());
    private final LengthCombo sync;
	private Loop recording;
	private final JButton metro;
	
    public HQ(JudahClock clock, Looper loops, SongTab songs, ChordTrack chords) {
    	this.clock = clock;
    	this.looper = loops;
    	this.songs = songs;
    	sync = new LengthCombo(clock);
    	tape.setOpaque(true);
		Gui.resize(scene, Size.SMALLER_COMBO);
    	
    	setLayout(new FlowLayout(FlowLayout.LEFT, 0, 4));
    	add(new StartBtn(clock));
    	metro = new Btn(Icons.get("left.png"), e->clock.skipBar());
		add(metro);
    	add(scene);
    	add(new ChordPlay(" " + MidiConstants.SHARP + " " + MidiConstants.FLAT + " ", chords));
    	add(record);
    	add(sync);
    	add(new Btn("Del", e->looper.clear()));
    	add(tape);
    	clock.addListener(this);
    }
    
	private void trigger() {
		if (SetCombo.getSet() != null)
			SetCombo.set();
		else if (songs.getOnDeck() != null)
			songs.launchScene(songs.getOnDeck());
		else 
			JudahZone.getSongs().trigger();
	}

	void record() {
		if (recording == null) {
			recording = clock.isActive() ? looper.getLoopA() : looper.getLoopC();
			recording.trigger();
		}
		else {
			recording.record(false);
			recording = null;
		}
	}

	public void length() {
		if (sync.getSelectedItem() != (Integer)JudahClock.getLength())
			sync.setSelectedItem(JudahClock.getLength());
	}
	
	public void sceneText() {
		StringBuffer sb = new StringBuffer();
		int idx = songs.getSong().getScenes().indexOf(songs.getCurrent());
		boolean onDeck = songs.getOnDeck() != null;
		if (idx == 0 && !onDeck)
			sb.append("Home");
		else {
			if (songs.getOnDeck() == null && idx < 10)
				sb.append("Scene:");
			sb.append(idx);
			if (songs.getOnDeck() != null) 
				sb.append(" to ").append(songs.getSong().getScenes().indexOf(songs.getOnDeck()));
		}
		if (SetCombo.getSet() != null)
			sb.append("!");
		scene.setText(sb.toString());
	}

	public void update() {
		tape.setBackground(JudahZone.getMains().getTape() == null ? null : Pastels.RED);
	}
	
}
