package net.judah.song;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;

public class Options extends DefaultCellEditor implements ActionListener {

	private final JTable table;
	JComboBox<String> editor = new JComboBox<>();
	int row, col;
	
	public Options(JTable table) {
		super(new JComboBox<String>());
		this.table = table;
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Cmd cmd = (Cmd) table.getModel().getValueAt(row, 0);
		String[] options = Cmd.getCmdr(cmd).getKeys();
		editor.removeActionListener(this);
		editor.removeAllItems();
		for (String s : options) {
			editor.addItem(s);
			if (s.equals(value))
				editor.setSelectedItem(s);
		}
		this.row = row;
		this.col = column;
		editor.addActionListener(this); 
		return editor; 
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		table.getModel().setValueAt(editor.getSelectedItem(), row, col);
	}
	
}
