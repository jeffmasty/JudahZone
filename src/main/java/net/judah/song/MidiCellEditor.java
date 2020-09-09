package net.judah.song;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.EventObject;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import lombok.extern.log4j.Log4j;
import net.judah.CommandHandler;
import net.judah.midi.Midi;
import net.judah.midi.MidiListener;
import net.judah.song.CellDialog.CallBack;

@Log4j
public class MidiCellEditor extends JLabel implements TableCellEditor, MidiListener, CallBack {

	Midi feed;
	Midi midi;
	int row;
	int column;
	JTable table;
	JPanel editor;
	JToggleButton midiLearn;
	JTextField midiText;
	CellDialog dialog;
	CommandHandler cmdr = CommandHandler.getInstance();
	
	@Override
	public Object getCellEditorValue() {
		return midi;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean stopCellEditing() {
		return true;
	}

	@Override
	public void cancelCellEditing() {
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		this.midi= (Midi)value;
		this.table = table;
		this.row = row;
		this.column = column;
		setText(midi.toString());
		openEditor();
		return this;
	}

	private void openEditor() {
		editor = new JPanel();
		midiLearn = new JToggleButton("Midi Learn", false);
		midiLearn.addActionListener( (event) -> midiLearn());
		
		midiText = new JTextField(midi.toString(), 15);
		
		editor.setLayout(new FlowLayout());
		editor.add(midiLearn);
		editor.add(midiText);
		dialog = new CellDialog(editor, this);
		
	}

	private void midiLearn() {
		cmdr.setMidiListener(midiLearn.isSelected() ? this : null);
	}

	@Override
	public void feed(Midi midi) {
		feed = midi;
		if (midiText != null && midiText.isVisible()) 
			midiText.setText(midi.toString());
	}
	
	@Override
	public void callback(boolean ok) {
		if (ok && feed != null) {
			try {
				midi.setMessage(feed.getStatus(), feed.getData1(), feed.getData2());
				table.getModel().setValueAt(feed, row, column);
				setText(feed.toString());
				table.repaint();
			} catch (InvalidMidiDataException e) {
				log.error(e.getMessage() + ": " + feed, e);
			}
		}
		cmdr.setMidiListener(null);
	}
}
