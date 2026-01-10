package net.judah.seq.track;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import judahzone.gui.Actionable;
import net.judah.drumkit.DrumType;
import net.judah.seq.track.Edit.Type;
import net.judah.seq.track.Editor.Delta;

/**
 * Central menu action handler for track editing operations.
 * Provides a single origin ID for all menu-initiated editor actions.
 */
public class MenuActions implements Editor.TrackListener {

    private final Editor editor;
    private final MusicBox grid;
    private final NoteTrack track;

    public MenuActions(Editor editor, MusicBox grid) {
        this.editor = editor;
        this.grid = grid;
        this.track = grid.getTrack();
        // Register as listener to prevent feedback loops
        editor.addListener(this);
    }

    // SelectionListener implementation - no-op to avoid feedback
    @Override
    public void selectionChanged(Editor.Selection selection) {
        // Menu actions don't need to react to selection changes
        if (selection.originId() == this) {
            return; // Ignore our own selections
        }
    }

    // ========== Selection Operations ==========

    public void selectNone() {
        editor.publish(this, new ArrayList<>());
    }

    public void selectBarLeft() {
        editor.publish(this, editor.selectBar(true));
    }

    public void selectBarRight() {
        editor.publish(this, editor.selectBar(false));
    }

    public void selectFrame() {
        editor.publish(this, editor.selectFrame());
    }

    public void selectTrack() {
        editor.publish(this, editor.selectArea(0, track.getT().ticks()));
    }

    public void selectDrumType(DrumType drumType) {
        int data1 = drumType.getData1();
        editor.publish(this, editor.selectArea(0, track.getT().ticks(), data1, data1));
    }

    // ========== Edit Operations ==========

    public void copy() {
        editor.copy();
    }

    public void paste() {
        editor.paste();
    }

    public void delete() {
        editor.delete();
    }

    public void undo() {
        editor.undo();
    }

    public void redo() {
        editor.redo();
    }

    // ========== Tools Operations ==========

    public void trimFrame() {
        long start = track.getFrame() * track.getWindow();
        long end = start + 2 * track.getBarTicks();
        Edit trim = new Edit(Type.TRIM, editor.selectFrame());
        trim.setOrigin(new Prototype(0, start));
        trim.setDestination(new Prototype(0, end));
        editor.push(trim);
    }

    public void trimBar(boolean left) {
        long start = track.getFrame() * track.getWindow();
        if (!left)
            start += track.getBarTicks();
        long end = start + track.getBarTicks();
        Edit trim = new Edit(Type.TRIM, editor.selectBar(left));
        trim.setOrigin(new Prototype(0, start));
        trim.setDestination(new Prototype(0, end));
        editor.push(trim);
    }

    public void insertBar(boolean left) {
        long start = track.getFrame() * track.getWindow();
        if (!left)
            start += track.barTicks;
        long end = start + track.barTicks;
        Edit ins = new Edit(Type.INS, new ArrayList<MidiEvent>());
        ins.setOrigin(new Prototype(0, start));
        ins.setDestination(new Prototype(0, end));
        editor.push(ins);
    }

    public void insertFrame() {
        long start = track.getFrame() * track.getWindow();
        long end = start + track.getWindow();
        Edit ins = new Edit(Type.INS, new ArrayList<MidiEvent>());
        ins.setOrigin(new Prototype(0, start));
        ins.setDestination(new Prototype(0, end));
        editor.push(ins);
    }

    public void showTranspose() {
        new Transpose(track, grid);
    }

    // ========== Menu Building Helpers ==========

    /**
     * Builds the Edit menu with proper action bindings.
     * Should be called on EDT during menu initialization.
     */
    public void buildEditMenu(JMenu editMenu) {
        editMenu.removeAll();

        JMenu select = new JMenu("Select");
        select.add(new Actionable("None", e -> selectNone()));
        select.add(new Actionable("A", e -> selectBarLeft()));
        select.add(new Actionable("B", e -> selectBarRight()));
        select.add(new Actionable("Window", e -> selectFrame()));
        select.add(new Actionable("Track", e -> selectTrack()));

        if (track.isDrums()) {
            JMenu drumTypes = new JMenu("Drum");
            for (DrumType t : DrumType.values()) {
                drumTypes.add(new Actionable(t.name(),
                    e -> selectDrumType(t)));
            }
            select.add(drumTypes);
        }

        editMenu.add(select);
        editMenu.add(new Actionable("Copy", e -> copy()));
        editMenu.add(new Actionable("Paste", e -> paste()));
        editMenu.add(new Actionable("Delete", e -> delete()));
        editMenu.add(new Actionable("Undo", e -> undo()));
        editMenu.add(new Actionable("Redo", e -> redo()));
        editMenu.add(new Actionable("Transpose...", e -> showTranspose()));
    }

    /**
     * Builds the Tools menu with proper action bindings.
     * Should be called on EDT during menu initialization.
     */
    public void buildToolsMenu(JMenu toolsMenu) {
        SwingUtilities.invokeLater(() -> {
            toolsMenu.removeAll();

            JMenu trim = new JMenu("Trim");
            trim.add(new Actionable("Frame", e -> trimFrame()));
            trim.add(new Actionable("Left", e -> trimBar(true)));
            trim.add(new Actionable("Right", e -> trimBar(false)));

            JMenu insert = new JMenu("Insert");
            insert.add(new Actionable("Left", e -> insertBar(true)));
            insert.add(new Actionable("Right", e -> insertBar(false)));
            insert.add(new Actionable("Frame", e -> insertFrame()));

            toolsMenu.add(new Actionable("Track Info...", e -> track.info()));
            toolsMenu.add(trim);
            toolsMenu.add(insert);
            toolsMenu.add(new Actionable("Automation",
                e -> net.judah.gui.MainFrame.setFocus(track.getAutomation())));
        });
    }

	@Override
	public void dataChanged(Delta time) {
		// no - op  // recalc frames?
	}
}