package net.judah.seq.automation;

import static net.judah.seq.automation.Automation.MidiMode.*;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import lombok.Getter;
import net.judah.midi.Midi;
import net.judah.seq.MidiNote;
import net.judah.seq.MidiTools;
import net.judah.seq.automation.Automation.MidiMode;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
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

	MidiModel(MidiTrack midi, JTable target) {
		super(COL_NAMES, 0);
		this.track = midi;
		this.t = track.getT();

		Set<MidiEvent> handled = new HashSet<MidiEvent>();

		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			MidiMode mode = All;
			MidiNote row = new MidiNote(e, null);

			if (e.getMessage() instanceof ShortMessage msg) {
				if (Midi.isNoteOn(msg)) {
					mode = NoteOn; // TODO pair NoteOff
					if (track instanceof PianoTrack) {
						row.setOff(MidiTools.getOff(e, t));
						if (row.getOff() != null)
							handled.add(row.getOff());
					}
				}
				else {
					row = new MidiNote(e, null);
					if (Midi.isPitchBend(msg))
						mode = Pitch;
					else if (Midi.isCC(msg))
						mode = ControlChange.find(msg) == null ? All : CC;
					else if (Midi.isProgChange(msg))
						mode = Program;
					else if (Midi.isNoteOff(msg)) {
						if (handled.contains(e))
							continue;
						mode = NoteOff;
					}
				}
				addRow(new Object[] {e.getTick(), mode, row, mode == Program ? msg.getData1() : msg.getData2()});
			}
//TODO			else if (e.getMessage() instanceof MetaMessage)
//				mode = Meta;
		}
		target.setModel(this);

		CustomRender custom = new CustomRender();
		target.getColumnModel().getColumn(EVENT).setCellRenderer(custom);
		target.getColumnModel().getColumn(TYPE).setCellRenderer(custom);
		target.getColumnModel().getColumn(VALUE).setCellRenderer(custom);
	}

	@Override public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case TICK: return Long.class;
			case TYPE: return MidiMode.class;
			case EVENT: return MidiNote.class;
			case VALUE: return Byte.class;
		}
		return super.getColumnClass(idx);
	}

	// Custom Cell Renderer Example
	class CustomRender extends DefaultTableCellRenderer {

		@Override public Component getTableCellRendererComponent(JTable table, Object value,
	            boolean isSelected, boolean hasFocus, int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (column != EVENT)
				return c;

			if (value instanceof MidiNote pair) {
				long tick = pair.getTick();
				MidiMode mode = (MidiMode) getValueAt(row, TYPE);
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
					setValue(p.getData1() + " - " );
				}
				else if (mode == NoteOn) {
					if (track instanceof DrumTrack)
						setValue("");
					else if (pair.getOff() == null)
						setValue("???");
					else
						setValue("" + (pair.getOff().getTick() - tick));
				}
				else if (mode == NoteOff)
					setValue("unpaired");
				else if (mode == All) {
					setValue("? " + Midi.toString(m));
				}
			}
			else
				setValue("??");

	        return c;
	    }
	}

//	@Override public void setValueAt(Object val, int row, int column) {
//		if (column == COL_CMD)
//			data.get(row).setCmd((Cmd)val);
//		else
//			data.get(row).setVal("" + val);
//		super.setValueAt(val, row, column);
//		MainFrame.update(JudahZone.getOverview().getScene());
//	}

}
