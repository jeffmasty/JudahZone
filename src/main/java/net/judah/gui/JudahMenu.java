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
import net.judah.JudahZone;
import net.judah.gui.fx.PresetsGui;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.settable.Songs;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.song.Song;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class JudahMenu extends JMenuBar {
	
	/** If true, focus on sheet music tab when songs load */ 
	@Getter private final JCheckBoxMenuItem sheets = new JCheckBoxMenuItem();
	
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
		
        	
		song.add(new Actionable("Next", e->nextSong()));
        song.add(new Actionable("Save", e->save()));
        song.add(new Actionable("Save As..", e->{
        		File f = FileChooser.choose(Folders.getSetlist());
        		if (f == null) return;
        		save(f);
        		Constants.timer(200, () -> {
        			Songs.refill();
        			Songs.refresh();
        		});;
        	}));
    	song.add(new Actionable("Reload", e->reload()));
        song.add(new Actionable("Load..", e -> loadSong(FileChooser.choose(Folders.getSetlist()))));
    	song.add(new Actionable("New", e -> setCurrent(new Song(getSeq(), (int)(getClock().getTempo())))));
    	
    	for (File f : Folders.getSetlists()) 
			setlist.add(new Actionable(f.getName(), e -> Folders.setSetlist(f)));
        // song.add(setlist); // TODO
    	
    	
    	erase.add(new Actionable("All", e->getLooper().clear()));
    	looper.forEach(loop->erase.add(new Actionable(loop.getName(), e->loop.erase())));
    	looper.forEach(loop->duplicate.add(new Actionable(loop.getName(), e->loop.duplicate())));
    	SoloTrack solo = getLooper().getSoloTrack();
    	JMenu solotrack = new JMenu("Solo Track");
    	getInstruments().forEach(ch->solotrack.add(new Actionable(ch.getName(), e->solo.setSoloTrack(ch))));

    	JMenu sync = new JMenu("Sync");
    	sync.add(new Actionable("Clock", e->getClock().syncToLoop()));
    	sync.add(new Actionable("Loop Length", e->setLength()));
    	loops.add(sync);
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(new Actionable("Solo on/off", e->solo.toggle()));
    	loops.add(solotrack);
    	loops.add(new Actionable("A2J!", e->JudahZone.getMidi().recoverMidi()));
    	loops.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));

        sheets.setSelected(false);
        sheets.setToolTipText("Focus on Song SheetMusic");
        sheets.setText("Sheets");
    	for (KnobMode mode : KnobMode.values()) 
        	control.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));
    	control.add(new Actionable("Set", e->SetCombo.set()));

        views.add(new Actionable("Presets", e -> new PresetsGui(getPresets())));
        views.add(sheets);

        views.add(new Actionable("SheetMusic..", e->{
        	getFrame().sheetMusic(FileChooser.choose(Folders.getSheetMusic()));
        	getFrame().getTabs().setSelectedComponent(getFrame().getSheetMusic());
        }));
        
        views.add(new Actionable("ChordPro..", e-> getChords().load(
        		FileChooser.choose(Folders.getChordPro()), getCurrent())));
        
        add(loops);
        add(song);
        add(control);
        add(views);
    }

	private void setLength() {
		String s = Gui.inputBox("Loop Bar Length:");
		try {
			getClock().setLength(Integer.parseInt(s));
		} catch (Throwable t) {
			RTLogger.log(this, "Length not changed.");
		}
	}
    
}
