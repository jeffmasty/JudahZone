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
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.LFO;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.song.Overview;
import net.judah.song.Song;
import net.judah.song.setlist.Setlist;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class JudahMenu extends JMenuBar {
	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};

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
	
	public JudahMenu(int width, Looper looper, Overview overview) {

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
		
    	song.add(new Actionable("Next", e->overview.nextSong()));
		song.add(new Actionable("Save", e->overview.save()));
        song.add(new Actionable("Save As..", e->{
        		File f = FileChooser.choose(getSetlists().getDefault());
        		if (f == null) return;
        		overview.save(f);
        		Constants.timer(200, () -> {
        			SongCombo.refill();
        			SongCombo.refresh();
        		});;
        	}));
    	song.add(new Actionable("Reload", e->overview.reload()));
        song.add(new Actionable("Load..", e -> overview.loadSong(FileChooser.choose(getSetlists().getDefault()))));
    	song.add(new Actionable("New", e -> overview.setSong(new Song(getSeq(), (int)(getClock().getTempo())))));
    	for (Setlist list : getSetlists()) 
			setlist.add(new Actionable(list.toString(), e -> getSetlists().setCurrent(list.getSource())));
        song.add(setlist); 
    	song.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));

    	erase.add(new Actionable("All", e->getLooper().clear()));
    	looper.forEach(loop->erase.add(new Actionable(loop.getName(), e->loop.clear())));
    	looper.forEach(loop->duplicate.add(new Actionable(loop.getName(), e->loop.doubled())));
    	looper.forEach(loop->save.add(new Actionable(loop.getName(), e-> loop.save()))); 
    	looper.forEach(loop->load.add(new Actionable(loop.getName(), e-> Constants.execute(()->loop.load(true)))));

    	SoloTrack solo = getLooper().getSoloTrack();
    	getInstruments().forEach(ch->solotrack.add(new Actionable(ch.getName(), e->solo.setSoloTrack(ch))));

    	loops.add(new Actionable("Length..", e->setLength()));
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(save);
    	loops.add(load);
    	loops.add(new Actionable("Solo on/off", e->solo.toggle()));
    	loops.add(solotrack);
    	
		JMenu clock = new JMenu("Clock");
		clock.add(new Actionable("Toggle", e->getClock().toggle()));
    	clock.add(new Actionable("Reset", e->getClock().reset()));
    	clock.add(new Actionable("Sync2Loop", e->getClock().syncToLoop()));
    	clock.add(new Actionable("SyncTempo", e->getClock().syncTempo(looper.getPrimary())));
    	clock.add(new Actionable("Runners dial zero", e->getClock().runnersDialZero()));
    	
    	
    	JMenu lfo = new JMenu("LFO");
    	JMenu delay = new JMenu("Delay");
    	JMenu chorus = new JMenu("Chorus");
    	
    	for (int i = 0; i < TYPE.length; i++) {
    		final int idx = i;
    		lfo.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, LFO.class)));
    		delay.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, Delay.class)));
    		chorus.add(new Actionable(TYPE[i], e->getFxRack().timeFx(idx, Chorus.class)));
    	}
    	clock.add(lfo);
    	clock.add(delay);
    	clock.add(chorus);
    	
        sheets.setSelected(false);
        sheets.setToolTipText("Focus on Song SheetMusic");
        sheets.setText("Sheets");
    	for (KnobMode mode : KnobMode.values()) 
        	knobs.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));

    	tools.add(new Actionable("Record Session", e -> JudahZone.getMains().tape(true)));
        tools.add(sheets);
        tools.add(new Actionable("SheetMusic..", e->{
        	getFrame().sheetMusic(FileChooser.choose(Folders.getSheetMusic()));
        	getFrame().getTabs().setSelectedComponent(getFrame().getSheetMusic());
        }));
        tools.add(new Actionable("ChordPro..", e-> {
        	if (getChords().load() != null) 
        		getFrame().getTabs().setSelectedComponent(getChords().getChordSheet());
        }));
    	tools.add(new Actionable("JIT!", e->JudahZone.justInTimeCompiler()));
        tools.add(new Actionable("A2J!", e->JudahZone.getMidi().recoverMidi()));
        
        add(song);
        add(loops);
        add(clock);
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
