package net.judah.util;


import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.judah.song.Edits;

/**JTable for a HashMap
 * @author judah */
public class PropertiesTable extends JPanel implements Edits {

	private final DefaultTableModel model;
	private final JTable table;
	
	public static DefaultTableModel toTableModel(Map<String,Object> map, HashMap<String,Class<?>> definition) {
		if (definition == null) {
			return toTableModel(map);
		}
		
	    DefaultTableModel model = new DefaultTableModel(
	    		new Object[] { "Key", "Value" }, 0);
	    
	    if (map == null) map = new HashMap<String, Object>(definition.size());
	    for (String key : definition.keySet()) {
	    	if (!map.containsKey(key)) {
	    		map.put(key, null);
	    	}
	    }
	    for (Map.Entry<?,?> entry : map.entrySet()) 
	        model.addRow(new Object[] { entry.getKey(), entry.getValue() });
	    return model;
	}
	
	public static DefaultTableModel toTableModel(Map<?,?> map) {
	    DefaultTableModel model = new DefaultTableModel(
	    		new Object[] { "Key", "Value" }, 0);
	    if (map == null) return model;
	    for (Map.Entry<?,?> entry : map.entrySet()) 
	        model.addRow(new Object[] { entry.getKey(), entry.getValue() });
	    return model;
	}
	
	public PropertiesTable(HashMap<String, Object> data, HashMap<String, Class<?>> definition) {
		model = toTableModel(data, definition);
		table = new JTable(model);
		doTable();
	}
	
	public PropertiesTable(HashMap<String, Object> data){
		model = toTableModel(data, null);
		table = new JTable(model);
		doTable();
	}
	
	private void doTable() {
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		table.getColumnModel().getColumn(1).setPreferredWidth(275);
		JScrollPane scroll = new JScrollPane(table);
		Dimension size = new Dimension(model.getRowCount() * 30, 375);
		scroll.setPreferredSize(size);
		scroll.setSize(size);
		setLayout(new GridBagLayout());
		add(table, new GridBagConstraints());
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