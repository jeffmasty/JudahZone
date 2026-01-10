package net.judah.seq.track;

import static judahzone.api.MidiConstants.NOTE_ON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.SwingUtilities;

import judahzone.api.Key;
import judahzone.api.Midi;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.drumkit.DrumType;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.Panic;
import net.judah.seq.beatbox.RemapView;
import net.judah.seq.track.Edit.Type;

public class Editor {

	/** unified clipboard */
	public static final Clipboard clipboard = new Clipboard();

	///// Selection Interface /////
	/**Represents a selection of MIDI events within a track's editor.
	 * @param events A list of selected MIDI events
	 * @param originId An identifier for the component that originated this selection, used to prevent event feedback loops*/
	public record Selection(List<MidiEvent> events, Object originId) { }
	//	Automation.java: public static enum MidiMode { CC, Pitch, Program, Meta, All, NoteOn, NoteOff }; appropriate for Data Delta??
	public record Delta(long start, long end) { } // not implemented

	/** Listener interface for receiving notifications about selection changes. */
	public interface TrackListener {
	    void selectionChanged(Selection selection);
	    void dataChanged(Delta time);
	}

	private final List<TrackListener> listeners = new CopyOnWriteArrayList<>();

	public void addListener(TrackListener listener) {
	    listeners.add(listener);
	}

	public void removeListener(TrackListener listener) {
	    listeners.remove(listener);
	}

	/**Sets the current selection and notifies listeners.
	 * If the new selection is the same as the current one, no action is taken.
	 * Notifications are dispatched on the Swing Event Dispatch Thread.
	 * @param selection the new selection */
	public void setSelection(Selection selection) {
	    // Avoid redundant notifications
	    if (selection != null && selection.equals(this.selection)) {
	        return;
	    }
	    this.selection = selection;
	    // Notify listeners on the EDT
	    SwingUtilities.invokeLater(() -> {
	        for (TrackListener listener : listeners) {
	            listener.selectionChanged(selection);
	        }
	    });
	}

	/**Publishes a selection change to all registered listeners.
	 * @param origin The source of the selection (null allowed for backward compatibility)
	 * @param events The selected events */
	public void publish(Object origin, List<MidiEvent> events) {
	    Selection selection = new Selection(events, origin != null ? origin : this);
	    this.selection = selection;

	    for (TrackListener listener : listeners) {
	        listener.selectionChanged(selection);
	    }
	}

	/**Publishes a selection based on an Edit operation, resolving actual track events.
	 * Uses transfer buffer if available (for new events), otherwise looks up events by tick/data1.
	 * @param e The edit operation */
	private void publishSelectionForEdit(Edit e) {
	    ArrayList<MidiEvent> ons = new ArrayList<>();

	    // If recent edit added events, they are in transfer and are actual track events
	    if (!transfer.isEmpty()) {
	        for (MidiEvent p : transfer) {
	            if (Midi.isNoteOn(p.getMessage()))
	                ons.add(p);
	        }
	    } else {
	        // Fallback: resolve edit's stored events to the corresponding track events
	        for (MidiEvent ev : e.getNotes()) {
	            if (!(ev.getMessage() instanceof ShortMessage sm)) continue;
	            if (!Midi.isNoteOn(sm)) continue;
	            int data1 = sm.getData1();
	            long tick = ev.getTick();

	            // For TRANS edits, the tick has changed - compute the new tick
	            if (e.getType() == Type.TRANS && e.getDestination() != null) {
	                tick = computeTransposedTick(tick, e.getDestination());
	            }

	            MidiEvent on = MidiTools.lookup(NOTE_ON, data1, tick, t);
	            if (on != null) {
	                ons.add(on);
	                continue;
	            }

	            // Fallback with quantization
	            if (track instanceof NoteTrack notes) {
	                long qtick = notes.quantize(tick);
	                on = MidiTools.lookup(NOTE_ON, data1, qtick, t);
	                if (on != null)
	                    ons.add(on);
	            }
	        }
	    }

	    if (ons.isEmpty() && e.getType() != Type.DEL) {
	        publish(this, Collections.emptyList());
	        return;
	    }
	    publish(this, ons);
	}

	private void notifyDataChanged() {
	    SwingUtilities.invokeLater(() -> {
	        for (TrackListener listener : listeners) {
	            try { listener.dataChanged(null); }
	            catch (Exception ex) { RTLogger.warn(this, ex); }
	        }
	    });
	}

	///// Editor Core /////
	@Getter private final MidiTrack track;
	private final Track t;

	@Getter private volatile Selection selection;
	protected final ArrayList<MidiEvent> transfer = new ArrayList<>();

	/** undo/redo stack */
	private ArrayList<Edit> stack = new ArrayList<>();
	private int caret;

	public Editor(MidiTrack midi) {
	    track = midi;
	    t = track.getT();
	}

	/**Executes an edit operation on the track.
	 * @param e The edit to execute */
	protected void execute(Edit e) {
	    Type type = e.getType();
	    switch (type) {
	        case DEL -> editDel(e.getNotes());
	        case NEW -> editAdd(e.getNotes());
	        case MOD -> mod(e, true);
	        case LENGTH -> length(e, false);
	        case TRIM -> trim(e, true);
	        case INS -> insert(e, true);
	        case TRANS -> {
	            if (track instanceof NoteTrack notes)
	                transpose(notes, e);
	        }
	        case REMAP -> remap(e, false);
	    }
	    RTLogger.debug(this, "exe: " + e.getType() + " to " + e.getDestination() +
	        ": " + Arrays.toString(e.getNotes().toArray()));
	    // Notify listeners after track mutates
	    // TODO build start/end ticks for Delta.
	    notifyDataChanged(); // <-- More data
	    publishSelectionForEdit(e); // re-select our edits
	}

	public void undo() {
	    if (stack.size() <= caret || caret < 0) {
	        RTLogger.debug(this, "undo empty");
	        return;
	    }
	    Edit e = stack.get(caret);
	    Type type = e.getType();

	    switch (type) {
	        case DEL -> editAdd(e.getNotes());
	        case NEW -> editDel(e.getNotes());
	        case MOD -> mod(e, false);
	        case LENGTH -> length(e, true);
	        case TRIM -> trim(e, false);
	        case INS -> insert(e, false);
	        case TRANS -> {
	            if (track instanceof PianoTrack piano) {
	                if (track.isActive())
	                    new Panic(piano);
	                decomposePiano(e);
	            } else if (track instanceof DrumTrack) {
	                decomposeDrums(e);
	            }
	        }
	        case REMAP -> remap(e, false);
	    }

	    caret--;
	    RTLogger.debug(this, "undo: " + e.getType());
	    notifyDataChanged(); // <-- More data
	    publishSelectionForEdit(e); // re-select our edits
	}

	/**Executes edit and adds to undo stack.
	 * @param e The edit to push */
	public void push(Edit e) {
	    stack.add(e);
	    caret = stack.size() - 1;
	    execute(e);
	    publishSelectionForEdit(e);
	    // Notify listeners after push (execute already notifies; extra notify is safe)
	    notifyDataChanged();
	}

	public void redo() {
	    if (stack.size() <= caret + 1)
	        return;
	    caret++;
	    Edit e = stack.get(caret);
	    execute(e);
	    publishSelectionForEdit(e);
	    // Notify listeners after redo
	    notifyDataChanged();
	}

	public Edit peek() {
	    if (caret >= stack.size())
	        return null;
	    return stack.get(caret);
	}

	/**Adds events to track and populates transfer buffer with note events.
	 * @param replace Events to add */
	protected void editAdd(ArrayList<MidiEvent> replace) {
	    transfer.clear();
	    for (MidiEvent p : replace) {
	        if (Midi.isNote(p.getMessage()))
	            transfer.add(p);
	        t.add(p);
	    }
	}

	/** Removes events from track.
	 * @param list Events to delete */
	void editDel(ArrayList<MidiEvent> list) {
	    for (MidiEvent p: list)
	        MidiTools.delete(p, t);
	}

	/**Transposes drum events to a new pad and/or tick position.
	 * @param notes Events to transpose
	 * @param destination Target pad and tick offset */
	public void transposeDrums(ArrayList<MidiEvent> notes, Prototype destination) {
	    editDel(notes);
	    ArrayList<MidiEvent> replace = new ArrayList<>();
	    int delta = DrumType.index(destination.data1) -
	        DrumType.index(((ShortMessage)notes.getFirst().getMessage()).getData1());
	    long start = track.getCurrent() * track.getBarTicks();

	    for (MidiEvent note : notes)
	        replace.add(compute(note, delta, destination.tick, start, track.getWindow()));

	    transfer.clear();
	    editAdd(replace);
	}

	/**Transposes piano notes to a new pitch and/or tick position.
	 * @param raw Note-on/note-off pairs to transpose
	 * @param destination Target pitch offset and tick offset */
	public void transposePiano(ArrayList<MidiEvent> raw, Prototype destination) {
	    if (track.isActive())
	        new Panic((PianoTrack)track);
	    editDel(raw);
	    ArrayList<MidiEvent> replace = new ArrayList<>();
	    MidiEvent on = null;

	    for (MidiEvent note : raw) {
	        if (Midi.isNoteOn(note.getMessage())) {
	            on = note;
	        } else {
	            MidiNote result = MidiTools.compute(new MidiNote(on, note), destination, track);
	            replace.add(result);
	            replace.add(result.getOff());
	        }
	    }
	    transfer.clear();
	    editAdd(replace);
	}

	private void remap(Edit e, boolean exe) {
	    if (track instanceof DrumTrack)
	        remapDrums(e, exe);
	    else if (track instanceof PianoTrack)
	        remapPiano(e, exe);
	}

	/**Changes note data1 values for drum events.
	 * @param e The edit containing notes to remap
	 * @param exe true for execute, false for undo */
	protected void remapDrums(Edit e, boolean exe) {
	    int target = exe ? e.getDestination().data1 : e.getOrigin().data1;
	    try {
	        for (MidiEvent p : e.getNotes()) {
	            if (p.getMessage() instanceof ShortMessage midi)
	                midi.setMessage(midi.getCommand(), midi.getChannel(), target, midi.getData2());
	        }
	    } catch (Exception ex) {
	        RTLogger.warn(this, ex);
	    }
	    if (MainFrame.getKnobMode() == KnobMode.Remap)
	        MainFrame.setFocus(new RemapView((DrumTrack)track));
	}

	/**Transposes all notes in the track by a semitone offset.
	 * @param e The edit containing the transposition amount
	 * @param exe true for execute, false for undo */
	protected void remapPiano(Edit e, boolean exe) {
	    int diff = exe ? e.getDestination().data1 : -e.getDestination().data1;
	    try {
	        for (int i = 0; i < t.size(); i++) {
	            if (t.get(i).getMessage() instanceof ShortMessage m) {
	                int data1 = m.getData1() + diff;

	                // Wrap to MIDI range
	                while (data1 < 0) data1 += Key.OCTAVE;
	                while (data1 > 127) data1 -= Key.OCTAVE;

	                if (data1 < 0 || data1 > 127) {
	                    RTLogger.warn(this, "MIDI range error: " + data1);
	                    continue;
	                }
	                m.setMessage(m.getCommand(), m.getChannel(), data1, m.getData2());
	            }
	        }
	    } catch(InvalidMidiDataException ex) {
	        RTLogger.warn(this, ex);
	    }
	}

	/** Pastes clipboard contents to track. */
	public void paste() {
	    push(new Edit(Type.NEW, clipboard.paste(track)));
	}

	/**Modifies a CC or other event value.
	 * @param e Edit containing original and modified events
	 * @param exe true for execute, false for undo */
	protected final void mod(Edit e, boolean exe) {
	    MidiEvent alpha = e.getNotes().getFirst();
	    MidiEvent omega = e.getNotes().getLast();

	    if (exe) {
	        MidiTools.delete(alpha, t);
	        t.add(omega);
	    } else {
	        MidiTools.delete(omega, t);
	        t.add(alpha);
	    }
	}

	/**Adjusts note-off timing (piano tracks only).
	 * @param ed Edit containing notes and length adjustment
	 * @param undo true for undo, false for execute */
	protected void length(Edit ed, boolean undo) {
	    ArrayList<MidiEvent> replace = new ArrayList<>();
	    long ticks = ed.getDestination().tick;

	    for (MidiEvent e : ed.getNotes()) {
	        replace.add(new MidiEvent(e.getMessage(), e.getTick() + ticks));
	    }

	    if (undo) {
	        editDel(replace);
	        editAdd(ed.getNotes());
	    } else {
	        editDel(ed.getNotes());
	        editAdd(replace);
	    }
	}

	/**Removes a time range and shifts remaining events.
	 * @param e Edit containing time range to trim
	 * @param exe true for execute, false for undo */
	protected void trim(Edit e, boolean exe) {
	    long start = e.getOrigin().tick;
	    long end = e.getDestination().tick;
	    long diff = end - start;

	    if (exe) {
	        for (MidiEvent p : e.getNotes())
	            t.remove(p);
	        MidiTools.removeTape(t, end, diff);
	    } else {
	        MidiTools.addTape(t, start, diff);
	        for (MidiEvent p : e.getNotes())
	            t.add(p);
	    }
	}

	/** Inserts blank time by shifting events.
	 * @param e Edit containing time range to insert
	 * @param exe true for execute, false for undo */
	protected void insert(Edit e, boolean exe) {
	    long start = e.getOrigin().tick;
	    long end = e.getDestination().tick;
	    long diff = end - start;

	    if (exe)
	        MidiTools.addTape(t, start, diff);
	    else
	        MidiTools.removeTape(t, end, diff);
	}

	public void trimFrame() {
	    long start = track.getFrame() * track.getWindow();
	    long end = start + 2 * track.barTicks;
	    Edit trim = new Edit(Type.TRIM, selectFrame());
	    trim.setOrigin(new Prototype(0, start));
	    trim.setDestination(new Prototype(0, end));
	    push(trim);
	}

	public void trimBar(boolean left) {
	    long start = left ? track.getLeft() : track.getRight();
	    if (!left)
	        start += track.barTicks;
	    long end = start + track.barTicks;
	    Edit trim = new Edit(Type.TRIM, selectBar(left));
	    trim.setOrigin(new Prototype(0, start));
	    trim.setDestination(new Prototype(0, end));
	    push(trim);
	}

	/** Clears the current selection. */
	public void selectNone() {
	    Musician m = TabZone.getMusician(track);
	    if (m != null) {
	        publish(m, Collections.emptyList());
	    } else {
	        setSelection(new Selection(Collections.emptyList(), this));
	    }
	}

	public ArrayList<MidiEvent> selectBar(boolean left) {
	    long start = left ? track.getLeft() : track.getRight();
	    long end = start + track.getBarTicks();
	    return selectArea(start, end);
	}

	public ArrayList<MidiEvent> selectFrame() {
	    long start = track.getLeft();
	    long end = start + track.getWindow();
	    return selectArea(start, end);
	}

	/**
	 * Returns all MidiEvents in [startTick, endTick) whose data1 is within [lowData1, highData1].
	 * This is a convenience used by views that need a filtered query (does not pair note-offs).
	 * @param startTick Start of time range (inclusive)
	 * @param endTick End of time range (exclusive)
	 * @param lowData1 Minimum data1 value (inclusive)
	 * @param highData1 Maximum data1 value (inclusive)
	 * @return List of matching events
	 */
	public List<MidiEvent> getEventsInArea(long startTick, long endTick, int lowData1, int highData1) {
	    ArrayList<MidiEvent> eventsInArea = new ArrayList<>();
	    for (int i = 0; i < t.size(); i++) {
	        MidiEvent e = t.get(i);
	        long tick = e.getTick();
	        if (tick < startTick) continue;
	        if (tick >= endTick) break;
	        if (e.getMessage() instanceof ShortMessage sm) {
	            int data1 = sm.getData1();
	            if (data1 >= lowData1 && data1 <= highData1) {
	                eventsInArea.add(e);
	            }
	        }
	    }
	    return eventsInArea;
	}

	/**
	 * Selects events within a time range filtered by data1 range.
	 * For drums: returns matching events
	 * For piano: returns note-on plus its paired note-off
	 * Populates and returns the transfer buffer.
	 * @param start Start tick (inclusive)
	 * @param end End tick (exclusive)
	 * @param low Minimum data1 value
	 * @param high Maximum data1 value
	 * @return Selected events in transfer buffer
	 */
	public ArrayList<MidiEvent> selectArea(long start, long end, int low, int high) {
	    transfer.clear();

	    if (track.isDrums()) {
	        for (int i = MidiTools.find(t, start); i < t.size() && i >= 0; i++) {
	            MidiEvent e = t.get(i);
	            if (e.getTick() < start) continue;
	            if (e.getTick() >= end) break;
	            if (e.getMessage() instanceof ShortMessage sm) {
	                int data1 = sm.getData1();
	                if (data1 >= low && data1 <= high)
	                    transfer.add(e);
	            }
	        }
	    } else {
	        for (int i = 0; i < t.size(); i++) {
	            long tick = t.get(i).getTick();
	            if (tick < start) continue;
	            if (tick >= end) break;
	            MidiEvent maybeOn = t.get(i);
	            if (Midi.isNoteOn(maybeOn.getMessage())) {
	                ShortMessage sm = (ShortMessage) maybeOn.getMessage();
	                int data1 = sm.getData1();
	                if (data1 >= low && data1 <= high) {
	                    transfer.add(maybeOn);
	                    transfer.add(MidiTools.noteOff(maybeOn, t).getOff());
	                }
	            }
	        }
	    }
	    return transfer;
	}

	/**
	 * Selects all events within a time range.
	 * For drums: returns all events
	 * For piano: returns note-on/note-off pairs
	 * Populates and returns the transfer buffer.
	 * @param start Start tick (inclusive)
	 * @param end End tick (exclusive)
	 * @return Selected events in transfer buffer
	 */
	public ArrayList<MidiEvent> selectArea(long start, long end) {
	    transfer.clear();
	    long tick;

	    if (track.isDrums()) {
	        for (int i = MidiTools.find(t, start); i < t.size() && i >= 0; i++) {
	            MidiEvent e = t.get(i);
	            if (e.getTick() < start) continue;
	            if (e.getTick() >= end) break;
	            transfer.add(e);
	        }
	    } else {
	        for (int i = 0; i < t.size(); i++) {
	            tick = t.get(i).getTick();
	            if (tick < start) continue;
	            if (tick >= end) break;
	            if (Midi.isNoteOn(t.get(i).getMessage())) {
	                transfer.add(t.get(i));
	                transfer.add(MidiTools.noteOff(t.get(i), t).getOff());
	            }
	        }
	    }
	    return transfer;
	}

	public void insertBar(boolean left) {
	    long start = track.getFrame() * track.getWindow();
	    if (!left)
	        start += track.barTicks;
	    long end = start + track.barTicks;
	    Edit ins = new Edit(Type.INS, new ArrayList<MidiEvent>());
	    ins.setOrigin(new Prototype(0, start));
	    ins.setDestination(new Prototype(0, end));
	    push(ins);
	}

	public void insertFrame() {
	    long start = track.getFrame() * track.getWindow();
	    long end = start + track.getWindow();
	    Edit ins = new Edit(Type.INS, new ArrayList<MidiEvent>());
	    ins.setOrigin(new Prototype(0, start));
	    ins.setDestination(new Prototype(0, end));
	    push(ins);
	}

	public void transpose(NoteTrack notes, Edit e) {
	    if (notes instanceof DrumTrack)
	        transposeDrums(e.getNotes(), e.getDestination());
	    else if (notes instanceof PianoTrack)
	        transposePiano(e.getNotes(), e.getDestination());
	}

	/**
	 * Undoes a piano transposition by restoring original events.
	 * @param e Edit containing transposed events to decompose
	 */
	public void decomposePiano(Edit e) {
	    ArrayList<MidiEvent> delete = new ArrayList<>();
	    MidiEvent on = null;

	    for (MidiEvent note : e.getNotes()) {
	        if (Midi.isNoteOn(note.getMessage())) {
	            on = note;
	        } else {
	            MidiNote result = MidiTools.compute(new MidiNote(on, note), e.getDestination(), track);
	            delete.add(result);
	            delete.add(result.getOff());
	        }
	    }
	    editDel(delete);
	    editAdd(e.getNotes());
	}

	/**
	 * Computes transposed drum event position.
	 * @param in Source event
	 * @param delta Data1 offset
	 * @param protoTick Tick offset in steps
	 * @param start Window start tick
	 * @param window Window size in ticks
	 * @return New MidiEvent at computed position
	 */
	public MidiEvent compute(MidiEvent in, int delta, long protoTick, long start, long window) {
	    ShortMessage source = (ShortMessage)in.getMessage();
	    long tick = in.getTick() + protoTick * track.getStepTicks();

	    if (tick < start) tick += window;
	    if (tick >= start + window) tick -= window;

	    int data1 = DrumType.translate(source.getData1(), delta);
	    return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(),
	        data1, source.getData2()), tick);
	}

	/**
	 * Undoes a drum transposition by restoring original events.
	 * @param e Edit containing transposed events to decompose
	 */
	public void decomposeDrums(Edit e) {
	    ArrayList<MidiEvent> notes = e.getNotes();
	    Prototype destination = e.getDestination();
	    ArrayList<MidiEvent> delete = new ArrayList<>();
	    long start = track.getCurrent() * track.getBarTicks();
	    int delta = DrumType.index(destination.data1) -
	        DrumType.index(((ShortMessage)notes.get(e.getIdx()).getMessage()).getData1());

	    for (MidiEvent note : notes)
	        delete.add(compute(note, delta, destination.tick, start, track.getWindow()));

	    editDel(delete);
	    editAdd(notes);
	}

	/**Computes the transposed tick position for an event.
	 * @param sourceTick Original tick position
	 * @param destination Prototype with tick offset
	 * @return New tick position wrapped within current window*/
	private long computeTransposedTick(long sourceTick, Prototype destination) {
		long tick = sourceTick + (destination.tick * track.getStepTicks());
		long start = track.getCurrent() * track.getBarTicks();
		long window = track.getWindow();
		return MidiTools.wrapTickInWindow(tick, start, window); }

	/**Copies current selection to clipboard. */
	public void copy() {
	    Selection current = this.selection;
	    if (current == null || current.events() == null || current.events().isEmpty()) {
	        return;
	    }
	    clipboard.copy(current.events(), track);
	}

	/**Deletes current selection. */
	public void delete() {
	    Selection current = this.selection;
	    if (current == null || current.events() == null || current.events().isEmpty()) {
	        return;
	    }
	    push(new Edit(Type.DEL, new ArrayList<>(current.events())));
	}

}