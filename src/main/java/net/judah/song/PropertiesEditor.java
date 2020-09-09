package net.judah.song;

import java.awt.Component;
import java.util.EventObject;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import lombok.extern.log4j.Log4j;
import net.judah.song.CellDialog.CallBack;

@Log4j
public class PropertiesEditor extends JButton implements TableCellEditor, CallBack {

	int row;
	JTable table;
	HashMap<String, Object> props;
	PropertiesTable editor;
	CellDialog dialog;

	public PropertiesEditor() {
		super("Edit");
		addActionListener( (event) -> openEditor());
	}
	
	@Override
	public Object getCellEditorValue() {
		return props;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		//if (dialog != null) dialog.cancel();
		return true;
	}

	@Override
	public void cancelCellEditing() {
		if (dialog != null) dialog.cancel();
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		props = (HashMap)value;
		this.row = row;
		this.table = table;
		return this;
	}

	private void openEditor() {
		editor = new PropertiesTable(props);
		dialog = new CellDialog(editor, null);
	}

	@Override
	public void callback(boolean ok) {
		if (!ok) return;
		props.clear();
		props.putAll(editor.getMap());
	}
	
}
