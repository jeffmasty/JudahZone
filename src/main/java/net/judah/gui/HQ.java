package net.judah.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import judahzone.api.Notification.Property;
import judahzone.api.TimeListener;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.gui.Pastels;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.fx.EffectsRack;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.LengthCombo;
import net.judah.gui.widgets.TransportBtn;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.seq.chords.ChordPlay;
import net.judah.song.Overview;

public class HQ extends JPanel implements TimeListener {

	@Getter private static boolean shift;
	private final JudahClock clock;
	private final Looper looper;
	private final Overview songs;
	private final Btn scene = new Btn("", e->trigger());
    private final LengthCombo sync;
	private final JButton metro;
	private static Btn delete;

    public HQ(JudahClock clock, JudahZone zone) {//Looper loops, Overview songs, Chords chords, JudahScope scope) {
    	this.clock = clock;
    	this.looper = zone.getLooper();
    	this.songs = zone.getOverview();
    	sync = new LengthCombo(clock);
    	metro = new Btn(Icons.get("left.png"), e->clock.skipBar());
    	metro.setOpaque(true);

    	setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    	add(new TransportBtn(clock));
    	add(Gui.resize(scene, Size.SMALLER));
    	add(Gui.resize(new ChordPlay(zone.getChords()).makeFancy(), new Dimension(54, Size.STD_HEIGHT)));
    	add(new Btn(" Rec ", e->looper.trigger()));
    	add(sync);
    	delete = new Btn("Del", e->looper.delete(), "Clear Looper");
    	add(delete);
		add(metro);
    	clock.addListener(this);
    }

    public static void setShift(boolean on) {
    	shift = on;
    	delete.setText(on ? "SET" : "Del");
    	delete.setBackground(on ? Pastels.YELLOW : null);
    	EffectsRack fx = JudahZone.getInstance().getFxRack().getChannel().getGui();
    	fx.getEq().toggle();
    	fx.getReverb().toggle();

    	if (MainFrame.getKnobMode() == KnobMode.LFO)
    		((LFOKnobs)MainFrame.getKnobs()).upperLower();
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

	public void length() {
		if (sync.getSelectedItem() != (Integer)JudahClock.getLength())
			sync.setSelectedItem(JudahClock.getLength());
	}

	public void metronome() {
		if (!clock.isActive() && clock.isOnDeck())
			metro.setBackground(Pastels.YELLOW);
		else metro.setBackground(null);
	}

	public void sceneText() {
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
