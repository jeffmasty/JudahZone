package net.judah.seq.automation;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

public class AllTable extends JTable {

	public AllTable() {
		setRowSelectionAllowed(true);
		setColumnSelectionAllowed(false);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setAutoCreateRowSorter(true);


	}


}
