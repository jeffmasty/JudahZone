package net.judah.gui;

import java.awt.Dimension;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import net.judah.JudahZone;
import net.judah.fx.Chorus;
import net.judah.fx.Delay;
import net.judah.fx.LFO;
import net.judah.gui.knobs.KnobMode;
import net.judah.looper.Looper;
import net.judah.looper.SoloTrack;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.seq.AddTrack;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlist;
import net.judah.song.setlist.Setlists;
import net.judahzone.gui.Actionable;
import net.judahzone.gui.Gui;

public class JudahMenu extends JMenuBar {
	static String[] TYPE = {"1/8", "1/4", "3/8", "1/2"};

	private final JudahZone zone;
	private final JMenu synth = new JMenu("Synth");
	private final JudahClock clock;

	@SuppressWarnings("static-access")
	public JudahMenu(int width, JudahZone judahZone, TabZone tabs, JudahMidi midi) {
		this.zone = judahZone;
		this.clock = midi.getClock();

		Overview overview = zone.getOverview();
		Seq seq = zone.getSeq();

		JMenu song = new JMenu("Song");
	    JMenu loops = new JMenu("Looper");
	    JMenu knobs = new JMenu("Knobs");

		Dimension d = new Dimension(width - 8, Size.STD_HEIGHT);
		setPreferredSize(d);
		setSize(d);
		setMinimumSize(d);


    	song.add(new Actionable("Next", e->overview.nextSong()));
		song.add(new Actionable("Save", e->overview.save()));
        song.add(new Actionable("Save As..", e->overview.saveAs()));

        Setlists setlists = zone.getSetlists();

        song.add(new Actionable("Reload", e->overview.reload()));
        song.add(new Actionable("Load..", e->overview.loadSong(Folders.choose(setlists.getDefault()))));
    	song.add(new Actionable("New Song", e->overview.newSong()));
    	song.add(new Actionable("New Track..", e-> new AddTrack(seq)));
    	song.add(new Actionable("Bundle", e -> overview.bundle()));
    	// TODO song.add(new Actionable("Resolution..", e->seq.resolutionView()));

		JMenu setlist = new JMenu("Setlist");
		for (Setlist list : setlists)
			setlist.add(new Actionable(list.toString(), e -> setlists.setCurrent(list.getSource())));
        song.add(setlist);
    	song.add(new Actionable("Exit", e->System.exit(0), KeyEvent.VK_E));

    	Looper looper = zone.getLooper();
	    JMenu erase = new JMenu("Erase");
	    JMenu duplicate = new JMenu("Duplicate");
	    JMenu save = new JMenu("Save");
	    JMenu load = new JMenu("Load");
	    JMenu solotrack = new JMenu("Solo Track");
    	erase.add(new Actionable("All", e->looper.delete()));
    	looper.forEach(loop->erase.add(new Actionable(loop.getName(), e->looper.delete(loop))));
    	looper.forEach(loop->duplicate.add(new Actionable(loop.getName(), e->loop.duplicate())));
    	looper.forEach(loop->save.add(new Actionable(loop.getName(), e-> loop.save())));
    	looper.forEach(loop->load.add(new Actionable(loop.getName(), e-> loop.load(true))));
    	SoloTrack solo = looper.getSoloTrack();
    	zone.getInstruments().forEach(ch->solotrack.add(new Actionable(ch.getName(), e->solo.setSoloTrack(ch))));
    	loops.add(new Actionable("Length..", e->setLength()));
    	loops.add(erase);
    	loops.add(duplicate);
    	loops.add(save);
    	loops.add(load);
    	loops.add(new Actionable("Solo on/off", e->solo.toggle()));
    	loops.add(solotrack);
    	loops.add(new Actionable("Info..", e->Gui.infoBox(looper.toString(), "Info")));

    	JMenu time = new JMenu("Clock");
		time.add(new Actionable("Tempo", e->clock.inputTempo()));
		time.add(new Actionable("Start/Stop", e->clock.toggle()));
    	time.add(new Actionable("Reset", e->clock.reset()));
    	time.add(new Actionable("Send/Rcv", e->clock.primary()));
    	time.add(new Actionable("Sync2Loop", e->clock.syncToLoop(looper.getPrimary())));
    	time.add(new Actionable("Swing...", e->swing()));
    	//time.add(new Actionable("Runners dial zero", e->clock.runnersDialZero()));

    	JMenu lfo = new JMenu("LFO");
    	JMenu delay = new JMenu("Delay");
    	JMenu chorus = new JMenu("Chorus");

    	for (int i = 0; i < TYPE.length; i++) {
    		final int idx = i;
    		lfo.add(new Actionable(TYPE[i], e->zone.getFxRack().timeFx(idx, LFO.class)));
    		delay.add(new Actionable(TYPE[i], e->zone.getFxRack().timeFx(idx, Delay.class)));
    		chorus.add(new Actionable(TYPE[i], e->zone.getFxRack().timeFx(idx, Chorus.class)));
    	}
    	time.add(lfo);
    	time.add(delay);
    	time.add(chorus);
        time.add(new Actionable("A2J!", e->midi.recoverMidi()));

    	for (KnobMode mode : KnobMode.values())
        	knobs.add(new Actionable(mode.toString(), e->MainFrame.setFocus(mode)));

        add(song);
        add(loops);
        add(time);
        add(knobs);
        add(viewMenu(tabs));

	}

	public void refillTracks() {
		synth.removeAll();
		for (PianoTrack t : SynthRack.getSynthTracks())
			synth.add(new Actionable(t.getName(), e->TabZone.edit(t)));
	}

	private JMenu viewMenu(TabZone tabs) {
		JMenu views = new JMenu("View");
        views.add(new Actionable("BeatBox", e->tabs.drumZone()));
        views.add(synth);
        refillTracks();
        views.add(new Actionable("ChordPro..", e-> {
        	if (zone.getChords().isEmpty()) {
	        	if (zone.getChords().load() != null)
	        		tabs.chordSheet();
        	} else tabs.chordSheet();
        }));
        views.add(new Actionable("SheetMusic..", e->
        	tabs.sheetMusic(Folders.choose(Folders.getSheetMusic()), true)));
        views.add(new Actionable("Scope", e->tabs.scope()));
        views.add(new Actionable("Detach", e->TabZone.instance.detach()));

        return views;
	}

	private void setLength() {
		String s = Gui.inputBox("Loop Bar Length:");
		if (s == null || s.isBlank()) return;
		try {
			clock.setLength(Integer.parseInt(s));
		} catch (Throwable t) {
			RTLogger.log(this, "Length not changed.");
		}
	}

	private void swing() {

    	String change = JOptionPane.showInputDialog("Swing", (int)(clock.getSwing() * 100));
    	if (change == null)
    		return;
    	try {
    		int swing = Integer.parseInt(change);
    		clock.setSwing(swing * 0.01f);
    	} catch (NumberFormatException ne) {
    		RTLogger.log(ne, change + ": " + ne);
    	}

	}

}
