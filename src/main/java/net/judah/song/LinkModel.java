package net.judah.song;

import java.util.HashMap;
import java.util.LinkedHashSet;

import javax.swing.table.DefaultTableModel;

import net.judah.CommandHandler;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.util.JudahException;

public class LinkModel extends DefaultTableModel {
	private final CommandHandler commander;

	public LinkModel(LinkedHashSet<Link> links, CommandHandler commander) {
		super (new Object[] { "Name", "Command", "Midi", "Param"}, 0);
		this.commander = commander;
		if (links == null) return;
		for (Link link : links)
			addRow(link);
	}

	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case 0: return String.class;
			case 1: return Command.class;
			case 2: return Midi.class;
			case 3: return HashMap.class;
		}
		return super.getColumnClass(idx);
	}

	public void addRow(Link link) {
		addRow(new Object[] { link.getName(), commander.find(link.getCommand()),
        		Midi.copy(link.getMidi()), link.getProps()});
	}

	public LinkedHashSet<Link> getData() throws JudahException {
		LinkedHashSet<Link> result = new LinkedHashSet<>();
		for (int i = 0; i < getRowCount(); i++)
			result.add(getRow(i));
		return result;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" })
	public Link getRow(int i) throws JudahException {
		Command cmd = ((Command)getValueAt(i,1));
		if (cmd == null) throw new JudahException("no command for midi link");
		Link link = new Link(getValueAt(i, 0).toString(), cmd.getName(),
				(Midi)getValueAt(i, 2), new HashMap((HashMap)getValueAt(i, 3)), null);
		return link;
	}

}
