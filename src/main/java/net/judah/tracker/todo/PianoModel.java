package net.judah.tracker.todo;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.judah.api.Midi;
import net.judah.tracker.Track;
import net.judah.util.RTLogger;

public class PianoModel extends DefaultTableModel implements ListSelectionListener {

	private final Track track;
	private final JudahPiano piano;
	
	public PianoModel(JudahPiano piano) {
		this.piano = piano;
		track = piano.getTrack();
		setColumnCount(1 + 1 * track.getSteps());
	}

	public static int toMidi(int y) {
		return 96 - y;
	}
	
	
	@Override public boolean isCellEditable(int row, int col)
		{ return false; }
	
	@Override public String getColumnName(int col) {
		if (col == 0)
			return "𝅗𝅥";
		if ((col - 1) % track.getDiv() == 0)
			return "" + (1 + (col - 1) / track.getDiv());
		if (col % 2 == 0)
			return "";
		return "+";
	}
	
	@Override public int getRowCount() {
		return 6 * 12 + 1; // 3 octaves above and below middle c 
	}

	@Override public Object getValueAt(int row, int col) {
		if (col == 0) return letter(row);
		int step = row - 1;
		int midi = toMidi(col);
		return track.getCurrent().get(step, midi);
	}

	@Override public Class<?> getColumnClass(int idx) {
		if (idx == 0) return String.class;
		return Midi.class;
	}
	
	/** @return if no flat or sharp, return Note Letter, else blank, C has octave num */
	static String letter(int row) {
		if (row % 12 == 0) 		  return " C" + (6 - row / 12);
		if ( (row + 2) % 12 == 0) return " D";
		if ( (row + 4) % 12 == 0) return " E";
		if ( (row + 5) % 12 == 0) return " F";
		if ( (row + 7) % 12 == 0) return " G";
		if ( (row + 9) % 12 == 0) return " A";
		if ( (row + 11) % 12 == 0) return " B";
		return "";
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int[] select = piano.getSelectedColumns();
//		int midi = toMidi(piano.getSelectedRow());
//		if (select.length == 1) {
//			int step = select[0] - 1;
//			
//			Sequence beats = track.get(track.getCurrent()).get(0);
//			Beat candidate = null;
//			
//			for (Beat b : beats) {
//				if (b.getData2() == midi && b.getStep() == step) {
//					candidate = b;
//					break;
//				}
//			}
//			if (candidate == null) {
//				candidate = new Beat(step, midi);
//				beats.add(candidate);
//				RTLogger.log(this, "step " + step + " " + piano.getSelectedRow() + " to midi " + midi);
//			}
//			else {
//				beats.remove(candidate);
//				RTLogger.log(this, "goodbye " + candidate);
//
//			}
//		} else 
			if (select.length > 1) {
			
			RTLogger.log(this, "BOOYA, " + select.length + " @ " + select[0]);
		}
	}
	
/*
        Point xy = translate(e.getPoint());
        if (xy.y < 0) { // label row clicked, user wants to hear this step
            if (xy.x >= 0 && xy.x< JudahClock.getSteps())
                if (SwingUtilities.isRightMouseButton(e))
                    for (Track t : JudahClock.getInstance().getTracks())
                        t.step(xy.x);
                else
                    track.step(xy.x);
            return;
        }
        if (xy.x >= JudahClock.getSteps() || xy.x < 0) return; // off grid
        if (xy.y >= track.get(track.getCurrent()).size()) return; // off grid
        
        Sequence beats = track.get(track.getCurrent()).get((xy.y));
        Beat b = beats.getStep(xy.x);
        if (b == null) {
            Beat created = new Beat(xy.x);
            beats.add(created);
            // processNoteOff(beats, created);
        }
        else
            beats.remove(b);
        repaint();
	
 */
	
	//    private void processNoteOff(Sequence seq, Beat created) {
	//        int gate = view.getNoteOff();
	//        if (gate == 0) return; // no note off
	//        int next = gate + created.getStep();
	//        if (next >= JudahClock.getSteps())
	//            next -= JudahClock.getSteps();
	//        if (!seq.hasStep(next))
	//            seq.add(new Beat(next, Type.NoteOff));
	//    }

}
