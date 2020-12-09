package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import net.judah.CommandHandler;
import net.judah.api.Command;
import net.judah.song.Trigger.Type;
import net.judah.util.Console;
import net.judah.util.EditsPane;
import net.judah.util.JudahException;
import net.judah.util.PopupMenu;

public class TriggersTable extends JPanel implements Edits {

	static final int COL_TYPE = 0;
	static final int COL_TIME = 1;
	static final int COL_CMD = 2;
	static final int COL_NOTE = 3;
	static final int COL_PROP = 4;
	
	private final JTable table;
	private final SequencerModel model;
	private final CommandHandler commander;
	
	public TriggersTable(List<Trigger> sequence,  CommandHandler commander) {
		this.commander = commander;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		model = new SequencerModel(sequence, commander);
		table = new JTable(model);
		
		// table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		TableColumnModel cols = table.getColumnModel(); 
		cols.getColumn(COL_TYPE).setPreferredWidth(20);
		cols.getColumn(COL_TIME).setPreferredWidth(20);
		cols.getColumn(COL_CMD).setPreferredWidth(95);
		cols.getColumn(COL_NOTE).setPreferredWidth(115);
		cols.getColumn(COL_PROP).setPreferredWidth(280);
		
		table.setDefaultEditor(Type.class, new DefaultCellEditor(new JComboBox<Type>(Type.values())));
		table.setDefaultEditor(Command.class, new DefaultCellEditor(
				new JComboBox<Command>(commander.getAvailableCommands())));
		table.setDefaultEditor(HashMap.class, new PropertiesEditor());
		
		PopupMenu menu = new PopupMenu(this);
		add(new EditsPane(table, menu));
		table.setComponentPopupMenu(menu);
	}

	@Override public void editAdd() {
		Trigger trigger = new Trigger(Type.ABS, 0l, "", "", new HashMap<String, Object>(), null);
		if(table.getSelectedRow() < 0)
			model.addRow(newRow(trigger));
		else 
			model.insertRow(table.getSelectedRow() + 1, newRow(trigger));
	}

	private Object[] newRow(Trigger trigger) {
		return new Object[] { trigger.getType(), trigger.getTimestamp(), commander.find(trigger.getCommand()), 
				trigger.getNotes(), trigger.getParams()};
	}
	
	@Override public void editDelete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	/**@return triggers suitable for saving*/
	public ArrayList<Trigger> getFilteredData() {
		return model.getFilteredData();
	}

	@Override
	public List<Copyable> copy() {
		int selected[] = table.getSelectedRows();
		if (selected == null || selected.length == 0) return null;
		List<Copyable> result = new ArrayList<Copyable>();
		for (int i = 0; i < selected.length; i++) {
			try {
				result.add(  model.getRow(selected[i]).clone());
			} catch (JudahException e) {
				Console.warn(e.getMessage());
			}
		}
		return result;
	}

	@Override
	public List<Copyable> cut() {
		List<Copyable> result = copy();
		if (result != null && !result.isEmpty())
			editDelete();
		return result;
	}

	@Override
	public void paste(List<Copyable> clipboard) {
		if (clipboard == null || clipboard.isEmpty()) return;
		for (int i = clipboard.size() -1; i >= 0; i--) {
			Object c = clipboard.get(i);
			if (c instanceof Trigger) {
				if (table.getSelectedRow() == -1)
					model.addRow(newRow((Trigger)c));
				else 
					model.insertRow(table.getSelectedRow() + 1, newRow((Trigger)c));
			}
		}
	}
}
