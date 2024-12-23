package net.judah.gui;

import static net.judah.JudahZone.*;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import lombok.Getter;
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.LFO;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SongCombo;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;
import net.judah.omni.Threads;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlist;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class JudahMenu extends JMenuBar {
	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};

	/** If true, focus on sheet music tab when songs load */
	@Getter private final JCheckBoxMenuItem sheets = new JCheckBoxMenuItem("Sheets");

	public static class Actionable extends JMenuItem {
		public Actionable(String lbl, ActionListener l) {
			super(lbl);
			addActionListener(l);
		}
		public Actionable(String lbl, ActionListener l, int mnemonic) {
			this(lbl, l);
			setMnemonic(mnemonic);
		}
	}

	public JudahMenu(int width) {

		JMenu song = new JMenu("Song");
		JMenu setlist = new JMenu("Setlist");
	    JMenu loops = new JMenu("Looper");
	    JMenu erase = new JMenu("Erase");
	    JMenu duplicate = new JMenu("Duplicate");
	    JMenu save = new JMenu("Save");
	    JMenu load = new JMenu("Load");
	    JMenu solotrack = new JMenu("Solo Track");
	    JMenu tools = new JMenu("Tools");
	    JMenu knobs = new JMenu("View");

		Dimension d = new Dimension(width - 8, Size.STD_HEIGHT);
		setPreferredSize(d);
		setSize(d);
		setMinimumSize(d);

		Overview overview = getOverview();
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
    	for (Setlist list : getSetlists())
			setlist.add(new Actionable(list.toString(), e -> getSetlists().setCurrent(list.getSource())));
        song.add(setlist);
    	song.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));

    	Looper looper = getLooper();
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

    	for (KnobMode mode : KnobMode.values())
        	knobs.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));

    	tools.add(new Actionable("Record Session", e -> getMains().tape(true)));
        sheets.setSelected(false);
        sheets.setToolTipText("Focus on Song SheetMusic");
        tools.add(sheets);
        tools.add(new Actionable("SheetMusic..", e->{
        	getFrame().sheetMusic(Folders.choose(Folders.getSheetMusic()));
        }));
        tools.add(new Actionable("ChordPro..", e-> {
        	if (getChords().load() != null)
        		getFrame().getTabs().setSelectedComponent(getChords().getChordSheet());
        }));
    	tools.add(new Actionable("JIT!", e->justInTimeCompiler()));
        tools.add(new Actionable("A2J!", e->getMidi().recoverMidi()));
        tools.add(new Actionable("Scope", e->getFrame().getTabs().scope()));
        add(song);
        add(loops);
        add(time);
        add(knobs);
        add(tools);

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
