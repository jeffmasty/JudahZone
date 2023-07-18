package net.judah.song.cmd;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;

public class ParamTable extends JTable {

	public ParamTable(List<Param> sequence) {
		setModel(new ParamModel(sequence));
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		setDefaultEditor(Cmd.class, new DefaultCellEditor(new JComboBox<Cmd>(Cmd.values())));
		setDefaultEditor(String.class, new Options());
	}
	
	
	class Options extends DefaultCellEditor implements ActionListener {
		JComboBox<String> editor = new JComboBox<>();
		int row, col;
		
		public Options() {
			super(new JComboBox<String>());
		}
		
		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			Cmd cmd = (Cmd) getModel().getValueAt(row, 0);
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
	
		@Override public void actionPerformed(ActionEvent e) {
			getModel().setValueAt(editor.getSelectedItem(), row, col);
		}
	}
	
}

