package net.judah.song;

import java.util.HashMap;
import java.util.LinkedHashSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.judah.CommandHandler;
import net.judah.midi.Midi;
import net.judah.settings.Command;
import net.judah.util.Constants;
import net.judah.util.JudahException;

public class LinkTable extends JPanel implements Edits {

	public static final int COMMAND_COL = 1;
	
	private final LinkModel model;
	private final JTable table;
	
	public LinkTable(LinkedHashSet<Link> links,  CommandHandler commander) {

		model = new LinkModel(links, commander);
		table = new JTable(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().getColumn(0).setPreferredWidth(70);
		table.getColumnModel().getColumn(COMMAND_COL).setPreferredWidth(115);
		table.getColumnModel().getColumn(2).setPreferredWidth(70);
		table.getColumnModel().getColumn(3).setPreferredWidth(15);
		
		table.setDefaultEditor(Command.class, new DefaultCellEditor(
				new JComboBox<Command>(commander.getAvailableCommands())));
		table.setDefaultEditor(Midi.class, new MidiEditor());
		table.setDefaultEditor(HashMap.class, new PropertiesEditor());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(new JScrollPane(table));
	}

	public LinkedHashSet<Link> getLinks() throws JudahException {
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
		try {
			model.addRow(model.getRow(selected));
		} catch (JudahException e) {
			Constants.infoBox("Invalid source Link data", "Copy Failed");
		}
	}

}

