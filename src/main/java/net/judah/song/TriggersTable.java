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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.judah.CommandHandler;
import net.judah.settings.Command;
import net.judah.song.Trigger.Type;
import net.judah.util.Constants;
import net.judah.util.JudahException;

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
		
		add(new JScrollPane(table));
	}

	@Override public void add() {
		Trigger trigger = new Trigger(Type.ABSOLUTE, -1l, null, "", "", "", new HashMap<String, Object>());
		Object[] data = new Object[] { trigger.getTimestamp(), commander.find(trigger.getService(), trigger.getCommand()), 
				trigger.getNotes(), trigger.getParams()}; 
		
		if(table.getSelectedRow() < 0)
			model.addRow(data);
		else 
			model.insertRow(table.getSelectedRow() + 1, data);
	}

	@Override public void delete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	@Override public void copy() {
		Constants.infoBox("Coming Soon...", "Sequencer");
	}

	public ArrayList<Trigger> getSequence() throws JudahException {
		return model.getData();
	}
}
