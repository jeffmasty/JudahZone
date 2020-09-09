package net.judah.song;


import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**JTable for a HashMap
 * @author judah */
public class PropertiesTable extends JPanel implements Edits {

	private final DefaultTableModel model;
	private final JTable table;
	
	public static DefaultTableModel toTableModel(Map<?,?> map) {
	    DefaultTableModel model = new DefaultTableModel(
	    		new Object[] { "Key", "Value" }, 0);
	    if (map == null) return model;
	    for (Map.Entry<?,?> entry : map.entrySet()) 
	        model.addRow(new Object[] { entry.getKey(), entry.getValue() });
	    return model;
	}
	
	public PropertiesTable(HashMap<String, Object> data){
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		model = toTableModel(data);
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(0).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setPreferredWidth(300);
		add(new JScrollPane(table));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap<String, Object> getMap() {
		HashMap result = new HashMap<String, Object>();
		for (int i = 0; i < model.getRowCount(); i++)
			result.put(model.getValueAt(i, 0), model.getValueAt(i, 1));
		return result;
	}

	@Override public void add() {
		model.addRow(new Object[] {"", ""});
	}

	@Override public void delete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	@Override public void copy() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.addRow(new Object[] {model.getValueAt(selected, 0), model.getValueAt(selected, 1)});
	}
	
}