package net.judah.gui;

import static net.judah.JudahZone.getSeq;
import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.FocusEvent.Cause;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Detached.Floating;
import net.judah.seq.Trax;
import net.judah.seq.beatbox.BeatBox;
import net.judah.seq.beatbox.DrumZone;
import net.judah.seq.chords.ChordSheet;
import net.judah.seq.piano.PianoBox;
import net.judah.seq.piano.PianoView;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.Overview;
import net.judah.song.Song;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class TabZone extends JTabbedPane {
	public static TabZone instance;

	@Getter private final HashSet<Component> frames = new HashSet<Component>();

	private final SheetMusicPnl sheetMusic;
	private final Overview overview;
	private final DrumZone drumz;
	private final ChordSheet chords;

	public TabZone(Overview songs, DrumZone drumz, ChordSheet chordz) {
		instance = this;
		SheetMusicPnl exceptional = null;
		try { exceptional = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE); }
		catch (IOException e) { RTLogger.warn(this, e); }
		sheetMusic = exceptional;
		this.drumz = drumz;
		this.chords = chordz;
		this.overview = songs;

		Gui.resize(this, TAB_SIZE);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        addTab(JudahZone.JUDAHZONE, overview);
        addTab(drumz.getName(), drumz);
		addTab(sheetMusic.getName(), sheetMusic);
	}

	public void update(MidiTrack t) {
		if (t.isDrums())
			getDrummer((DrumTrack)t).repaint();
		else {
			PianoBox p = getPianist((PianoTrack)t);
			if (p != null)
				p.repaint();
		}
	}

	public static void edit(MidiTrack track) {
		instance._edit(track);
	}
	private void _edit(MidiTrack track) {
		if (track.isDrums()) {
			drumz.setCurrent(track);
			int idx = tabIndex(drumz);
			if (idx >= 0)
				setSelectedIndex(idx);
			else if (drumz.isShowing())
				drumz.requestFocus();
			else {
				addTab(drumz.getName(), drumz);
				setSelectedComponent(drumz);
			}
			MainFrame.setFocus(track);
		}
		else {
			pianoTrack((PianoTrack)track);
		}
	}

	public void updateSongTitle(Song current) {
		if (current == null || current.getFile() == null)
			return;
		for (int i = 0; i < getTabCount(); i++) // should always be 0
			if (getTabComponentAt(i) instanceof Overview)
				setTitleAt(i, current.getFile().getName());
	}

	public void title(JPanel tab) {
		title(tab, tab.getName());
	}

	public void title(JComponent c, String lbl) {
		if (frames.contains(c)) {
			Window window = SwingUtilities.getWindowAncestor(c);
            if (window instanceof JFrame)
            	((JFrame)window).setTitle(lbl);
		}
		else // tab title
			for (int i = 0; i < getTabCount(); i++)
				if (getComponentAt(i) == c)
					setTitleAt(i, lbl);
	}

	public void changeTab(boolean fwd) {
		MainFrame.focus.execute(() -> instance.setSelectedIndex(
				Constants.rotary(instance.getSelectedIndex(), instance.getTabCount(), fwd)));
	}

	public static BeatBox getDrummer(DrumTrack t) {
		return instance.drumz.getView(t).getGrid();
	}

	public static PianoBox getPianist(PianoTrack t) {
		if (instance.getPiano(t) != null)
			return instance.getPiano(t).getGrid();
		return null;
	}

	public PianoView getPiano(PianoTrack it) {
		for (int i = 0; i < getTabCount(); i++)
			if (getComponentAt(i) instanceof PianoView p)
				if (p.getTrack() == it)
					return p;
		for (Component c : frames)
			if (c instanceof PianoView p)
				if (p.getTrack() == it)
					return p;
		return null;
	}

	// internal
	public int tabIndex(JComponent it) {
		for (int i = 0; i < getTabCount(); i++)
			if (getTabComponentAt(i) == it)
				return i;
		return -1;
	}

	// external
	public PianoView getFrame(PianoTrack t) {
		for (Component c : frames)
			if (c instanceof PianoView v && v.getTrack() == t)
				return v;
		return null;
	}

    public void closeFrame(Component child) {
    	if (frames.contains(child)) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(child);
            if (frame != null)
                frame.dispose(); // Close the JFrame
    	}
    }

    // Detached window listener
	public void closed(Component content) {
		frames.remove(content);
	}

	public void attach(JComponent view) {
		closeFrame(view);
		show(view);
	}

	public void detach(Component c) { // drumzone, chords, pianoview
		if (c == null || c == overview)
			return;
		setSelectedIndex(0);
		remove(c);
		new Detached(c, this);
	}

	public void detach() {
		detach(getSelectedComponent());
	}

	private void focus(JFrame f) {
		f.setExtendedState(JFrame.MAXIMIZED_BOTH);
		f.setVisible(true);
		f.toFront();
		f.requestFocusInWindow(Cause.TRAVERSAL);
	}

	public void sheetMusic(File file, boolean focus) {
    	try {
    		sheetMusic.setImage(file);
			title(sheetMusic, sheetMusic.getName());
    		if (tabIndex(sheetMusic) < 0 && !frames.contains(sheetMusic))
    			addTab(sheetMusic.getName(), sheetMusic);
    		if (focus) {
	    		int idx = tabIndex(sheetMusic);
	    		if (idx >= 0)
	    			setSelectedComponent(sheetMusic);
	    		else
	    			focus(((JFrame) SwingUtilities.getWindowAncestor(sheetMusic)));
    		}
    	} catch (Throwable e) {
    		RTLogger.warn(this, e);
    	}
	}
    public void sheetMusic(Song song) {
    	if (song.getFile() != null) {
    		String name = song.getFile().getName();
    		for (File f : Folders.getSheetMusic().listFiles())
    			if (f.getName().startsWith(name))
    				sheetMusic(f, false);
    	}
	}

    public void show(JComponent o) {
		int idx = tabIndex(o);
		if (idx >= 0)
			setSelectedIndex(idx);
		else if (frames.contains(o)) {
			JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(o);
			focus(frame);
		}
		else {
			if (o instanceof Floating tab)
				tab.resized(TAB_SIZE.width, TAB_SIZE.height);
			addTab(o.getName(), o);
			setSelectedComponent(o);
		}
    }

	public void chordSheet() {
		show(chords);
	}

	public void drumZone() {
		show(drumz);
	}

	/** show PianoTrack, creating if necessary */
	public void pianoTrack(PianoTrack p) {
		PianoView view = getPiano(p);
		if (view == null)
			show(new PianoView(p));
		else {
			if (getFrame(p) == null)
				setSelectedComponent(view);
			view.requestFocus();
		}
	}
	public void pianoTrack(Trax p) {
		for (PianoTrack t : getSeq().getSynthTracks())
			if (t.getType() == p)
				pianoTrack(t);
	}

	@Override
	public void setSelectedIndex(int index) {
		if (getSelectedIndex() != index)
			super.setSelectedIndex(index);
		getSelectedComponent().requestFocusInWindow();
	}
}

