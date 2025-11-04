package net.judah.seq.beatbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import net.judah.JudahZone;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.GMDrum;
import net.judah.gui.MainFrame;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.Musician;
import net.judah.seq.Notes;
import net.judah.seq.Prototype;
import net.judah.seq.track.DrumTrack;
import net.judah.util.RTLogger;

public class RemapTable extends JTable {

	static final String[] cols = {"GMDrum", "Totals", "Target"};
	static final int KIT_COL = 2;

	private DefaultTableModel model = new DefaultTableModel(cols, 0) {
		@Override public Class<?> getColumnClass(int idx) {
			switch (idx) {
				case 0: return String.class;
				case 1: return Integer.class;
				case 2: return DrumType.class;
			}
			return super.getColumnClass(idx);
		}};

	private final DrumType[] values = DrumType.values();
	private final DrumTrack track;
	private final Musician drummer;
	private final Track t;
	private final HashMap<Integer, Vector<MidiEvent>> map = new HashMap<Integer, Vector<MidiEvent>>();
	private final ArrayList<Integer> sorted = new ArrayList<Integer>();
	private final JComboBox<DrumType> combo = new JComboBox<DrumType>(DrumType.values());

	private int row;

	public RemapTable(DrumTrack stub, Musician view) {
		track = stub;
		t = track.getT();
		drummer = view;

		// 1st, build map of totals
		for (int i = 0; i < t.size(); i++) {
			MidiEvent e = t.get(i);
			if (e.getMessage() instanceof ShortMessage m) {
				int data1 = m.getData1();
				if (map.containsKey(data1) == false)
					map.put(data1, new Vector<MidiEvent>());
				map.get(data1).add(e);
			}
		}
		// 2nd, build model
		sorted.addAll(map.keySet());
		Collections.sort(sorted);

		for (Integer data1 : sorted) {
			int idx = DrumType.index(data1);
			model.addRow(new Object[] {nombre(data1), map.get(data1).size(), (idx >= 0 ? values[idx] : null)});
		}

		setModel(model);

		combo.addActionListener(e-> {
			try {
				model.setValueAt(combo.getSelectedItem(), row, KIT_COL);
				if (combo.getSelectedItem() == null)
					return;
				int origin = sorted.get(row);
				DrumType target = (DrumType)combo.getSelectedItem();
				Edit edit = new Edit(Type.REMAP, map.get(origin));
				edit.setOrigin(new Prototype(origin, 0l));
				edit.setDestination(new Prototype(target.getData1(), 0l));
				view.push(edit);
			} catch (Exception ex) {RTLogger.warn(RemapTable.this, ex);}
		});
	}

	private String nombre(int data1) {
		String name = data1 < 10 ? " " : "";
		name += data1 + ":" + GMDrum.lookup(data1);
		return name;
	}

	@Override public TableCellEditor getCellEditor(int row, int column) {
        this.row = row;
		if (KIT_COL == convertColumnIndexToModel(column))
        	return new DefaultCellEditor(combo);
        return super.getCellEditor(row, column);
    }

	public void pad2() {
		int row = getSelectedRow();
		int data1 = sorted.get(row);
		String msg = "Really delete  " + getModel().getValueAt(row, 0) + "?";
		int result = JOptionPane.showConfirmDialog(JudahZone.getFrame(), msg);
		if (result != JOptionPane.YES_OPTION)
			return;
		Notes selected = drummer.selectArea(0, t.ticks(), data1, data1);
		Edit e = new Edit(Type.DEL, selected);
		drummer.push(e);
		// model.removeRow(row);
		MainFrame.setFocus(new RemapView(drummer)); // No undo on Remap View
	}

}
