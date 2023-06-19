package net.judah.gui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.LengthCombo;
import net.judah.gui.widgets.StartBtn;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.seq.MidiConstants;
import net.judah.seq.chords.ChordTrack;
import net.judah.song.SongTab;

public class HQ extends JPanel {
	
	private final JudahClock clock;
	private final Looper looper;
	private final SongTab songs;
	private final Btn scene = new Btn("OpenMic", e->JudahZone.getSongs().trigger());
	private Loop recording;
	private final JButton record;
    private final LengthCombo sync;
    
    public HQ(JudahClock clock, Looper loops, SongTab songs, ChordTrack chords) {
    	this.clock = clock;
    	this.looper = loops;
    	this.songs = songs;
    	record = new Btn("Record", e->record());
    	sync = new LengthCombo(clock);
    	
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	
    	add(new StartBtn(clock));
    	add(scene);
    	add(chords.createBtn(" " + MidiConstants.SHARP + " " + MidiConstants.FLAT + " "));
    	add(Box.createHorizontalGlue());
    	add(record);
    	add(sync);
    	add(new Btn("Del", e->looper.reset()));
    	add(Box.createHorizontalGlue());
    }
    
	void record() {
		if (recording == null) {
			if (clock.isActive()) {
				recording = looper.getLoopA();
				recording.trigger();
			} else {
				recording = looper.getLoopC();
				recording.record(true);
			}
		}
		else {
			recording.record(false);
			recording = null;
		}
	}

	public void length() {
		if (sync.getSelectedItem() != (Integer)clock.getLength())
			sync.setSelectedItem(clock.getLength());
	}
	
	public void sceneText() {
		StringBuffer sb = new StringBuffer();
		int idx = songs.getSong().getScenes().indexOf(songs.getCurrent());
		if (idx == 0)
			sb.append("Home");
		else 
			sb.append("Scene:").append(idx);
		if (songs.getOnDeck() != null) 
				sb.append("|").append(songs.getSong().getScenes().indexOf(songs.getOnDeck()));
		if (SetCombo.getSet() != null)
			sb.append("!");
		scene.setText(sb.toString());
	}

	
}
