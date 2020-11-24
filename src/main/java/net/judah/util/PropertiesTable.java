package net.judah.util;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.judah.song.Edits;

/**JTable for a HashMap
 * @author judah */
public class PropertiesTable extends JPanel implements Edits {

	private final DefaultTableModel model;
	private final JTable table;
	private final boolean popup;
	
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
	
	
	/** Has an associated command (is a popup window) */
	public PropertiesTable(HashMap<String, Object> data, HashMap<String, Class<?>> definition) {
		model = toTableModel(data, definition);
		table = new JTable(model);
		popup = true;
		doTable();
	}
	
	public PropertiesTable(HashMap<String, Object> data){
		model = toTableModel(data, null);
		table = new JTable(model);
		popup = false;
		doTable();
	}
	
	private void doTable() {
		PopupMenu menu = new PopupMenu(this);
		table.setComponentPopupMenu(menu);
		
		if (popup) {
			table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			table.getColumnModel().getColumn(0).setPreferredWidth(100);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			setLayout(new GridBagLayout());
			add(table, new GridBagConstraints());
		}
		else {
			EditsPane scroller = new EditsPane(table, menu);
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			add(scroller);
		}
		
		
		
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public HashMap<String, Object> getMap() {
		HashMap result = new HashMap<String, Object>();
		for (int i = 0; i < model.getRowCount(); i++)
			result.put(model.getValueAt(i, 0), model.getValueAt(i, 1));
		return result;
	}

	@Override public void editAdd() {
		model.addRow(new Object[] {"", ""});
	}

	@Override public void editDelete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	@Override
	public List<Copyable> copy() {
		if (table.getSelectedRow() < 0) return null;
		List<Copyable> result = new ArrayList<>();
		for (int rownum : table.getSelectedRows()) {
			result.add(new KeyPair((String)model.getValueAt(rownum, 0), model.getValueAt(rownum, 1)));
		}
		return result;
	}

	@Override
	public List<Copyable> cut() {
		List<Copyable> result = copy();
		if (result == null || result.isEmpty()) return null;
		editDelete();
		return result;
	}

	@Override
	public void paste(List<Copyable> clipboard) {
		if (clipboard == null || clipboard.isEmpty()) return;
		for (Object o : clipboard) {
			KeyPair prop = (KeyPair)o;
			model.addRow(new Object[] {prop.getKey(), prop.getValue()});
		}
	}

//	@Override public void copy() {
//		int selected = table.getSelectedRow();
//		if (selected < 0) return;
//		model.addRow(new Object[] {model.getValueAt(selected, 0), model.getValueAt(selected, 1)});
//	}
	
}