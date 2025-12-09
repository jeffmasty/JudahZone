package net.judah.seq.beatbox;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
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

	public RemapTable(DrumTrack stub, Musician view) {
		model.addTableModelListener(l->{});
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
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) == false)
					return;
				remap();
			}
		});
	}

	private String nombre(int data1) {
		String name = data1 < 10 ? " " : "";
		name += data1 + ":" + GMDrum.lookup(data1);
		return name;
	}

	@Override public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);
		if (e.getColumn() != KIT_COL)
			return;
		int row = e.getFirstRow();
		if (model.getValueAt(row, KIT_COL) == null)
			return;
		DrumType target = (DrumType)model.getValueAt(row, KIT_COL);
		int origin = sorted.get(row);
		if (origin == target.getData1())
			return;
		Edit remap = new Edit(Type.REMAP, map.get(origin));
		remap.setOrigin(new Prototype(origin, 0l));
		remap.setDestination(new Prototype(target.getData1(), 0l));
		track.getEditor().push(remap);
	}

	@Override public TableCellEditor getCellEditor(int row, int column) {
		if (KIT_COL == convertColumnIndexToModel(column))
        	return new DefaultCellEditor(new JComboBox<DrumType>(DrumType.values()));
        return super.getCellEditor(row, column);
    }

	public void remap() {
		int row = getSelectedRow();
		if (row < 0)
			return;
		int data1 = sorted.get(row);
		String msg = "Really delete  " + getModel().getValueAt(row, 0) + "?";
		int result = JOptionPane.showConfirmDialog(JudahZone.getFrame(), msg);
		if (result != JOptionPane.YES_OPTION)
			return;
		Notes selected = drummer.selectArea(0, t.ticks(), data1, data1);
		Edit e = new Edit(Type.DEL, selected);
		track.getEditor().push(e);
		MainFrame.setFocus(new RemapView(drummer)); // No undo on Remap View
	}

}
