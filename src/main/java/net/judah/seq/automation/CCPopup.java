package net.judah.seq.automation;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import judahzone.gui.Actionable;
import net.judah.seq.automation.Automation.CCData;
import net.judah.seq.automation.Automation.MidiMode;
import net.judah.seq.track.MidiTrack;

/** lists out CCs for current frame and right-click menu */
public class CCPopup extends JPopupMenu {

	private final MidiTrack track;
	private final boolean lineAxis;
	private final JComponent view;
	private final CCModel dat;
	private final Automation automation;

	public CCPopup(MidiTrack midi, JComponent parent, boolean horizontal, Automation auto) {
		track = midi;
		dat = new CCModel(midi);
		view = parent;
		lineAxis = horizontal;
		automation = auto;
	}

	public void popup(MouseEvent me, int step) {
		removeAll();
		// CCs
	    add(new Actionable("New CC", e->create(me.getPoint())));
	    for (CCData d : getStep(step)) {
	    	add(new Actionable(d.type().toString() + ": " + ((ShortMessage)d.e().getMessage()).getData2(),
	    			e->automation.edit((MidiTrack)dat.track, d))); // TODO
	    }
	    // Pitch Bend
	    MidiEvent bend = getPitch(step);
	    if (bend == null) {
	        long tick = track.getFrame() * track.getWindow() + step * track.getStepTicks();
	        add(new Actionable("Pitch Bend", e -> automation.init(track, tick, MidiMode.Pitch)));
	    } else {
	        add(new Actionable("Pitch Bend", e -> automation.edit(track, bend)));
	    }
	    // ProgChange
	    if (track instanceof MidiTrack t) {
		    MidiEvent prog = getProg(step);
		    if (prog == null) {
		    	long tick = track.getFrame() * track.getWindow() + step * track.getStepTicks();
				add(new Actionable("ProgChange", e->automation.init(t, tick, MidiMode.Program)));
		    }
		    else {
		    	String[] source = t.getPatches();
		    	int data1 = ((ShortMessage)prog.getMessage()).getData1();
		    	String instr;
		    	if (data1 > source.length)
		    		instr = "" + data1;
		    	else
		    		instr = source[data1];
		    	add(new Actionable("Prog: " + instr, e->automation.edit(track, prog))); // edit w/ delete // TODO
		    }
	    }
	    JMenu tools = new JMenu("Tools");
	    track.getEditor().tools(tools);
	    add(tools);
	    show(view, me.getX(), me.getY());
	}


	public ArrayList<CCData> getStep(int step) {
		ArrayList<CCData> result = new ArrayList<CCData>(dat.
				roster[step]);
		long start = track.getLeft() + step * track.getStepTicks();
		long end = start + track.getStepTicks();
		for (CCData cc : dat.cc) {
			if (cc.e().getTick() < start) continue;
			if (cc.e().getTick() >= end) break; // fixed: exclude events exactly at end
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

	public MidiEvent getPitch(int step) {
	    long start = track.getLeft() + step * track.getStepTicks();
	    long end = start + track.getStepTicks();
	    for (MidiEvent e : dat.pitch) {
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
		automation.init(track, tick, MidiMode.CC); // TODO
	}

	public int[] populate(long left, long right) {
		return dat.populate(left, right);
	}

}
