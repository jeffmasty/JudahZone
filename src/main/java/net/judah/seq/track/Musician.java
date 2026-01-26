package net.judah.seq.track;

import java.awt.Point;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.api.Signature;
import judahzone.gui.Gui;
import net.judah.seq.track.Editor.TrackListener;

/** User interaction with a Notes Track */
public interface Musician extends TrackListener, Gui.Mouse {

	NoteTrack getTrack();

	// UI Translation Methods
	void timeSig(Signature value);
	long toTick(Point p);
	int toData1(Point p);
	Prototype translate(Point p);

	// UI Action Handlers
	void dragStart(Point p);
	void drag(Point p);
	void drop(Point p);

	void velocity(boolean up);

	/** @return The editor that manages state and history for this musician's track. */
	default Editor getEditor() {
		return getTrack().getEditor();
	}

	default public MidiEvent findNoteAt(long tick, int data1) {
	    long tolerance = getTrack().getStepTicks() / 4; // quarter-step tolerance
	    for (int i = 0; i < getTrack().getT().size(); i++) {
	        MidiEvent e = getTrack().getT().get(i);
	        if (Math.abs(e.getTick() - tick) > tolerance) continue;

	        if (e.getMessage() instanceof ShortMessage sm) {
	            if (Midi.isNoteOn(sm) && sm.getData1() == data1) {
	                return e;
	            }
	        }
	    }
	    return null;
	}

//	void updateSelection(Selection selection);

//	/**
//	 * Calculates which events fall within a given geometric and time-based area.
//	 * This method does NOT change the current selection.
//	 * @return A list of events within the specified bounds.
//	 */
//	List<MidiEvent> getEventsInArea(long startTick, long endTick, int lowData1, int highData1);
//
//	/**
//	 * Pushes a 'delete' edit for the currently selected notes to the editor.
//	 */
//	void deleteSelection();
//
//	/**
//	 * Copies the currently selected notes to the editor's clipboard.
//	 */
//	void copySelection();
//
//	/**
//	 * Publishes a new selection containing no events.
//	 */
//	void selectNone();


//	@Deprecated
//	List<MidiEvent> selectArea(long start, long end, int low, int high);
//	@Deprecated
//	List<MidiEvent> selectFrame();
//	@Deprecated
//	default List<MidiEvent> getSelection() {
//		return getEditor().getSelection().events();
//	}
//	@Deprecated
//	default void delete() {
//		getEditor().push(new Edit(Type.DEL, new ArrayList<MidiEvent>(getSelection())));
//	}

//	@Deprecated
//	void copy();

}