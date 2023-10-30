package net.judah.song.cmd;

import java.util.List;

import javax.swing.table.DefaultTableModel;

import net.judah.JudahZone;
import net.judah.gui.MainFrame;

public class ParamModel extends DefaultTableModel {
	static final int COL_CMD = 0;
	static final int COL_VAL = 1;
	
	private final List<Param> data;
	
	public ParamModel(List<Param> sequence) {
		super (new Object[] { "Command", "Value"}, 0);
		data = sequence;
		data.forEach(p -> addRow(new Object[] {p.getCmd(), p.getVal()}));
	}
	
	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case COL_CMD: return Cmd.class;
			case COL_VAL: return String.class;
		}
		return super.getColumnClass(idx);
	}

	public Param getRow(int i) {
		return data.get(i);
	}
	
	@Override public void setValueAt(Object val, int row, int column) {
		if (column == COL_CMD) 
			data.get(row).setCmd((Cmd)val);
		else 
			data.get(row).setVal("" + val);
		super.setValueAt(val, row, column);
		MainFrame.update(JudahZone.getOverview().getScene());
	}
	
}
