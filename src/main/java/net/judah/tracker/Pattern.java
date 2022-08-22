package net.judah.tracker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.Key;
import net.judah.api.Midi;
import net.judah.midi.NoteOff;
import net.judah.midi.NoteOn;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;

// map of notes per step
// @EqualsAndHashCode(callSuper = false)
public class Pattern extends HashMap<Integer, Notes> implements TableModel, TableCellRenderer, MouseListener {

	public static final String PATTERN_TOKEN = "?@";
	
	@Getter private final Track track;
	@Getter @Setter String name = "A";
	@Getter private final JTable table;
	private TableModelListener listener;
	
	// mouse listener
	private Point mouseDown;
	boolean _released;
	boolean _clicked;
	
	
	public Pattern(String name, Track t) {
		this.name = name;
		this.track = t;
		
		if (t.isDrums())
			table = null;
		else {
			table = new JTable(this);
	        table.setFillsViewportHeight(true);
	        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	        table.setRowSelectionAllowed(false);
	        table.addMouseListener(this);
	        table.setDefaultRenderer(String.class, this);
	        table.setDefaultRenderer(Midi.class, this);
		}
		
	}
	
	public Pattern(String name, Pattern copy, Track t) {
		this(name, t);
		for (int step : copy.keySet()) {
			put(step, new Notes(copy.get(step)));
		}
	}

	// -------------- util/crud -------------------
	@Override
	public boolean equals(Object o) {
		if (false == super.equals(o))
			return false;
		Pattern p = (Pattern)o;
		return p.track.getName().equals(track.getName()) && p.name.equals(name);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + track.num - name.hashCode();
	}
	
	public String forSave(boolean isDrums) {
		StringBuffer sb = new StringBuffer(PATTERN_TOKEN).append(name).append(Constants.NL);
		for (int step : keySet()) {
			if (get(step) == null)
				continue;
			for (Midi m : get(step))
				sb.append(step).append("/").append(m.toString()).append(Constants.NL);
		}
		return sb.toString();
	}
	
	private int num(String s) {
		return Integer.parseInt(s);
	}
	
	public void raw(String line) throws InvalidMidiDataException {	
		String[] split = line.split("/");
		if (split.length < 2) // oops, probably an empty pattern
			return;
		int step = Integer.parseInt(split[0]);
		
		Midi midi = new Midi(num(split[1]), num(split[2]), num(split[3]), num(split[4]));
		
		Notes n = get(step);
		if (n == null) 
			put(step, new Notes(midi));
		else 
			n.add(midi);
	}
	
	public void update(int row) {
		if (listener != null) 
			listener.tableChanged(new TableModelEvent(this, row, row, TableModelEvent.ALL_COLUMNS));
	}

	public void update() {
		if (listener != null) 
			listener.tableChanged(new TableModelEvent(this));
	}
	

	static int toRow(int midi) {
		return midi - 96 * -1;
	}
	
	static int toMidi(int y) {
		return 96 - y;
	}
	

	private void click(int row, int col, int gate) {
		Object o = getValueAt(row, col);

		RTLogger.log(this, "CLICK " + row + " " + col + " " + gate + " : " + o);
		
		if (o == null) {
			noteOn(row, col);
			gate(row, col, gate);
		}
		
		else if (o instanceof Midi) {
			Notes n = getNote(col - 1, toMidi(row));
			RTLogger.log(this, "remove " + o + " of "  + n);
			n.remove(o);
			if (n.isEmpty())
				setValueAt(null, row, col);
			// todo clear left -over noteOffs
		} 
		update(row);
	}
	
	public void noteOn(int row, int col) {
		try {
			int data1 = toMidi(row);
			Midi note = new NoteOn(track.getCh(), data1);
			setValueAt(note, row, col);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
		
	}
	
	private void gate(int row, int col, int gate) {
    	int end = col + gate;
    	if (end >= table.getColumnCount() -1) 
    		return;// TODO ghost noteOff
    		// end = table.getColumnCount() -1;
    	try {
    		Midi noteOff = new NoteOff(track.getCh(), toMidi(row));
    		setValueAt(noteOff, row, end);
    	} catch (InvalidMidiDataException e) {
    		RTLogger.warn(this, e);
    	}
    }
	
	//-----------------  PianoTable Model ---------------
	@Override public void setValueAt(Object midi, int row, int col) {
		if (row == 0) return;
		int data1 = toMidi(row);
		int step = col - 1;
		Notes note = get(step);
		if (midi == null) RTLogger.log(this, "ouch " + step + "/" + data1);
		if (note == null) {
			put(step, new Notes((Midi)midi));
		} else {
			if (note.find(data1) == null) {
				note.add((Midi)midi);
			} else {
				note.remove(note.find(data1));
				if (note.isEmpty())
					remove(step);
			}
		}
	}

	
	@Override public int getColumnCount() {
		return 1/*note name*/ + 1 /* measures */ * track.getSteps();
	}

	@Override public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	/** @return return Note Letter if no sharp of flat, else blank, C has octave num */
	public static String letter(int row) {
		if (row % 12 == 0) 		  return " C" + (6 - row / 12);
		if ( (row + 2) % 12 == 0) return " D";
		if ( (row + 4) % 12 == 0) return " E";
		if ( (row + 5) % 12 == 0) return " F";
		if ( (row + 7) % 12 == 0) return " G";
		if ( (row + 9) % 12 == 0) return " A";
		if ( (row + 11) % 12 == 0) return " B";
		return "";
	}
	
	public Notes getNote(int step, int data1) {
		Notes notes = get(step);
		if (notes == null) 
			return null;
		for (Midi m : notes)
			if (m != null && data1 == m.getData1())
				return notes;
		return null;
	}

	@Override
	public Object getValueAt(int row, int col) {
		
		if (col == 0) return letter(row);
		
		int step = col - 1;
		int midi = toMidi(row);
		if (get(step) == null)
			return null;
		for (Midi msg : get(step)) {
			if (midi == msg.getData1()) {
				return msg;
			}
		}
		return null;
	}
	
	@Override public boolean isCellEditable(int row, int col) { 
		return false; 
	}
	
	@Override public String getColumnName(int col) {
		if (col == 0)
			return "-" + name + "-";
		if ((col - 1) % track.getDiv() == 0)
			return "" + (1 + (col - 1) / track.getDiv());
		if (col % 2 == 0)
			return "";
		return "+";
	}
	
	@Override public int getRowCount() {
		return 6 * 12 + 1; // 3 octaves above and below middle c 
	}

	@Override public void addTableModelListener(TableModelListener l) { 
		this.listener = l;
	}
	@Override public void removeTableModelListener(TableModelListener l) { 
		l = null;
	}

    @Override
    public Component getTableCellRendererComponent(
	        JTable table, Object value, boolean isSelected,
	        boolean hasFocus, int row, int column) {
    	JComponent result = column == 0 ? 
	    	new JLabel(letter(row), JLabel.CENTER)
	    	: new JPanel();
	    result.setOpaque(true);

	    if (column == 0 || value == null) {
	    	result.setBackground(Key.isPlain(toMidi(row)) ? Color.WHITE : Pastels.EGGSHELL);
	    }
	    else if (value instanceof Midi) {
	    	result.setBackground(((ShortMessage)value).getCommand() == ShortMessage.NOTE_ON ? 
	    		Pastels.GREEN : Color.GREEN.darker());
	    }
	    return result;
    }

    
    @Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }

	/** start operations */
	@Override public void mousePressed(MouseEvent evt) {
		mouseDown = evt.getPoint();
//		int col = table.columnAtPoint(mouseDown);
//		int row = table.rowAtPoint(mouseDown);
//		
//		if (Boolean.FALSE.equals(getValueAt(row, col))) {
//			_created = true;
//			noteOn(row, col);
//			update(row);
//		}
	}

	/** single click */
	@Override public void mouseClicked(MouseEvent evt) {
		_clicked = true;
		int col = table.columnAtPoint(mouseDown);
		int row = table.rowAtPoint(mouseDown);
		click(row, col, ((PianoTrack)track).getGate());
    }

	/** drag - n - drop */
	@Override public void mouseReleased(MouseEvent evt) {
				// initial released bug?
		if (!_released) {
			_released = true;
			return;
		}
		if (_clicked) {
			_clicked = false;
			return;
		}

RTLogger.log(this, "released" + print(evt) + on());
		int row = table.rowAtPoint(mouseDown);

		int init = table.columnAtPoint(mouseDown);
		int end = table.columnAtPoint(evt.getPoint());
		if (end > init)
			click(row, init, end - init);
		else 
			click(row, init, ((PianoTrack)track).getGate());
	}

	private String print(MouseEvent evt) {
    	return " " + table.columnAtPoint(evt.getPoint()) + "," + table.rowAtPoint(evt.getPoint());
    }
    private String on() {
    	return mouseDown == null ? " off" : " on";
    }


}
