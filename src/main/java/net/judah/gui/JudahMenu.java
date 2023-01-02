package net.judah.gui;

import static net.judah.JudahZone.*;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.judah.controllers.KnobMode;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.song.Song;
import net.judah.util.Folders;
import net.judah.util.RTLogger;
import net.judah.widgets.FileChooser;

public class JudahMenu extends JMenuBar {
	
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
	
	public JudahMenu(int width, Looper looper) {
		JMenu song = new JMenu("Song");
		JMenu setlist = new JMenu("Setlist");
	    JMenu loops = new JMenu("Looper");
	    JMenu erase = new JMenu("Erase");
	    JMenu duplicate = new JMenu("Duplicate");
	    JMenu control = new JMenu("Controls");
	    JMenu views = new JMenu("Views");

		Dimension d = new Dimension(width - 8, Size.STD_HEIGHT);
		setPreferredSize(d);
		setSize(d);
		setMinimumSize(d);
		
        for (File f : Folders.getSetlists()) 
			setlist.add(new Actionable(f.getName(), e -> Folders.setSetlist(f)));
        	
		song.add(new Actionable("Next", e->nextSong()));
        song.add(new Actionable("Save", e->save()));
        song.add(new Actionable("Save As..", e->{
        		File f = FileChooser.choose(Folders.getSetlist());
        		if (f == null) return;
        		save(f);
        	}));
    	song.add(new Actionable("Reload", e->loadSong(getCurrent().getFile())));
        song.add(new Actionable("Load..", e -> loadSong(FileChooser.choose(Folders.getSetlist()))));
    	song.add(new Actionable("New", e -> setCurrent(new Song())));
        song.add(setlist);
    	song.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));
    	
    	erase.add(new Actionable("all", e->getLooper().clear()));
    	looper.forEach(loop->erase.add(new Actionable(loop.getName(), e->loop.delete())));
    	looper.forEach(loop->duplicate.add(new Actionable(loop.getName(), e->loop.duplicate())));
    	SoloTrack solo = getLooper().getSoloTrack();
    	JMenu solotrack = new JMenu("Solo Track");
    	getNoizeMakers().forEach(ch->solotrack.add(new Actionable(ch.getName(), e->solo.setSoloTrack(ch))));

    	JMenu sync = new JMenu("Sync");
    	sync.add(new Actionable("loopLength", e->setLength()));
    	sync.add(new Actionable("clock2Loop", e->getClock().syncToLoop()));
    	loops.add(sync);
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(new Actionable("Solo 0/1", e->solo.toggle()));
    	loops.add(solotrack);

        for (KnobMode mode : KnobMode.values()) 
        	control.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));
        views.add(new Actionable("Presets", e -> new PresetsGui(getPresets())));
        views.add(new Actionable("Sheets", e->getFrame().sheetMusic(new File(Folders.getSheetMusic(), "Four.png"))));

        add(song);
        add(loops);
        add(control);
        add(views);
    }

	private void setLength() {
		String s = Gui.inputBox("Loop Bars:");
		try {
			getClock().setLength(Integer.parseInt(s));
		} catch (Throwable t) {
			RTLogger.log(this, "Length not set.");
		}
	}
    
}
//    JMenuItem beatBox = new JMenuItem("BeatBox");
//    JMenuItem synths = new JMenuItem("Synths");
//    JMenuItem tracker = new JMenuItem("Tracker");
//    JMenuItem kits = new JMenuItem("Kits");
//        synths.addActionListener(e->getFrame().addOrShow(SynthEngines.getInstance(), SynthEngines.NAME));
//        kits.addActionListener(e->getFrame().addOrShow(KitzView.getInstance(), KitzView.NAME));
//        tracker.addActionListener(e->getFrame().addOrShow(getTracker(), Tracker.NAME));
//        views.add(synths);
//        views.add(kits);
//        views.add(tracker);
//        views.add(beatBox);

