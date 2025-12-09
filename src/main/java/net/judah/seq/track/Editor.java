package net.judah.seq.track;

import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JMenu;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.drumkit.DrumType;
import net.judah.gui.Actionable;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.midi.Midi;
import net.judah.midi.Panic;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiNote;
import net.judah.seq.MidiTools;
import net.judah.seq.Prototype;
import net.judah.seq.beatbox.RemapView;
import net.judah.seq.track.Computer.TrackUpdate;
import net.judah.seq.track.Computer.Update;
import net.judah.util.RTLogger;

public class Editor {

	@Getter MidiTrack track;
	protected final Track t;
	private final TrackUpdate update;

	protected final ArrayList<MidiEvent> transfer = new ArrayList<MidiEvent>();

	/** undo/redo */
	protected ArrayList<Edit> stack = new ArrayList<>();
	protected int caret;

	public Editor(MidiTrack midi) {
		track = midi;
		t = track.getT();
		update = new TrackUpdate(Update.EDIT, track);
	}

	protected void execute(Edit e) {
		Type type = e.getType();
		if (type == Type.DEL)
			editDel(e.getNotes());
		else if (type == Type.NEW)
			editAdd(e.getNotes());
		else if (type == Type.MOD)
			mod(e, true);
		else if (type == Type.LENGTH)
			length(e, false);
		else if (type == Type.TRIM)
			trim(e, true);
		else if (type == Type.INS)
			insert(e, true);
		else if (type == Type.TRANS) {
			if (track instanceof NoteTrack notes)
				transpose(notes, e);
		}
		else if (type == Type.REMAP) {
			remap(e, false);
		}
		MainFrame.update(update);
		RTLogger.debug("exe: " + e.getType() + " to " + e.getDestination() + ": " + Arrays.toString(e.getNotes().toArray()));
	}

	public void undo() {
		if (stack.size() <= caret || caret < 0) {
			RTLogger.debug("undo empty");
			return;
		}
		Edit e = stack.get(caret);
		Type type = e.getType();
		if (type == Type.DEL)
			editAdd(e.getNotes());
		else if (type == Type.NEW)
			editDel(e.getNotes());
		else if (type == Type.MOD)
			mod(e, false);
		else if (type == Type.LENGTH)
			length(e, true);
		else if (type == Type.TRIM)
			trim(e, false);
		else if (type == Type.INS)
			insert(e, false);
		else if (type == Type.TRANS) {
			if (track instanceof PianoTrack piano) {
				if (track.isActive())
					new Panic(piano);
				decomposePiano(e);
			}
			else if (track instanceof DrumTrack)
				decomposeDrums(e);
		}
		else if (type == Type.REMAP) {
			remap(e, false);
		}
		caret--;
		MainFrame.update(update);
		RTLogger.debug("undo: " + e.getType());
	}

	private void remap(Edit e, boolean exe) {
		if (track instanceof DrumTrack)
			remapDrums(e, exe);
		else if (track instanceof PianoTrack)
			remapPiano(e, exe);
	}

	/** change or undo e.getNotes() to  e.getDestination().data1;
	 * @param exe false if undo */
	protected void remapDrums(Edit e, boolean exe) {
		int target = exe ? e.getDestination().data1 : e.getOrigin().data1;
		MidiMessage on;
		try {
			for (MidiEvent p : e.getNotes()) {
				on = p.getMessage();
				if (on instanceof ShortMessage midi)
					midi.setMessage(midi.getCommand(), midi.getChannel(), target, midi.getData2());
			}
		} catch (Exception ex) { RTLogger.warn(this, ex); }
		if (MainFrame.getKnobMode() == KnobMode.Remap)
			MainFrame.setFocus(new RemapView((DrumTrack)track));
	}

	protected void remapPiano(Edit e, boolean exe) {
		int diff = e.getDestination().data1;
		if (!exe)
			diff *= -1;
		int data1;
		try {
			for (int i = 0; i< t.size(); i++) {
				if (t.get(i).getMessage() instanceof ShortMessage m) {
					data1 = m.getData1() + diff;
					if (data1 < 0) {
						RTLogger.warn("below MIDI range", m.toString());
						data1 += Key.OCTAVE;
						while (data1 < 0)
							data1 += Key.OCTAVE;
					}
					if (data1 > 127) {
						RTLogger.warn("above MIDI range", m.toString());
						data1 -= Key.OCTAVE;
						while (data1 > 127)
							data1 -= Key.OCTAVE;
					}
				m.setMessage(m.getCommand(), m.getChannel(), data1, m.getData2());
				}
			}
		} catch(InvalidMidiDataException ex) { RTLogger.warn(this, ex); }
	}


	/** execute edit and add to undo stack */
	public void push(Edit e) {
		stack.add(e);
		caret = stack.size() - 1;
		execute(e);
	}

	public void redo() {
		if (stack.size() <= caret + 1)
			return;
		caret++;
		execute(stack.get(caret));
	}

	public Edit peek() {
		if (caret >= stack.size())
			return null;
		return stack.get(caret);
	}

	protected void editAdd(ArrayList<MidiEvent> replace) {
		transfer.clear();
		for (MidiEvent p : replace) {
			if (Midi.isNote(p.getMessage()))
				transfer.add(p); // ignore CCs
			t.add(p);
		}
	}

	void editDel(ArrayList<MidiEvent> list) {
		for (MidiEvent p: list)
			MidiTools.delete(p, t);
	}

	public void paste() {
		push(new Edit(Type.NEW, JudahZone.getClipboard().paste(track)));
	}

	protected final void mod(Edit e, boolean exe) {
		MidiEvent alpha = e.getNotes().getFirst();
		MidiEvent omega = e.getNotes().getLast();

		if (exe) { // remove original cc (on), add changed cc (off)
			MidiTools.delete(alpha, t);
			t.add(omega);
		} else { // undo, restore original cc (on)
			MidiTools.delete(omega, t);
			t.add(alpha);
		}
	}

	protected void length(Edit ed, boolean undo) {
		ArrayList<MidiEvent> replace = new ArrayList<MidiEvent>();
		long ticks = ed.getDestination().tick;

		for (MidiEvent e : ed.getNotes()) {
			replace.add(new MidiEvent(e.getMessage(), e.getTick() + ticks));
		}

		if (undo) {
			editDel(replace);
			editAdd(ed.getNotes());
		}
		else {
			editDel(ed.getNotes());
			editAdd(replace);
		}
	}

	protected void trim(Edit e, boolean exe) {
		long start = e.getOrigin().tick;
		long end = e.getDestination().tick;
		long diff = end - start;
		if (exe) { // remove notes then remove tape (shift remaining notes)
			for (MidiEvent p : e.getNotes())
				t.remove(p);
			MidiTools.removeTape(t, end, diff);
		} else { // undo: shift existing notes then paste deleted notes
			MidiTools.addTape(t, start, diff);
			for (MidiEvent p : e.getNotes())
				t.add(p);
		}
	}

	// add blank tape by moving existing notes
	protected void insert(Edit e, boolean exe) {
		long start = e.getOrigin().tick;
		long end = e.getDestination().tick;
		long diff = end - start;
		if (exe)
			MidiTools.addTape(t, start, diff);
		else  // undo
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
		long start = track.getFrame() * track.getWindow();
		if (!left)
			start += track.barTicks;
		long end = start + track.barTicks;
		Edit trim = new Edit(Type.TRIM, selectBar(left));
		trim.setOrigin(new Prototype(0, start));
		trim.setDestination(new Prototype(0, end));
		push(trim);
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



	public ArrayList<MidiEvent> selectArea(long start, long end) {

		transfer.clear();
		long tick;
		if (track.isDrums()) {
			for (int i = MidiTools.fastFind(t, start); i < t.size() && i >= 0; i++) {
				MidiEvent e = t.get(i);
				if (e.getTick() < start) continue;
				if (e.getTick() >= end) break;
				transfer.add(e);
			}
		}
		else {
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

	// TODO Transpose Drums
	public void transposeDrums(ArrayList<MidiEvent> notes, Prototype destination) {
		editDel(notes);
		ArrayList<MidiEvent> replace = new ArrayList<>();
		int delta = DrumType.index(destination.data1) - DrumType.index(((ShortMessage)notes.getFirst().getMessage()).getData1());
				//click.data1);
		long start = track.getCurrent() * track.getBarTicks();
		for (MidiEvent note : notes)
			replace.add(compute(note, delta, destination.tick, start, track.getWindow()));
		editAdd(replace);
	}

	public void transposePiano(ArrayList<MidiEvent> raw, Prototype destination) {
		if (track.isActive())
			new Panic((PianoTrack)track);
		editDel(raw);
		ArrayList<MidiEvent> replace = new ArrayList<>();
		MidiEvent on = null;
		for (MidiEvent note : raw) {
			if (Midi.isNoteOn(note.getMessage()))
				on = note;
			else {
				MidiNote result = compute(new MidiNote(on, note), destination, track);
				replace.add(result);
				replace.add(result.getOff());
			}
		}
		editAdd(replace);
	}

	public void decomposePiano(Edit e) {

		ArrayList<MidiEvent> delete = new ArrayList<>();
		MidiEvent on = null;
		for (MidiEvent note : e.getNotes())
			if (Midi.isNoteOn(note.getMessage()))
				on = note;
			else {
				MidiNote result = compute(new MidiNote(on, note), e.getDestination(), track);
				delete.add(result);
				delete.add(result.getOff());
			}
		editDel(delete);
		editAdd(e.getNotes());
	}




	/**@param in source note (off is null for drums)
	 * @param destination x = +/-ticks,   y = +/-data1
	 * @return new midi */
	public MidiEvent compute(MidiEvent in, int delta, long protoTick, long start, long window) {
		ShortMessage source = (ShortMessage)in.getMessage();
		long tick = in.getTick() + protoTick * track.getStepTicks();
		if (tick < start) tick += window;
		if (tick >= start + window) tick -= window;
		int data1 =  DrumType.translate(source.getData1(), delta);
		return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick);
	}

	/**@param in source note (off is null for drums)
	 * @param destination x = +/-ticks,   y = +/-data1
	 * @return new midi */
	public static MidiNote compute(MidiNote in, Prototype destination, MidiTrack t) {
		if (in.getMessage() instanceof ShortMessage == false)
			return in;
		MidiEvent on = transposePiano((ShortMessage)in.getMessage(), in.getTick(), destination, t);
		MidiEvent off = null;
		if (in.getOff() != null)
			off = transposePiano((ShortMessage)in.getOff().getMessage(), in.getOff().getTick(), destination, t);
		return new MidiNote(on, off);
	}

	static MidiEvent transposePiano(ShortMessage source, long sourceTick, Prototype destination, MidiTrack t) {
		long window = t.getWindow();
		long start = t.getCurrent() * t.getBarTicks();
		long tick = sourceTick + (destination.tick * t.getStepTicks());

		// TODO wonky, need to split?
		if (tick < start) tick += window;
		if (tick >= start + window) tick -= window;

		int data1 = source.getData1() + destination.data1;
		if (data1 < 0) data1 += Key.OCTAVE;
		if (data1 > 127) data1 -= Key.OCTAVE;
		return new MidiEvent(Midi.create(source.getCommand(), source.getChannel(), data1, source.getData2()), tick);
	}

	public void decomposeDrums(Edit e) {
		ArrayList<MidiEvent> notes = e.getNotes();
		Prototype destination = e.getDestination();
		ArrayList<MidiEvent> delete = new ArrayList<>();
		long start = track.getCurrent() * track.getBarTicks();
		int delta = DrumType.index(destination.data1) - DrumType.index(
				((ShortMessage)notes.get(e.getIdx()).getMessage()).getData1());
		for (MidiEvent note : notes)
			delete.add(compute(note, delta, destination.tick, start, track.getWindow()));
		editDel(delete);
		editAdd(notes);
	}

	private static final String LEFT = "Left";
	private static final String RIGHT = "Right";

	public void tools(JMenu menu) {
		JMenu trim = new JMenu("Trim");
		trim.add(new Actionable("Frame", e->trimFrame()));
		trim.add(new Actionable(LEFT, e->trimBar(true)));
		trim.add(new Actionable(RIGHT, e->trimBar(false)));
		JMenu insert = new JMenu("Insert"); // TODO
		insert.add(new Actionable(LEFT, e->insertBar(true)));
		insert.add(new Actionable(RIGHT, e->insertBar(false)));
		insert.add(new Actionable("Frame", e->insertFrame()));

		menu.add(new Actionable("Track Info...", e->track.info()));
		menu.add(trim);
		menu.add(insert);

	}


}
