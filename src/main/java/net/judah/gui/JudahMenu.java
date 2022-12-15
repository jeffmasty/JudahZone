package net.judah.gui;

import static net.judah.JudahZone.*;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import net.judah.controllers.KnobMode;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.mixer.LineIn;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class JudahMenu extends JMenuBar {

	JMenu song = new JMenu("Song");
	JMenuItem next = new JMenuItem("Next");
	JMenuItem reload = new JMenuItem("Reload");
    JMenu loops = new JMenu("Looper");
    JMenuItem reset = new JMenuItem("Reset");
    JMenu erase = new JMenu("Erase");
    JMenu duplicate = new JMenu("Duplicate");
    JMenuItem length = new JMenuItem("Sync Length...");
    JMenuItem syncDrums = new JMenuItem("Sync Clock");
    JMenuItem drumtrack = new JMenuItem ("Solo on/off");
    JMenu solotrack = new JMenu("Solo Track");
    JMenu control = new JMenu("Controls");
    JMenu views = new JMenu("Views");
    JMenuItem presets = new JMenuItem("Presets");
    JMenuItem sheetMusic = new JMenuItem("Sheet Music");
    JMenuItem exit = new JMenuItem("Exit");
    
	public JudahMenu(int width, Looper looper) {
		Dimension d = new Dimension(width - 8, Size.STD_HEIGHT);
		setPreferredSize(d);
		setSize(d);
		setMinimumSize(d);
		
    	// TODO save midi bundle for song
    	next.addActionListener(e->nextSong());
    	reload.addActionListener(e->loadSong());
    	exit.addActionListener(e-> System.exit(0));
        exit.setMnemonic(KeyEvent.VK_E);
    	song.add(next);
    	song.add(reload);
    	song.add(exit);
    	
    	
    	length.addActionListener(e->setLength());
    	syncDrums.addActionListener(e->getClock().syncToLoop());
    	reset.addActionListener(e->getLooper().clear());
    	for (Loop loop : looper) {
    		JMenuItem delete = new JMenuItem(loop.getName());
    		delete.addActionListener(e->loop.delete());
    		erase.add(delete);
    		JMenuItem dup = new JMenuItem(loop.getName());
    		dup.addActionListener(e->loop.duplicate());
    		duplicate.add(dup);
    	}
    	SoloTrack solo = getLooper().getSoloTrack();
    	drumtrack.addActionListener(e->solo.toggle());
    	for (LineIn ch : getNoizeMakers()) {
    		JMenuItem assign = new JMenuItem(ch.getName());
    		assign.addActionListener(e->solo.setSoloTrack(ch));
    		solotrack.add(assign);
    	}
    	
    	loops.add(length);
    	loops.add(syncDrums);
    	loops.add(reset);
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(drumtrack);
    	loops.add(solotrack);
    	
    	
        for (KnobMode mode : KnobMode.values()) {
        	JMenuItem knobs = new JMenuItem(mode.toString());
        	knobs.addActionListener(e->MainFrame.setFocus(mode));
        	control.add(knobs);
        }
        
    	presets.addActionListener( e -> new PresetsGui(getPresets()));
        sheetMusic.addActionListener(e->getFrame().sheetMusic(new File(Folders.getSheetMusic(), "Four.png")));
        views.add(presets);
        views.add(sheetMusic);

        add(song);
        add(loops);
        add(control);
        add(views);
    }

	private void setLength() {
		String s = Constants.inputBox("Loop Bars:");
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

