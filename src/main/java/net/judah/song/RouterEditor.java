package net.judah.song;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.EventObject;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

import net.judah.midi.MidiClient;
import net.judah.midi.MidiPair;
import net.judah.midi.Route;
import net.judah.util.EditorDialog;
import net.judah.util.MidiForm;

public class RouterEditor implements TableCellEditor {

	private MidiForm fromCard;
	private MidiForm toCard;
	private JCheckBox channel9;
	int column;
	private EditorDialog dialog;
	
	@Override
	public Object getCellEditorValue() {
		switch (column) {
			case 1: return toCard.getMidi();
			default: return fromCard.getMidi();
		}
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.column = column;
		dialog = new EditorDialog("Midi") {
			@Override public void dispose() {
				channel9.setSelected(false);
				toggleAudition();
				super.dispose();
			}};
		MidiPair val = (MidiPair)table.getModel().getValueAt(row, 0);
		fromCard = new MidiForm(val.getFromMidi(), true);
		fromCard.hideData2();
		fromCard.hideDynamic();
		toCard = new MidiForm(val.getToMidi(), true);
		toCard.hideData2();
		toCard.hideDynamic();
		Component c = table.getCellRenderer(row, column).getTableCellRendererComponent(
				table, value, isSelected, isSelected, row, column);
		
		JPanel cards = new JPanel();
		cards.setLayout(new BoxLayout(cards, BoxLayout.PAGE_AXIS));
		cards.add(new JLabel(" "));
		cards.add(new JLabel("From Midi"));
		cards.add(fromCard);
		
		JPanel aisle9 = new JPanel(new FlowLayout());
		channel9 = new JCheckBox("Sample on channel 9");
		boolean selected = val.getFrom() != null && val.getFromMidi().getChannel() == 9;
		channel9.setSelected(selected);
		if (selected) {
			toggleAudition();
		}
		channel9.addActionListener( (event) -> toggleAudition());
		aisle9.add(new JLabel("To Midi"));
		aisle9.add(channel9);
		
		cards.add(new JLabel(" "));
		cards.add(aisle9);
		cards.add(toCard);
		
		boolean result = dialog.showContent(cards);
		if (result) {
			MidiPair route = new MidiPair(fromCard.getMidi(), toCard.getMidi());
			if (c != null && c instanceof JLabel)
				((JLabel)c).setText("" + route);
			table.getModel().setValueAt(route, row, 0);
			new Thread() {
				@Override
				public void run() {
					try {Thread.sleep(5);} catch (InterruptedException e) { }
					((DefaultTableModel)table.getModel()).fireTableDataChanged();
				}
			}.start();
		}
		return c;
	}
	
	private void toggleAudition() {
		if (channel9.isSelected())  {
			MidiClient.getInstance().getRouter().add(new Route(0, 9));
			toCard.setChannel(9);
		}
		else 
			MidiClient.getInstance().getRouter().remove(new Route(0, 9));
	}

	@Override public void cancelCellEditing() {
		if (dialog != null && dialog.isVisible()) {
			dialog.cancel();
		}
	}
	@Override public boolean isCellEditable(EventObject anEvent) { return true; }
	@Override public boolean shouldSelectCell(EventObject anEvent) { return true; }
	@Override public boolean stopCellEditing() { return true; }
	@Override public void addCellEditorListener(CellEditorListener l) {	}
	@Override public void removeCellEditorListener(CellEditorListener l) {	}


}
