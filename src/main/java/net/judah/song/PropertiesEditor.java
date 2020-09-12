package net.judah.song;

import java.awt.Component;
import java.util.EventObject;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import lombok.extern.log4j.Log4j;
import net.judah.settings.Command;
import net.judah.song.CellDialog.CallBack;

@Log4j
public class PropertiesEditor extends JButton implements TableCellEditor, CallBack {

	public static final int COMMAND_COLUMN = 1;
	
	int row, column;
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
		this.column = column;
		this.table = table;
		return this;
	}

	private void openEditor() {
		Object o = table.getModel().getValueAt(row, COMMAND_COLUMN);
		if (o == null) 
			editor = new PropertiesTable(props);
		else {
			Command cmd = (Command)table.getModel().getValueAt(row, COMMAND_COLUMN);
			editor = new PropertiesTable(props, cmd.getProps());
		}
		dialog = new CellDialog(editor, this);
	}

	@Override
	public void callback(boolean ok) {
		if (!ok) return;
		table.getModel().setValueAt(editor.getMap(), row, column);
		log.info("Callback + " + Command.toString(editor.getMap()));
		
	}
	
}
