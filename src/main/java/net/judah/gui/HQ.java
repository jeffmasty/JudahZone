package net.judah.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.LengthCombo;
import net.judah.gui.widgets.StartBtn;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.omni.Icons;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.Overview;

public class HQ extends JPanel implements TimeListener {

	private final JudahClock clock;
	private final Looper looper;
	private final Overview songs;
	private final Btn scene = new Btn("", e->trigger());
    private final LengthCombo sync;
	private final JButton metro;

    public HQ(JudahClock clock, Looper loops, Overview songs, ChordTrack chords) {
    	this.clock = clock;
    	this.looper = loops;
    	this.songs = songs;
    	sync = new LengthCombo(clock);
    	metro = new Btn(Icons.get("left.png"), e->clock.skipBar());
    	metro.setOpaque(true);

    	setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    	add(new StartBtn(clock));
    	add(Gui.resize(scene, Size.SMALLER_COMBO));
    	add(Gui.resize(new ChordPlay(chords).makeFancy(), new Dimension(54, Size.STD_HEIGHT)));
    	add(new Btn(" Rec ", e->loops.trigger()));
    	add(sync);
    	add(new Btn("Del", e->looper.delete(), "Clear Looper"));
		add(metro);
    	clock.addListener(this);
    }

	@Override public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			metro.setIcon(clock.isEven() ? Icons.get("left.png") : Icons.get("right.png"));
		metronome();
	}

	private void trigger() {
		if (SetCombo.getSet() != null)
			SetCombo.set();
		else if (songs.getOnDeck() != null)
			songs.setScene(songs.getOnDeck());
		else
			songs.trigger();
		sceneText();
	}

//	void capture() {
//		if (recording == null) {
//			looper.trigger(clock.isActive() ? looper.getLoopA() : looper.getLoopC());
//		}
//		else {
//			recording.capture(false);
//			recording = null;
//		}
//	}

	public void length() {
		if (sync.getSelectedItem() != (Integer)JudahClock.getLength())
			sync.setSelectedItem(JudahClock.getLength());
	}

	public void metronome() {
		metro.setBackground(clock.isActive() == false && clock.isOnDeck() ? Pastels.YELLOW : null);
	}

	void sceneText() {
		StringBuffer sb = new StringBuffer();
		int idx = songs.getSong().getScenes().indexOf(songs.getScene());
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
		scene.setBackground(onDeck ? songs.getOnDeck().getType().getColor() : null);
	}

}
