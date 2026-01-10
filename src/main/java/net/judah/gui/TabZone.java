package net.judah.gui;

import static net.judah.gui.Size.TAB_SIZE;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.FocusEvent.Cause;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import judahzone.gui.Floating;
import judahzone.gui.Gui;
import judahzone.jnajack.BasicPlayer;
import judahzone.scope.JudahScope;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.widgets.CloseableTabbedPane;
import net.judah.seq.beatbox.BeatBox;
import net.judah.seq.beatbox.DrumCage;
import net.judah.seq.beatbox.DrumZone;
import net.judah.seq.chords.ChordSheet;
import net.judah.seq.piano.Piano;
import net.judah.seq.piano.PianoView;
import net.judah.seq.track.Computer;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Musician;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.Overview;
import net.judah.song.Song;

public class TabZone extends CloseableTabbedPane {
	public static TabZone instance;

	@Getter private final HashSet<Component> frames = new HashSet<Component>();

	private final JudahZone zone;
	private final SheetMusicPnl sheetMusic;
	private final Overview overview;
	private final DrumZone drumz;
	private final ChordSheet chords;
	private JudahScope scope;

	public TabZone(JudahZone judahZone, DrumZone drumz) {

		// Overview songs, DrumZone drumz, ChordSheet chordz, JudahScope scope
		this.zone = judahZone;
		this.overview = zone.getOverview();
		this.drumz = drumz;
		this.chords = zone.getChords().getChordSheet();

		instance = this;
		SheetMusicPnl exceptional = null;
		try { exceptional = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE); }
		catch (IOException e) { RTLogger.warn(this, e); }
		sheetMusic = exceptional;

		Gui.resize(this, TAB_SIZE);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        addTab(JudahZone.JUDAHZONE, overview);
        addTab(drumz.getName(), drumz, true);
		addTab(sheetMusic.getName(), sheetMusic, true);
	}

	public static void edit(MidiTrack track) {
		instance._edit(track);
	}
	private void _edit(MidiTrack track) {
		if (track instanceof PianoTrack piano)
			pianoTrack(piano);
		else if (track instanceof DrumTrack drumTrack) {
			if (drumz.getTracks().contains(drumTrack)) {
				drumz.setCurrent(drumTrack);
				int idx = indexOfComponent(drumz);
				if (idx >= 0)
					setSelectedIndex(idx);
				else if (drumz.isShowing())
					drumz.requestFocus();
				else {
					addTab(drumz.getName(), drumz, true);
					setSelectedComponent(drumz);
				}
				MainFrame.setFocus(track);
			}
			else { // ??
				Box box = new Box(BoxLayout.Y_AXIS);
				box.add(new BeatBox(drumTrack, drumz));
				addTab("import",  box, true);
			}
		}
		else // if (track instanceof ChannelTrack)
			RTLogger.log(this, "No Editor for MidiTrack " + track.getName());
		zone.getSeq().getTracks().setCurrent(track);

	}

	public void updateSongTitle(Song current) {
		String name = "";
		if (current != null && current.getFile() != null)
			current.getFile().getName();
		for (int i = 0; i < getTabCount(); i++) // should always be 0
			if (getTabComponentAt(i) instanceof Overview)
				setTitleAt(i, name);
	}

	public void title(JComponent tab) {
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
		Threads.execute(() -> instance.setSelectedIndex(
				Constants.rotary(instance.getSelectedIndex(), instance.getTabCount(), fwd)));
	}

	public static Musician getMusician(MidiTrack track) {
		if (track instanceof PianoTrack piano)
			return getPianist(piano);
		else if (track instanceof DrumTrack drums)
			return getDrummer(drums);
		return null; // ChannelTrack
	}

	public static BeatBox getDrummer(DrumTrack t) {
		return instance.drumz.getView(t).getGrid();
	}

	public static Piano getPianist(PianoTrack t) {
		if (instance.getPiano(t) == null)
			return null;
		return instance.getPiano(t).getGrid();
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

    public void closeFrame(Component child) {
    	if (frames.contains(child)) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(child);
            if (frame != null)
                frame.dispose(); // Close the JFrame
    	}
    }

	private void focus(JFrame f) {
		f.setExtendedState(JFrame.MAXIMIZED_BOTH);
		f.setVisible(true);
		f.toFront();
		f.requestFocusInWindow(Cause.TRAVERSAL);
	}

	public void attach(JComponent view) {
		closeFrame(view);
		show(view);
	}

    // Detached window's listeners
	public void closed(Component content) {
		deactivateScope(content);
		unregisterTrack(content);
		frames.remove(content);
	}

    @Override public void removeTabAt(int index) {
    	Component goner = getComponentAt(index);
    	deactivateScope(goner);
    	unregisterTrack(goner);
    	super.removeTabAt(index);
    }

    private void deactivateScope(Component c) {
    	if (c instanceof JudahScope scope && scope.isActive())
    		scope.setActive(false);
    }

    private void unregisterTrack(Component c) {
    	if (c instanceof PianoView box)
    		box.unregister();
    	else if (c instanceof DrumZone cage)
    		cage.unregister();
	}

	// external window
	public PianoView getFrame(PianoTrack t) {
		for (Component c : frames)
			if (c instanceof PianoView v && v.getTrack() == t)
				return v;
		return null;
	}
	public void sheetMusic(File file, boolean focus) {
		if (file == null)
			return;
    	try {
    		sheetMusic.setImage(file);
    		install(sheetMusic);
    	} catch (Throwable e) {
    		RTLogger.warn(this, e);
    	}
	}

    public void sheetMusic(Song song) {
    	if (song.getFile() != null) {
    		String name = song.getFile().getName();
    		for (File f : Folders.getSheetMusic().listFiles())
    			if (f.getName().startsWith(name))
    				sheetMusic(f, false); // TODO focus sheetMusic toggle on/off
    	}
	}

    public void show(JComponent o) {
		int idx = indexOfComponent(o);
		if (idx >= 0)
			setSelectedIndex(idx);
		else if (frames.contains(o)) {
			JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(o);
			focus(frame);
		}
		else {
			if (o instanceof Floating tab)
				tab.resized(TAB_SIZE.width, TAB_SIZE.height);
			addTab(o.getName(), o, true);
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
		if (view == null) {
			PianoView instance = new PianoView(p, zone.getSeq());
			show(instance);
		}
		else {
			if (getFrame(p) == null)
				setSelectedComponent(view);
			view.requestFocus();
		}
	}

	@Override public void setSelectedIndex(int index) {
		if (getSelectedIndex() != index)
			super.setSelectedIndex(index);
		getSelectedComponent().requestFocusInWindow();
	}

	public void detach(Component c) { // drumzone, chords, pianoview
		if (c == null || c == overview)
			return;
		setSelectedIndex(0);
		remove(c);
		new Detached(c, this);
	}

	@Override public void detach() {
		detach(getSelectedComponent());
	}

	public void update(Update type, Computer c) {
		if (c instanceof NoteTrack == false)
			return;
		NoteTrack notes = (NoteTrack)c;
		if (type == Update.REZ) {
			if (notes.isDrums()) {
				DrumCage view = drumz.getView(notes);
				if (view != null)
					view.getGrid().repaint();
			}
			else if (notes.isSynth()) {
				PianoView view = getPiano((PianoTrack)notes);
				if (view != null)
					view.refresh();
			}
		} else if (notes.isDrums()) {
			DrumCage view = drumz.getView(notes);
			if (view != null)
				view.getMenu().update(type);
		} else if (notes.isSynth()) {
			PianoView view = getPiano((PianoTrack)notes);
			if (view != null) {
				view.getMenu().update(type);
			}
		} // else ChannelTrack

//		if (type == Update.EDIT)
//			update(notes);
		if (type == Update.CURRENT)
			update(notes);
		if (type == Update.FILE)
			update(notes);
	}

	public void update(MidiTrack t) {
		if (t.isDrums())
			getDrummer((DrumTrack)t).repaint();
		else if (t.isSynth()) {
			PianoView view = getPiano((PianoTrack)t);
			if (view != null) {
				view.getGrid().repaint();
				view.getSteps().repaint();
			}
		} // else ChannelTrack
	}

	public void install(JComponent c) {
		title(c, c.getName());
		if (indexOfComponent(c) < 0 && !frames.contains(c))
			addTab(c.getName(), c, true);
		show(c);
	}

	public void scope() {
		if (scope == null) {// lazyload
			BasicPlayer playa = zone.getSampler().add(new BasicPlayer());
			scope = new JudahScope(Size.WIDTH_TAB, playa, zone);
		}
		install(scope);
	}

}

