package net.judah.seq.automation;

import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.table.DefaultTableModel;

import lombok.Getter;
import net.judah.midi.Midi;
import net.judah.seq.MidiTools;
import net.judah.seq.automation.Automation.AutoMode;
import net.judah.seq.track.MidiTrack;

@Getter
public class AllModel extends DefaultTableModel {

	static final String[] COL_NAMES = {"Type", "Tick", "Value"};
	static final int COLS = COL_NAMES.length;
	static final int TYPE = 0;
	static final int TICK = 1;
	static final int VALUE = 2;

	final MidiTrack track;
	final Track t;
	final ArrayList<CCData>    cc = new ArrayList<CCData>();
	final ArrayList<MidiEvent> prog = new ArrayList<MidiEvent>();
	final ArrayList<MidiEvent> pitch = new ArrayList<MidiEvent>();
	final ArrayList<MidiEvent> unhandled = new ArrayList<MidiEvent>();
	int[] roster; // length = steps value = # of CCs in that step

	record Type(MidiEvent e, AutoMode cat, CCData cc) implements Comparable<Type> {

		/* Sort by AutoMode<br>
		Within CC sort by MidiConstants.CC.ordinal() then Tick<br>
		Within other categories sort by Tick*/
		@Override public int compareTo(Type o) {
			if (this == o) return 0;
			int result = Integer.compare(cat.ordinal(), o.cat.ordinal());
			if (result != 0)
				return result;
			if (cc == null)
				return Time.compare(e, o.e);
			result = cc.type().name().compareTo(o.cc.type().name());
			if (result != 0)
				return result;
			return Time.compare(e, o.e);
		}

		@Override public final String toString() {
			// if CC, CC type, else Prog/Pitch/data1
			if (cc != null)
				return cc.type().name();
			else if (cat == AutoMode.All)
				return "" + ((ShortMessage)e.getMessage()).getData1();
			else
				return cat.name();
		}
	}

	record Time(MidiEvent e) implements Comparable<Time> {
		public static int compare(MidiEvent a, MidiEvent b) {
			return Long.compare(a.getTick(), b.getTick());
		}

		@Override public int compareTo(Time other) {
			return compare(e, other.e);
		}
		@Override public final String toString() {
			//TODO bar.beat.tick
			return e.getTick() + "";
		}
	}


	AllModel(MidiTrack midi) {
		super(COL_NAMES, 0);
		this.track = midi;
		this.t = track.getT();


		populate(0, t.ticks());
		for (CCData dat : cc)
			addRow(new Object[] {new Type(dat.e(), AutoMode.CC, dat), new Time(dat.e()),
					((ShortMessage)dat.e().getMessage()).getData2()});

		for (MidiEvent dat : prog)
			addRow(new Object[] {new Type(dat, AutoMode.Prog, null), new Time(dat),
					((ShortMessage)dat.getMessage()).getData1()});

		for (MidiEvent dat : pitch)
			addRow(new Object[] {new Type(dat, AutoMode.Hz, null), new Time(dat),
					((ShortMessage)dat.getMessage()).getData2()}); // TODO lsb/msb

		for (MidiEvent dat : unhandled)
			addRow(new Object[] {new Type(dat, AutoMode.All, null), new Time(dat),
					((ShortMessage)dat.getMessage()).getData2()});


	}

	@Override public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case TYPE: return Type.class;
			case TICK: return Time.class;
			case VALUE: return Integer.class;
		}
		return super.getColumnClass(idx);
	}

//	@Override public void setValueAt(Object val, int row, int column) {
//		if (column == COL_CMD)
//			data.get(row).setCmd((Cmd)val);
//		else
//			data.get(row).setVal("" + val);
//		super.setValueAt(val, row, column);
//		MainFrame.update(JudahZone.getOverview().getScene());
//	}

	public int[] populate(long start, long end) {
		cc.clear();
		prog.clear();
		pitch.clear();
		unhandled.clear();

		for (int i = MidiTools.fastFind(t, start); i < t.size() && i >= 0; i++) {
			MidiEvent e = t.get(i);
			if (e.getTick() < start) continue;
			if (e.getTick() >= end) break;
			if (e.getMessage() instanceof ShortMessage msg) {
				if (Midi.isPitchBend(msg))
					pitch.add(e);
				else if (Midi.isCC(msg)) {
					CC type = CC.find(msg);
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
		long left = track.getLeft(); // step 1
		long step = track.getStepTicks();
		for (CCData d : cc)
			roster[(int) ((d.e().getTick() - left) / step)]++;
		return roster;
	}


}
