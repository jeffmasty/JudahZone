package net.judah.song;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.judah.CommandHandler;
import net.judah.settings.Command;
import net.judah.song.Trigger.Type;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.EditsPane;
import net.judah.util.JudahException;
import net.judah.util.PopupMenu;

public class TriggersTable extends JPanel implements Edits {

	private final JTable table;
	private final SequencerModel model;
	private final CommandHandler commander;
	
	public TriggersTable(List<Trigger> sequence,  CommandHandler commander) {
		this.commander = commander;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		model = new SequencerModel(sequence, commander);
		table = new JTable(model);
		
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(0).setPreferredWidth(60);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);
		table.getColumnModel().getColumn(2).setPreferredWidth(140);
		table.getColumnModel().getColumn(3).setPreferredWidth(20);
		
		table.setDefaultEditor(Command.class, new DefaultCellEditor(
				new JComboBox<Command>(commander.getAvailableCommands())));
		table.setDefaultEditor(HashMap.class, new PropertiesEditor());
		table.setDefaultRenderer(HashMap.class, new TableCellRenderer() {
			@SuppressWarnings("rawtypes")
			@Override public Component getTableCellRendererComponent(JTable table, Object value, 
					boolean isSelected, boolean hasFocus, int row, int column) {
				
				boolean empty = value == null || !(value instanceof HashMap) || ((HashMap) value).isEmpty(); 
				JLabel lbl = new JLabel( empty ? "..." : "abc");
				lbl.setBorder(isSelected ? Constants.Gui.GRAY1 : null);
				lbl.setForeground(isSelected ? Color.BLACK : Color.GRAY);
				return lbl;
			}
		});
		
		PopupMenu menu = new PopupMenu(this);
		add(new EditsPane(table, menu));
		table.setComponentPopupMenu(menu);
	}

	@Override public void editAdd() {
		Trigger trigger = new Trigger(Type.ABSOLUTE, -1l, null, "", "", new HashMap<String, Object>(), null);
		if(table.getSelectedRow() < 0)
			model.addRow(newRow(trigger));
		else 
			model.insertRow(table.getSelectedRow() + 1, newRow(trigger));
	}

	private Object[] newRow(Trigger trigger) {
		return new Object[] { trigger.getTimestamp(), commander.find(trigger.getCommand()), 
				trigger.getNotes(), trigger.getParams()};
	}
	
	@Override public void editDelete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}


	public ArrayList<Trigger> getSequence() throws JudahException {
		return model.getData();
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
		for (Copyable c : clipboard)
			if (c instanceof Trigger) {
				model.addRow(newRow((Trigger)c));
			}
	}
}
