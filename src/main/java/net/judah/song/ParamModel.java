package net.judah.song;

import java.util.List;

import javax.swing.table.DefaultTableModel;

public class ParamModel extends DefaultTableModel {
	static final int COL_CMD = 0;
	static final int COL_VAL = 1;
	
	private final List<Param> model;
	
	public ParamModel(List<Param> sequence) {
		super (new Object[] { "Command", "Value"}, 0);
		model = sequence;
		model.forEach(p -> addRow(new Object[] {p.getCmd(), p.getVal()}));
	}
	
	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case COL_CMD: return Cmd.class;
			case COL_VAL: return Integer.class;
		}
		return super.getColumnClass(idx);
	}

	public Param getRow(int i) {
		return model.get(i);
	}
	
	@Override
	public void setValueAt(Object val, int row, int column) {
		if (column == COL_CMD)
			model.get(row).setCmd((Cmd)val);
		else 
			model.get(row).setVal((int)val);
		super.setValueAt(val, row, column);
	}
//	public ArrayList<Scene> getFilteredData() {
//		ArrayList<Scene> result = new ArrayList<Scene>();
//		for (int i = 0; i < getRowCount(); i++)
//			try {
//				result.add(getRow(i));
//			} catch (JudahException e) {
//				// skip empty and error rows (on save)
//// 				Console.warn("Skipping row " + i, e);
//			}
//		return result;
//	}

}
