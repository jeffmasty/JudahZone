package net.judah.sequencer.editor;

import static net.judah.sequencer.editor.TriggersTable.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import net.judah.api.Command;
import net.judah.sequencer.CommandHandler;
import net.judah.sequencer.editor.Trigger.Type;
import net.judah.util.Console;
import net.judah.util.JudahException;

public class SequencerModel extends DefaultTableModel {

	
	public SequencerModel(List<Trigger> sequence, CommandHandler commander) {
		super (new Object[] { "Type", "Time", "Command", "Notes", "Param"}, 0);
		if (sequence == null) return;
		for (Trigger trigger: sequence) 
			addRow(new Object[] {trigger.getType(), trigger.getTimestamp(), commander.find(trigger.getCommand()), 
					trigger.getNotes(), trigger.getParams()});
	}
	
	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case COL_TYPE: return Type.class;
			case COL_TIME: return Long.class;
			case COL_CMD: return Command.class;
			case COL_NOTE: return String.class;
			case COL_PROP: return HashMap.class;
		}
		return super.getColumnClass(idx);
	}

	@SuppressWarnings( { "rawtypes", "unchecked" })
	public Trigger getRow(int i) throws JudahException {
		Command cmd = ((Command)getValueAt(i,COL_CMD));
		if (cmd == null) throw new JudahException("Sequencer row " + i + " no command.");
		assert cmd.getName() != null;
		return new Trigger((Type)getValueAt(i, COL_TYPE), (long)getValueAt(i, COL_TIME), cmd.getName(),
				getValueAt(i, COL_NOTE).toString(), (HashMap)getValueAt(i, COL_PROP), cmd);
	}
	
	public ArrayList<Trigger> getFilteredData() {
		ArrayList<Trigger> result = new ArrayList<Trigger>();
		for (int i = 0; i < getRowCount(); i++)
			try {
				result.add(getRow(i));
			} catch (JudahException e) {
				// skip empty and error rows (on save)
				Console.warn("Skipping row " + i, e);
			}
		return result;
	}

}
