package net.judah.song;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.table.DefaultTableModel;

import net.judah.CommandHandler;
import net.judah.settings.Command;
import net.judah.song.Trigger.Type;
import net.judah.util.JudahException;

public class SequencerModel extends DefaultTableModel {

	public SequencerModel(ArrayList<Trigger> sequence) {
		super (new Object[] { "Timestamp", "Command", "Notes", "Param"}, 0);
		if (sequence == null) return;
		for (Trigger trigger: sequence) 
			addRow(trigger);
	}

	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case 0: return Long.class;
			case 1: return Command.class;
			case 2: return String.class;
			case 3: return HashMap.class;
		}
		return super.getColumnClass(idx);
	}

	public void addRow(Trigger trigger) {
		addRow(new Object[] { trigger.getTimestamp(), CommandHandler.find(trigger.getService(), trigger.getCommand()), 
        		trigger.getNotes(), trigger.getParams() });
	}

	@SuppressWarnings( { "rawtypes", "unchecked" })
	public Trigger getRow(int i) throws JudahException {
		Command cmd = ((Command)getValueAt(i,1));
		if (cmd == null) throw new JudahException("no command for midi link");
		return new Trigger(Type.ABSOLUTE, (long)getValueAt(i, 0), null, cmd.getService().getServiceName(), cmd.getName(),
				getValueAt(i, 2).toString(), (HashMap)getValueAt(i, 3));
	}

	
	public ArrayList<Trigger> getData() throws JudahException {
		ArrayList<Trigger> result = new ArrayList<Trigger>();
		for (int i = 0; i < getRowCount(); i++)
			result.add(getRow(i));
		return result;
	}

}
