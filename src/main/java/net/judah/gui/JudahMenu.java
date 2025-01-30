package net.judah.gui;

import static net.judah.JudahZone.*;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.LFO;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SongCombo;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;
import net.judah.omni.Threads;
import net.judah.seq.Trax;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlist;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class JudahMenu extends JMenuBar {
	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};
	private final TabZone tabs;

	public JudahMenu(int width, Overview overview, TabZone tabz) {
		this.tabs = tabz;
	    JMenu song = new JMenu("Song");
	    JMenu loops = new JMenu("Looper");
	    JMenu knobs = new JMenu("Knobs");

		Dimension d = new Dimension(width - 8, Size.STD_HEIGHT);
		setPreferredSize(d);
		setSize(d);
		setMinimumSize(d);

    	song.add(new Actionable("Next", e->overview.nextSong()));
		song.add(new Actionable("Save", e->overview.save()));
        song.add(new Actionable("Save As..", e->{
        		File f = Folders.choose(getSetlists().getDefault());
        		if (f == null) return;
        		overview.save(f);
        		Threads.timer(200, () -> {
        			SongCombo.refill();
        			SongCombo.refresh();
        		});;
        	}));
    	song.add(new Actionable("Reload", e->overview.reload()));
        song.add(new Actionable("Load..", e -> overview.loadSong(Folders.choose(getSetlists().getDefault()))));
    	song.add(new Actionable("New", e -> overview.newSong()));

		JMenu setlist = new JMenu("Setlist");
		for (Setlist list : getSetlists())
			setlist.add(new Actionable(list.toString(), e -> getSetlists().setCurrent(list.getSource())));
        song.add(setlist);
    	song.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));

    	Looper looper = getLooper();
	    JMenu erase = new JMenu("Erase");
	    JMenu duplicate = new JMenu("Duplicate");
	    JMenu save = new JMenu("Save");
	    JMenu load = new JMenu("Load");
	    JMenu solotrack = new JMenu("Solo Track");
    	erase.add(new Actionable("All", e->looper.clear()));
    	looper.forEach(loop->erase.add(new Actionable(loop.getName(), e->looper.clear(loop))));
    	looper.forEach(loop->duplicate.add(new Actionable(loop.getName(), e->loop.duplicate())));
    	looper.forEach(loop->save.add(new Actionable(loop.getName(), e-> loop.save())));
    	looper.forEach(loop->load.add(new Actionable(loop.getName(), e-> loop.load(true))));

    	SoloTrack solo = looper.getSoloTrack();
    	getInstruments().forEach(ch->solotrack.add(new Actionable(ch.getName(), e->solo.setSoloTrack(ch))));

    	loops.add(new Actionable("Length..", e->setLength()));
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(save);
    	loops.add(load);
    	loops.add(new Actionable("Solo on/off", e->solo.toggle()));
    	loops.add(solotrack);

		JMenu time = new JMenu("Clock");
		JudahClock clock = getClock();
		time.add(new Actionable("Start/Stop", e->clock.toggle()));
    	time.add(new Actionable("Reset", e->clock.reset()));
    	time.add(new Actionable("Send/Rcv", e->clock.primary()));
    	time.add(new Actionable("Sync2Loop", e->clock.syncToLoop()));
    	time.add(new Actionable("SyncTempo", e->clock.syncTempo(looper.getPrimary())));
    	//time.add(new Actionable("Runners dial zero", e->clock.runnersDialZero()));

    	JMenu lfo = new JMenu("LFO");
    	JMenu delay = new JMenu("Delay");
    	JMenu chorus = new JMenu("Chorus");

    	for (int i = 0; i < TYPE.length; i++) {
    		final int idx = i;
    		lfo.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, LFO.class)));
    		delay.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, Delay.class)));
    		chorus.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, Chorus.class)));
    	}
    	time.add(lfo);
    	time.add(delay);
    	time.add(chorus);
    	time.add(new Actionable("JIT!", e->justInTimeCompiler()));
        time.add(new Actionable("A2J!", e->getMidi().recoverMidi()));

    	for (KnobMode mode : KnobMode.values())
        	knobs.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));

        add(song);
        add(loops);
        add(time);
        add(knobs);
        add(viewMenu());

	}

	private JMenu viewMenu() {
		JMenu views = new JMenu("View");
        views.add(new Actionable("BeatBox", e->{
        	tabs.drumZone();
        }));

        JMenu track = new JMenu("Track");
        for (Trax p : Trax.pianos)
        	track.add(new Actionable(p.getName(), e->tabs.pianoTrack(p)));
        views.add(track);

        views.add(new Actionable("ChordPro..", e-> {
        	if (getChords().isEmpty()) {
	        	if (getChords().load() != null)
	        		tabs.chordSheet();
        	} else tabs.chordSheet();

        }));
        views.add(new Actionable("SheetMusic..", e->{
        	tabs.sheetMusic(Folders.choose(Folders.getSheetMusic()), true);
        }));
        views.add(new Actionable("Detach", e->TabZone.instance.detach()));
        return views;
	}

	private void setLength() {
		String s = Gui.inputBox("Loop Bar Length:");
		if (s == null || s.isBlank()) return;
		try {
			getClock().setLength(Integer.parseInt(s));
		} catch (Throwable t) {
			RTLogger.log(this, "Length not changed.");
		}
	}

}
