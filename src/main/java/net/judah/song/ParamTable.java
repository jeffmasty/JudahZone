package net.judah.song;

import static net.judah.song.ParamModel.*;

import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

public class ParamTable extends JTable {

	public ParamTable(List<Param> sequence) {
		setModel(new ParamModel(sequence));
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumnModel cols = getColumnModel(); 
		cols.getColumn(COL_CMD).setPreferredWidth(100);
		cols.getColumn(COL_VAL).setPreferredWidth(100);
		setDefaultEditor(Cmd.class, new DefaultCellEditor(new JComboBox<Cmd>(Cmd.values())));
		setDefaultEditor(String.class, new Options(this));
	}
}

