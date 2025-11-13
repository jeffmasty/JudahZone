package net.judah.seq.track;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import lombok.Getter;
import net.judah.gui.Actionable;
import net.judah.midi.Midi;
import net.judah.seq.MidiConstants.CC;
import net.judah.seq.MidiTools;
import net.judah.seq.track.Automation.AutoMode;

/** lists out CCs for current frame and right-click menu */
public class CCHandler extends JPopupMenu {

	public record CCData(MidiEvent e, CC type) {};

	private final ArrayList<CCData> data = new ArrayList<CCData>();
	private final ArrayList<MidiEvent> prog = new ArrayList<MidiEvent>();
	private final ArrayList<MidiEvent> unhandled = new ArrayList<MidiEvent>();
	private final MidiTrack track;
	private final Track t;
	private final JComponent view;
	private final boolean lineAxis;
	@Getter private int[] roster; // length = steps value = # of CCs in that step

	public CCHandler(MidiTrack midi, JComponent parent, boolean horizontal) {
		track = midi;
		t = track.getT();
		view = parent;
		lineAxis = horizontal;
	}

	public void popup(MouseEvent me, int step) {
		removeAll();
		// CCs
	    add(new Actionable("New CC", e->create(me.getPoint())));
	    for (CCData d : getStep(step)) {
	    	add(new Actionable(d.type.toString() + ": " + ((ShortMessage)d.e.getMessage()).getData2(),
	    			e->Automation.getInstance().edit(track, d)));
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

	public int[] populate() {
		data.clear();
		unhandled.clear();
		prog.clear();

		long start = track.getLeft();
		long end = start + track.getWindow();

		for (int i = MidiTools.fastFind(t, start); i < t.size() && i >= 0; i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage msg) {
				if (Midi.isCC(msg)) {
					CC type = CC.find(msg);
					if (type == null)
						unhandled.add(e);
					else
						data.add(new CCData(e, type));
				} else if (Midi.isProgChange(msg))
					prog.add(e);
			}

		}

		int steps = track.getClock().getTimeSig().steps * 2;
		if (roster == null || roster.length != steps)
			roster = new int[steps];
		else
			for (int i = 0; i < steps; i++)
				roster[i] = 0;
		long left = track.getLeft(); // step 1
		long step = track.getStepTicks();
		for (CCData d : data)
			roster[(int) ((d.e.getTick() - left) / step)]++;
		return roster;
	}

	public ArrayList<CCData> getStep(int step) {
		ArrayList<CCData> result = new ArrayList<CCData>(roster[step]);
		long start = track.getLeft() + step * track.getStepTicks();
		long end = start + track.getStepTicks();
		for (CCData cc : data) {
			if (cc.e.getTick() < start) continue;
			if (cc.e.getTick() > end) break;
			result.add(cc);
		}
		return result;
	}

	public MidiEvent getProg(int step) {
		long start = track.getLeft() + step * track.getStepTicks();
		long end = start + track.getStepTicks();
		for (MidiEvent e : prog) {
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


}
