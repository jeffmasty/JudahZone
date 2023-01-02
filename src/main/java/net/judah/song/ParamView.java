package net.judah.song;

import static net.judah.song.ParamModel.*;

import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

public class ParamView extends JTable /* implements Edits */{


	public ParamView(List<Param> sequence) {
		
		setModel(new ParamModel(sequence));
		setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS); // ?
		TableColumnModel cols = getColumnModel(); 
		cols.getColumn(COL_CMD).setPreferredWidth(100);
		cols.getColumn(COL_VAL).setPreferredWidth(100);
		
		setDefaultEditor(Cmd.class, new DefaultCellEditor(new JComboBox<Cmd>(Cmd.values())));
		
		
		
//		table.setDefaultEditor(Command.class, new DefaultCellEditor(
//				new JComboBox<Command>(commander.getAvailableCommands())));
//		table.setDefaultEditor(HashMap.class, new PropertiesEditor());

//		PopupMenu menu = new PopupMenu(this);
//		add(new EditsPane(table, menu));
//		table.setComponentPopupMenu(menu);

	}

//	@Override public void editAdd() {
//		Trigger trigger = new Trigger(Type.ABS, 0l, "", "", new HashMap<String, Object>(), null);
//		if(table.getSelectedRow() < 0)
//			model.addRow(newRow(trigger));
//		else 
//			model.insertRow(table.getSelectedRow() + 1, newRow(trigger));
//	}

//	private Object[] newRow(Trigger trigger) {
//		return new Object[] { trigger.getType(), trigger.getTimestamp(), commander.find(trigger.getCommand()), 
//				trigger.getNotes(), trigger.getParams()};
//	}
	
//	@Override public void editDelete() {
//		int selected = table.getSelectedRow();
//		if (selected < 0) return;
//		model.removeRow(selected);
//	}

//	/**@return triggers suitable for saving*/
//	public ArrayList<Scene> getFilteredData() {
//		return model.getFilteredData();
//	}

//	@Override
//	public List<Copyable> copy() {
//		int selected[] = table.getSelectedRows();
//		if (selected == null || selected.length == 0) return null;
//		List<Copyable> result = new ArrayList<Copyable>();
//		for (int i = 0; i < selected.length; i++) {
//			try {
//				result.add(  model.getRow(selected[i]).clone());
//			} catch (JudahException e) {
//				Console.warn(e.getMessage(), e);
//			}
//		}
//		return result;
//	}

//	@Override
//	public List<Copyable> cut() {
//		List<Copyable> result = copy();
//		if (result != null && !result.isEmpty())
//			editDelete();
//		return result;
//	}

//	@Override
//	public void paste(List<Copyable> clipboard) {
//		if (clipboard == null || clipboard.isEmpty()) return;
//		for (int i = clipboard.size() -1; i >= 0; i--) {
//			Object c = clipboard.get(i);
//			if (c instanceof Trigger) {
//				if (table.getSelectedRow() == -1)
//					model.addRow(newRow((Trigger)c));
//				else 
//					model.insertRow(table.getSelectedRow() + 1, newRow((Trigger)c));
//			}
//		}
//	}
	
}
