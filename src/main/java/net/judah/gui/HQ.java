package net.judah.gui;

import static net.judah.JudahZone.*;

import java.awt.Dimension;
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
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.Overview;

public class HQ extends JPanel implements TimeListener {
	
	private final JudahClock clock;
	private final Looper looper;
	private final Overview songs;
	private final Btn scene = new Btn("OpenMic", e->trigger());
	private final Btn record = new Btn("Record", e->record());
    private final LengthCombo sync;
	private Loop recording;
	private final JButton metro;
	
    public HQ(JudahClock clock, Looper loops, Overview songs, ChordTrack chords) {
    	this.clock = clock;
    	this.looper = loops;
    	this.songs = songs;
    	sync = new LengthCombo(clock);
		Gui.resize(scene, Size.SMALLER_COMBO);
    	
    	setLayout(new FlowLayout(FlowLayout.LEFT, 0, 2));
    	add(new StartBtn(clock));
    	metro = new Btn(Icons.get("left.png"), e->clock.skipBar());
    	metro.setOpaque(true);
		add(metro);
    	add(scene);
    	add(Gui.resize(new ChordPlay(chords).makeFancy(), new Dimension(54, Size.STD_HEIGHT)));
    	add(record);
    	add(sync);
    	add(new Btn("Del", e->looper.clear()));
    	clock.addListener(this);
    }
    
	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			metro.setIcon(JudahClock.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
		metro.setBackground(clock.isActive() == false && clock.isOnDeck() ? Pastels.YELLOW : null);
	}
    
	private void trigger() {
		if (SetCombo.getSet() != null)
			SetCombo.set();
		else if (getOnDeck() != null)
			setScene(JudahZone.getOnDeck());
		else 
			songs.trigger();
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
		int idx = getSong().getScenes().indexOf(getScene());
		boolean onDeck = getOnDeck() != null;
		if (idx == 0 && !onDeck)
			sb.append("Home");
		else {
			if (getOnDeck() == null && idx < 10)
				sb.append("Scene:");
			sb.append(idx);
			if (getOnDeck() != null) 
				sb.append(" to ").append(getSong().getScenes().indexOf(getOnDeck()));
		}
		if (SetCombo.getSet() != null)
			sb.append("!");
		scene.setText(sb.toString());
	}


}
