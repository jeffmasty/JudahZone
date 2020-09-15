package net.judah.song;

import java.util.HashMap;
import java.util.LinkedHashSet;

import javax.swing.table.DefaultTableModel;

import net.judah.CommandHandler;
import net.judah.midi.Midi;
import net.judah.settings.Command;
import net.judah.util.JudahException;

public class LinkModel extends DefaultTableModel {
	
	public LinkModel(LinkedHashSet<Link> links) {
		super (new Object[] { "Name", "Command", "Midi", "Param"}, 0);
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
		addRow(new Object[] { link.getName(), CommandHandler.find(link.getService(), link.getCommand()), 
        		new Midi(link.getMidi()), link.getProps()});
	}

	public LinkedHashSet<Link> getData() throws JudahException {
		LinkedHashSet<Link> result = new LinkedHashSet<Link>();
		for (int i = 0; i < getRowCount(); i++)
			result.add(getRow(i));
		return result;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" })
	public Link getRow(int i) throws JudahException {
		Command cmd = ((Command)getValueAt(i,1));
		if (cmd == null) throw new JudahException("no command for midi link");
		Link link = new Link(getValueAt(i, 0).toString(), cmd.getService().getServiceName(), cmd.getName(),
				((Midi)getValueAt(i, 2)).getMessage(), (HashMap)getValueAt(i, 3));
		return link;
	}

}
