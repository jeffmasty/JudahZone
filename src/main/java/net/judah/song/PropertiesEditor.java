package net.judah.song;

import java.awt.Component;
import java.util.EventObject;
import java.util.HashMap;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import net.judah.settings.Command;
import net.judah.util.EditorDialog;
import net.judah.util.PropertiesTable;

public class PropertiesEditor implements TableCellEditor {

	private PropertiesTable cell;
	private EditorDialog dialog;

	@Override
	public Object getCellEditorValue() {
		return cell.getMap();
	}
	
	@Override @SuppressWarnings({ "unchecked", "rawtypes" })
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		dialog = new EditorDialog("Midi");
		HashMap props = (HashMap)value;
		Object o = table.getModel().getValueAt(row, LinkTable.COMMAND_COL);
		if (o == null || false == o instanceof Command) 
			cell = new PropertiesTable((HashMap)value);
		else {
			Command cmd = (Command)o;
			cell = new PropertiesTable(props, cmd.getProps());
		}

		cell = new PropertiesTable((HashMap)value);
		Component c = table.getCellRenderer(row, column).getTableCellRendererComponent(
				table, value, isSelected, isSelected, row, column);
		boolean result = dialog.showContent(cell);
		if (result) {
			table.setValueAt(cell.getMap(), row, column);
			table.getModel().setValueAt(cell.getMap(), row, column);
			table.invalidate();
//			if (c != null && c instanceof JLabel) 
//				((JLabel)c).setText(midiCard.getMidi().toString());
//			else log.warn(c.getClass().getCanonicalName());
		}
		return c;
	}
	
	@Override public void cancelCellEditing() {
		if (dialog != null && dialog.isVisible()) {
			dialog.cancel();
		}
	}
	@Override public boolean isCellEditable(EventObject anEvent) { return true; }
	@Override public boolean shouldSelectCell(EventObject anEvent) { return false; }
	@Override public boolean stopCellEditing() { return true; }
	@Override public void addCellEditorListener(CellEditorListener l) {	}
	@Override public void removeCellEditorListener(CellEditorListener l) {	}

}

/*
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
//		props = (HashMap)value;
//		this.row = row;
//		this.column = column;
//		this.table = table;
//		return this;
//		
		return table.getCellRenderer(row, column).getTableCellRendererComponent(table, value, isSelected, isSelected, row, column);
		
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

*/