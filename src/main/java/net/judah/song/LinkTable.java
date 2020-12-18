package net.judah.song;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.plugin.MPK;
import net.judah.util.EditsPane;
import net.judah.util.JudahException;
import net.judah.util.PopupMenu;

@Log4j
public class LinkTable extends JPanel implements Edits {

	public static final int COMMAND_COL = 1;
	
	private final LinkModel model;
	private final JTable table;
	
	public LinkTable(LinkedHashSet<Link> links,  CommandHandler commander) {

		model = new LinkModel(links, commander);
		table = new JTable(model);
		table.getColumnModel().getColumn(0).setPreferredWidth(100);
		table.getColumnModel().getColumn(COMMAND_COL).setPreferredWidth(75);
		table.getColumnModel().getColumn(2).setPreferredWidth(112);
		table.getColumnModel().getColumn(3).setPreferredWidth(300);
		
		table.setDefaultEditor(Command.class, new DefaultCellEditor(
				new JComboBox<Command>(commander.getAvailableCommands())));
		table.setDefaultEditor(Midi.class, new MidiEditor());
		table.setDefaultRenderer(Midi.class, new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				return new JLabel(MPK.format((Midi)value));
			}
		});
		table.setDefaultEditor(HashMap.class, new PropertiesEditor());
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		PopupMenu menu = new PopupMenu(this);
		add(new EditsPane(table, menu));
		table.setComponentPopupMenu(menu);
	}

	public LinkedHashSet<Link> getLinks() throws JudahException {
		return model.getData();
	}

	@Override public void editAdd() {
		model.addRow(new Link("", "", null, new HashMap<String, Object>(), null));
	}

	@Override public void editDelete() {
		int selected = table.getSelectedRow();
		if (selected < 0) return;
		model.removeRow(selected);
	}

	@Override
	public List<Copyable> copy() {
		int[] selected = table.getSelectedRows();
		if (selected == null || selected.length == 0) return null;
		List<Copyable> result = new ArrayList<>();
		for (int i = 0; i < selected.length; i++) {
			try {
				result.add(model.getRow(selected[i]).clone());
			} catch (Exception e) {
				log.warn(e.getMessage() + " for link " + i);
			} 

		}
		return result;
	}

	@Override
	public List<Copyable> cut() {
		List<Copyable> result = copy();
		if (result == null || result.isEmpty()) return null;
		editDelete();
		return result;
	}

	@Override
	public void paste(List<Copyable> clipboard) {
		if (clipboard == null) return;
		for (Object o : clipboard) 
			model.addRow((Link)o);
	}
	
//	@Override public void copy() {
//		int selected = table.getSelectedRow();
//		if (selected < 0) return;
//		try {
//			model.addRow(model.getRow(selected));
//		} catch (JudahException e) {
//			Constants.infoBox("Invalid source Link data", "Copy Failed");
//		}
//	}

}

