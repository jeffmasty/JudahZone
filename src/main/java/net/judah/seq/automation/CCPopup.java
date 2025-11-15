package net.judah.seq.automation;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import net.judah.gui.Actionable;
import net.judah.seq.automation.Automation.AutoMode;
import net.judah.seq.track.MidiTrack;

/** lists out CCs for current frame and right-click menu */
public class CCPopup extends JPopupMenu {

	private final AllModel dat;
	private final JComponent view;
	private final boolean lineAxis;
	private final MidiTrack track;

	public CCPopup(MidiTrack midi, JComponent parent, boolean horizontal) {
		track = midi;
		dat = new AllModel(midi);
		view = parent;
		lineAxis = horizontal;
	}

	public void popup(MouseEvent me, int step) {
		removeAll();
		// CCs
	    add(new Actionable("New CC", e->create(me.getPoint())));
	    for (CCData d : getStep(step)) {
	    	add(new Actionable(d.type().toString() + ": " + ((ShortMessage)d.e().getMessage()).getData2(),
	    			e->Automation.getInstance().edit(dat.track, d)));
	    }
	    // TODO PitchBend
	    // ProgChange
	    MidiEvent prog = getProg(step);
	    if (prog == null) {
	    	long tick = track.getFrame() * track.getWindow() + step * track.getStepTicks();
			add(new Actionable("ProgChange", e->Automation.getInstance().init(track, tick, AutoMode.Prog)));
	    }
	    else {
	    	String[] source = track.getMidiOut().getPatches();
	    	int data1 = ((ShortMessage)prog.getMessage()).getData1();
	    	String instr;
	    	if (data1 > source.length)
	    		instr = "" + data1;
	    	else
	    		instr = source[data1];
	    	add(new Actionable("Prog: " + instr, e->Automation.getInstance().edit(track, prog))); // edit w/ delete
	    }
	    show(view, me.getX(), me.getY());
	}


	public ArrayList<CCData> getStep(int step) {
		ArrayList<CCData> result = new ArrayList<CCData>(dat.
				roster[step]);
		long start = track.getLeft() + step * track.getStepTicks();
		long end = start + track.getStepTicks();
		for (CCData cc : dat.cc) {
			if (cc.e().getTick() < start) continue;
			if (cc.e().getTick() > end) break;
			result.add(cc);
		}
		return result;
	}

	public MidiEvent getProg(int step) {
		long start = track.getLeft() + step * track.getStepTicks();
		long end = start + track.getStepTicks();
		for (MidiEvent e : dat.prog) {
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			return e;
		}
		return null;
	}

	private void create(Point point) { // horizontal vs vertical
		// click / length = tick / window;
		int click = lineAxis ? point.x : point.y;
		int length = lineAxis ? view.getWidth() : view.getHeight();
		long zeroBased = click * track.getWindow() / length;
		long tick = track.getLeft() + zeroBased;
		Automation.getInstance().init(track, tick, AutoMode.CC);
	}

	public int[] populate(long start, long end) {
		return dat.populate(start, end);
	}


}
