package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import net.judah.CommandHandler;
import net.judah.midi.Midi;
import net.judah.settings.Command;

public class LinkModel extends DefaultTableModel {
	
	public static final String SPLIT = ">";
	
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
	
	public LinkModel(List<Link> links) {
		super (new Object[] { "Name", "Command", "Midi", "Param"}, 0);
		if (links == null) return;
		for (Link link : links)
			addRow(link);
	}

	public void addRow(Link link) {
		addRow(new Object[] { link.getName(), CommandHandler.find(link.getService(), link.getCommand()), 
        		new Midi(link.getMidi()), link.getProps()});
	}

	public ArrayList<Link> getData() {
		ArrayList<Link> result = new ArrayList<Link>();
		for (int i = 0; i < getRowCount(); i++)
			result.add(getRow(i));
		return result;
	}

	@SuppressWarnings( { "rawtypes", "unchecked" })
	public Link getRow(int i) {
		Command cmd = ((Command)getValueAt(i,1));
		if (cmd == null) throw new NullPointerException("no command for midi link");
		Link link = new Link(getValueAt(i, 0).toString(), cmd.getService().getServiceName(), cmd.getName(),
				((Midi)getValueAt(i, 2)).getMessage(), (HashMap)getValueAt(i, 3));
		return link;
	}

}
