package net.judah.seq.automation;

import static net.judah.seq.automation.Automation.MidiMode.*;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import judahzone.api.Midi;
import lombok.Getter;
import net.judah.drumkit.DrumType;
import net.judah.seq.automation.Automation.MidiMode;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiNote;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.NotePairer;
import net.judah.seq.track.PianoTrack;

@Getter
public class MidiModel extends DefaultTableModel {

	static final String[] COL_NAMES = {"Tick", "Type", "CC/Dur", "Val/Vol"};
	static final int COLS = COL_NAMES.length;
	static final int TICK = 0;
	static final int TYPE = 1;
	static final int EVENT = 2;
	static final int VALUE = 3;

	final MidiTrack track;
	final Track t;
	private final List<MidiNote> rows = new ArrayList<>();

	MidiModel(MidiTrack midi, JTable target) {
		super(COL_NAMES, 0);
		this.track = midi;
		this.t = track.getT();

		Set<MidiEvent> handled = new HashSet<>();
		List<Object[]> rowData = new ArrayList<>();

		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (handled.contains(e)) continue;

			MidiMode mode = All;
			MidiNote rowNote;

			if (e.getMessage() instanceof ShortMessage msg) {
				if (Midi.isNoteOn(msg)) {
					mode = NoteOn;
					rowNote = new MidiNote(e, null);
					if (track instanceof PianoTrack) {
						MidiEvent offEvent = NotePairer.getOff(e, t);
						rowNote.setOff(offEvent);
						if (offEvent != null)
							handled.add(offEvent);
					}
				} else {
					rowNote = new MidiNote(e, null);
					if (Midi.isPitchBend(msg))
						mode = Pitch;
					else if (Midi.isCC(msg))
						mode = ControlChange.find(msg) == null ? All : CC;
					else if (Midi.isProgChange(msg))
						mode = Program;
					else if (Midi.isNoteOff(msg)) {
						mode = ERROR; // noteOff error mode
					}
				}
				rows.add(rowNote);
				rowData.add(new Object[] {e.getTick(), mode, rowNote, mode == Program ? msg.getData1() : msg.getData2()});
			}
		}

		for (Object[] r : rowData) {
			addRow(r);
		}

		target.setModel(this);

		CustomRender custom = new CustomRender();
		target.getColumnModel().getColumn(EVENT).setCellRenderer(custom);
		target.getColumnModel().getColumn(TYPE).setCellRenderer(custom);
		target.getColumnModel().getColumn(VALUE).setCellRenderer(custom);
	}


	public MidiEvent getEventAt(int modelRow) {
		if (modelRow >= 0 && modelRow < rows.size()) {
			return rows.get(modelRow);
		}
		return null;
	}


	public int getRowForEvent(MidiEvent event) {
	    // Unwrap MidiNote if needed
	    MidiEvent searchEvent = event;
	    if (event instanceof MidiNote note) {
	        searchEvent = note; // The MidiNote itself is in the rows list
	    }

	    for (int i = 0; i < rows.size(); i++) {
	        MidiNote note = rows.get(i);
	        // Check if the note matches (compare ticks and messages)
	        if (eventsMatch(note, searchEvent)) {
	            return i;
	        }
	        // Also check the off event for piano tracks
	        if (note.getOff() != null && eventsMatch(note.getOff(), searchEvent)) {
	            return i;
	        }
	    }
	    return -1;
	}

	private boolean eventsMatch(MidiEvent a, MidiEvent b) {
	    if (a == b) return true;
	    if (a == null || b == null) return false;

	    // Compare tick and message content
	    if (a.getTick() != b.getTick()) return false;

	    if (a.getMessage() instanceof ShortMessage smA &&
	        b.getMessage() instanceof ShortMessage smB) {
	        return smA.getCommand() == smB.getCommand() &&
	               smA.getChannel() == smB.getChannel() &&
	               smA.getData1() == smB.getData1() &&
	               smA.getData2() == smB.getData2();
	    }

	    return false;
	}

	@Override public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case TICK: return Long.class;
			case TYPE: return MidiMode.class;
			case EVENT: return MidiNote.class;
			case VALUE: return Integer.class;
		}
		return super.getColumnClass(idx);
	}

	// Custom Cell Renderer Example
	class CustomRender extends DefaultTableCellRenderer {

		@Override public Component getTableCellRendererComponent(JTable table, Object value,
	            boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			int modelRow = table.convertRowIndexToModel(row);
			MidiNote pair = rows.get(modelRow);
			MidiMode mode = (MidiMode) getValueAt(modelRow, TYPE);

			if (column == EVENT) {
				long tick = pair.getTick();
				MidiMessage m = pair.getMessage();
				if (mode == CC) {
					ControlChange cc = ControlChange.find(pair.getMessage());
					setValue(cc == null ? "??" : cc.name());
				}
				else if (mode == Program) {
					int data1 = ((ShortMessage)m).getData1();
					String[] patches = track.getPatches();
					if (data1 < patches.length)
						setValue(patches[data1]);
					else
						setValue("" + data1);
				}
				else if (mode == Pitch) {
					ShortMessage p = (ShortMessage)m;
					int bend = p.getData1() | (p.getData2() << 7);
					setValue("Bend: " + (bend - 8192));
				}
				else if (mode == NoteOn) {
					if (track instanceof DrumTrack) {
						int data1 = ((ShortMessage)m).getData1();
						int idx = DrumType.index(data1);
						if (idx >= 0)
							setValue(DrumType.values()[idx].name());
						else
							setValue("? " + data1);
					}
					else if (pair.getOff() == null)
						setValue("???");
					else
						setValue("" + (pair.getOff().getTick() - tick));
				}
				else if (mode == MidiMode.ERROR) // NoteOff error mode
					setValue("unpaired");
				else if (mode == All) {
					setValue("? " + Midi.toString(m));
				}
			} else if (column == VALUE) {
				if (mode == NoteOn || mode == CC) {
					setValue(((ShortMessage)pair.getMessage()).getData2());
				} else if (mode == Program) {
					setValue(((ShortMessage)pair.getMessage()).getData1());
				} else {
					setValue("");
				}
			}

	        return c;
	    }
	}
}