package net.judah.seq.automation;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import judahzone.api.Midi;
import lombok.Getter;
import net.judah.seq.automation.Automation.CCData;
import net.judah.seq.track.Computer;
import net.judah.seq.track.MidiTools;

@Getter
public class CCModel /* extends DefaultTableModel*/ {

	static final String[] COL_NAMES = {"Type", "Tick", "Value"};
	static final int COLS = COL_NAMES.length;
	static final int TYPE = 0;
	static final int TICK = 1;
	static final int VALUE = 2;

	final Computer track;
	final Track t;
	final ArrayList<CCData>    cc = new ArrayList<CCData>();
	final ArrayList<MidiEvent> prog = new ArrayList<MidiEvent>();
	final ArrayList<MidiEvent> pitch = new ArrayList<MidiEvent>();
	final ArrayList<MidiEvent> unhandled = new ArrayList<MidiEvent>();
	int[] roster; // length = steps value = # of CCs in that step

	CCModel(Computer midi) {
		this.track = midi;
		this.t = track.getT();

		populate(0, t.ticks());

	}

	public int[] populate(long start, long end) {
		cc.clear();
		prog.clear();
		pitch.clear();
		unhandled.clear();

		for (int i = MidiTools.find(t, start); i < t.size() && i >= 0; i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage msg) {
				if (Midi.isPitchBend(msg))
					pitch.add(e);
				else if (Midi.isCC(msg)) {
					ControlChange type = ControlChange.find(msg);
					if (type == null)
						unhandled.add(e);
					else
						cc.add(new CCData(e, type));
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

		long left = track.getLeft();
		long right = track.getLeft() + track.getWindow();
		long step = track.getStepTicks();
		for (CCData d : cc)
			if (d.e().getTick() >= left && d.e().getTick() < right)
				roster[(int) ((d.e().getTick() - left) / step)]++;
		return roster;

	}

}
