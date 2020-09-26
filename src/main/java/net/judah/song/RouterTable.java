package net.judah.song;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import lombok.extern.log4j.Log4j;
import net.judah.midi.MidiPair;
import net.judah.midi.NoteOn;
import net.judah.util.PopupMenu;

@Log4j
public class RouterTable extends JPanel implements Edits {

	private final DefaultTableModel model;
	private final JTable list;

	public RouterTable(List<MidiPair> routes) {
		model = toTableModel(routes);
		list = new JTable(model);
		list.setDefaultEditor(MidiPair.class, new RouterEditor());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JScrollPane(list));
		list.setComponentPopupMenu(new PopupMenu(this));
	}
	
	public static DefaultTableModel toTableModel(List<MidiPair> routes) {
	    DefaultTableModel model = new DefaultTableModel(new String[] { " From  -->   To " }, 0) {
    		@Override public Class<?> getColumnClass(int idx) {return MidiPair.class;}};
        if (routes == null || routes.isEmpty()) return model;
        for (MidiPair pair : routes) 
        	model.addRow(new Object[] {pair});
        return model;
	}
	
	public List<MidiPair> getRoutes() {
		ArrayList<MidiPair> routes = new ArrayList<MidiPair>();
		for (int i = 0; i < model.getRowCount(); i++) {
			routes.add((MidiPair)model.getValueAt(i, 0));
		}
		return routes;
	}
	
	@Override public void editAdd() {
		try {
			model.addRow(new Object[] {new MidiPair(new NoteOn(), new NoteOn())});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override public void editDelete() {
		int selected = list.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}
	
	@Override
	public void paste(List<Copyable> clipboard) {
		if (clipboard == null || clipboard.isEmpty()) return;
		int selected = list.getSelectedRow();
		if (selected < 0) selected = 0;
		for (int i = clipboard.size() -1; i >= 0; i--)
			model.insertRow(selected, new Object[] {(MidiPair)clipboard.get(i)});
		
	}

	@Override
	public List<Copyable> cut() {
		List<Copyable> result = copy();
		if (result != null)
			editDelete();
		return result;
	}
	
	@Override
	public List<Copyable> copy() {
		int selected = list.getSelectedRow();
		if (selected < 0) return null;
		ArrayList<Copyable> result = new ArrayList<>();
		for (int rownum : list.getSelectedRows()) {
			result.add(new MidiPair((MidiPair)model.getValueAt(rownum, 0)));
		}
		return result;
	}
	//	public void copy() {
	//		int selected = list.getSelectedRow();
	//		if (selected < 0) return;
	//		model.addRow(new Object[] { new MidiPair((MidiPair)model.getValueAt(selected, 0))});
	//	}
	
}
