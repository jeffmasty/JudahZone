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

@Log4j
public class RouterTable extends JPanel implements Edits {

//	@AllArgsConstructor @Getter @Setter @EqualsAndHashCode
//	public static class FromTo {
//		private Midi from, to;
//		public FromTo(FromTo source) {
//			from = source.from;
//			to = source.to;
//		}
//		@Override
//		public String toString() {
//			return from + " -> " + to;
//		}
//	}
	
	private final DefaultTableModel model;
	private final JTable list;

	public RouterTable(List<MidiPair> routes) {
		model = toTableModel(routes);
		list = new JTable(model);
		list.setDefaultEditor(MidiPair.class, new RouterEditor());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JScrollPane(list));
	}
	
//	public RouterTable(HashMap<byte[], byte[]> routes) {
//		model = toTableModel(routes);
//		list = new JTable(model);
//		list.setDefaultEditor(FromTo.class, new RouterEditor());
//
//		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
//		add(new JScrollPane(list));
//	}
	
	public static DefaultTableModel toTableModel(List<MidiPair> routes) {
	    DefaultTableModel model = new DefaultTableModel(new String[] { " From  -->   To " }, 0) {
    		@Override public Class<?> getColumnClass(int idx) {return MidiPair.class;}};
        if (routes == null || routes.isEmpty()) return model;
        for (MidiPair pair : routes) 
        	model.addRow(new Object[] {pair});
        return model;
	}
	
//	public static DefaultTableModel toTableModel(Map<byte[],byte[]> map) {
//	    DefaultTableModel model = new DefaultTableModel(new String[] { " From  -->   To " }, 0) {
//	    		@Override public Class<?> getColumnClass(int idx) {return FromTo.class;}};
//	    if (map == null) return model;
//	    Midi from, to;
//	    for (Map.Entry<byte[],byte[]> entry : map.entrySet()) {
//	    	from = entry.getKey() == null ? null : new Midi(entry.getKey());
//	    	to = entry.getValue() == null ? null : new Midi(entry.getValue());
//	    	model.addRow(new Object[] {new FromTo (from, to)});
//	    }
//	    return model;
//	}

	public List<MidiPair> getRoutes() {
		ArrayList<MidiPair> routes = new ArrayList<MidiPair>();
		for (int i = 0; i < model.getRowCount(); i++) {
			routes.add((MidiPair)model.getValueAt(i, 0));
		}
		return routes;
	}
	
//	public HashMap<byte[], byte[]> getRoutes() {
//		HashMap<byte[], byte[]> result = new HashMap<byte[], byte[]>();
//		Object o1;
//		for (int i = 0; i < model.getRowCount(); i++) {
//			o1 = model.getValueAt(i, 0);
//			if (o1 != null) { 
//				FromTo val = ((FromTo)o1);
//				result.put(val.getFrom().getMessage(), val.getTo().getMessage());
//			}
//		}
//		return result;
//	}
	
	@Override public void add() {
		try {
			model.addRow(new Object[] {new MidiPair(new NoteOn(), new NoteOn())});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override public void delete() {
		int selected = list.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}
	
	@Override public void copy() {
		int selected = list.getSelectedRow();
		if (selected < 0) return;
		model.addRow(new Object[] { new MidiPair((MidiPair)model.getValueAt(selected, 0))});
	}
	
}
