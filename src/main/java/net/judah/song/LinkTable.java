package net.judah.song;

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

import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.midi.Midi;
import net.judah.settings.Command;

@Log4j
public class LinkTable extends JPanel implements Edits {

	private final LinkModel model;
	private final JTable table;
	
	public LinkTable(List<Link> links) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		model = new LinkModel(links);
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(0).setPreferredWidth(70);
		table.getColumnModel().getColumn(1).setPreferredWidth(115);
		table.getColumnModel().getColumn(2).setPreferredWidth(70);
		table.getColumnModel().getColumn(3).setPreferredWidth(15);
		
		table.setDefaultEditor(Command.class, new DefaultCellEditor(
				new JComboBox<Command>(CommandHandler.getAvailableCommands())));
		table.setDefaultEditor(Midi.class, new MidiCellEditor());
		table.setDefaultEditor(HashMap.class, new PropertiesEditor());
		table.setDefaultRenderer(Midi.class, new TableCellRenderer() {
			@Override public Component getTableCellRendererComponent(JTable table, Object value, 
					boolean isSelected, boolean hasFocus, int row, int column) {
				return new JLabel(value.toString());
			}
		});
		
		add(new JScrollPane(table));
		
	}

	public ArrayList<Link> getLinks() {
		return model.getData();
	}

	@Override public void add() {
			model.addRow(new Link("", "", "", null, new HashMap<String, Object>()));
	}

	@Override public void delete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	@Override public void copy() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.addRow(model.getRow(selected));
	}

}

