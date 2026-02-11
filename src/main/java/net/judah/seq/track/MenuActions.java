package net.judah.seq.track;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.swing.JMenu;

import judahzone.gui.Actionable;
import net.judah.drums.DrumType;
import net.judah.gui.MainFrame;
import net.judah.seq.track.Edit.Type;

/**
 * Central menu action handler for track editing operations.
 * Provides a single origin ID for all menu-initiated editor actions.
 */
public class MenuActions {

    private final Editor editor;
    private final MusicBox grid;
    private final NoteTrack track;

    public MenuActions(Editor editor, MusicBox grid) {
        this.editor = editor;
        this.grid = grid;
        this.track = grid.getTrack();
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
    public void buildEditMenu(JMenu menu) {
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

        menu.add(select);
        menu.add(new Actionable("Copy", e -> copy()));
        menu.add(new Actionable("Paste", e -> paste()));
        menu.add(new Actionable("Delete", e -> delete()));
        menu.add(new Actionable("Undo", e -> undo()));
        menu.add(new Actionable("Redo", e -> redo()));
        menu.add(new Actionable("Automation",
                e -> MainFrame.setFocus(track.getAutomation())));
	}

    /**
     * Builds the Tools menu with proper action bindings.
     * Should be called on EDT during menu initialization.
     */
    public void buildToolsMenu(JMenu menu) {
        JMenu trim = new JMenu("Trim");
        trim.add(new Actionable("Frame", e -> trimFrame()));
        trim.add(new Actionable("Left", e -> trimBar(true)));
        trim.add(new Actionable("Right", e -> trimBar(false)));

        JMenu insert = new JMenu("Insert");
        insert.add(new Actionable("Left", e -> insertBar(true)));
        insert.add(new Actionable("Right", e -> insertBar(false)));
        insert.add(new Actionable("Frame", e -> insertFrame()));

        menu.add(new Actionable("Track Info...", e -> track.info()));
        menu.add(trim);
        menu.add(insert);
        menu.add(new Actionable("Transpose...", e -> showTranspose()));
    }

}