package net.judah.song;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import net.judah.midi.Midi;
import net.judah.midi.MidiForm;
import net.judah.settings.DynamicCommand;
import net.judah.util.EditorDialog;

public class MidiEditor implements TableCellEditor {
	
	private MidiForm midiCard;
	private EditorDialog dialog;
	
	@Override
	public Object getCellEditorValue() {
		return midiCard.getMidi();
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		dialog = new EditorDialog("Midi");
		Object o = table.getModel().getValueAt(row, LinkTable.COMMAND_COL);
		if (o != null && o instanceof DynamicCommand) 
			midiCard = new MidiForm((Midi)value, true);
		else 
			midiCard = new MidiForm((Midi)value);
		Component c = table.getCellRenderer(row, column).getTableCellRendererComponent(
				table, value, isSelected, isSelected, row, column);
		boolean result = dialog.showContent(midiCard);
		if (result) {
			table.setValueAt(midiCard.getMidi(), row, column);
			table.getModel().setValueAt(new Midi(midiCard.getMidi().getMessage()), row, column);
			if (c != null && c instanceof JLabel) 
				((JLabel)c).setText(midiCard.getMidi().toString());
			table.invalidate();
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
