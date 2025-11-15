package net.judah.seq.automation;

public class CCHandler {

//	@Getter final ArrayList<CCData>    cc = new ArrayList<CCData>();
//	@Getter final ArrayList<MidiEvent> prog = new ArrayList<MidiEvent>();
//	@Getter final ArrayList<MidiEvent> pitch = new ArrayList<MidiEvent>();
//	@Getter final ArrayList<MidiEvent> unhandled = new ArrayList<MidiEvent>();
//	@Getter int[] roster; // length = steps value = # of CCs in that step
//	@Getter final Track t;
//
//	@Getter final MidiTrack track;

//	public CCHandler(MidiTrack midi) {
//		track = midi;
//		t = track.getT();
//	}
//	public int[] populate(long start, long end) {
//		cc.clear();
//		prog.clear();
//		pitch.clear();
//		unhandled.clear();
//
//		for (int i = MidiTools.fastFind(t, start); i < t.size() && i >= 0; i++) {
//			MidiEvent e = t.get(i);
//			if (e.getTick() < start) continue;
//			if (e.getTick() >= end) break;
//			if (e.getMessage() instanceof ShortMessage msg) {
//				if (Midi.isPitchBend(msg))
//					pitch.add(e);
//				else if (Midi.isCC(msg)) {
//					CC type = CC.find(msg);
//					if (type == null)
//						unhandled.add(e);
//					else
//						cc.add(new CCData(e, type));
//				} else if (Midi.isProgChange(msg))
//					prog.add(e);
//			}
//
//		}
//
//		int steps = track.getClock().getTimeSig().steps * 2;
//		if (roster == null || roster.length != steps)
//			roster = new int[steps];
//		else
//			for (int i = 0; i < steps; i++)
//				roster[i] = 0;
//		long left = track.getLeft(); // step 1
//		long step = track.getStepTicks();
//		for (CCData d : cc)
//			roster[(int) ((d.e().getTick() - left) / step)]++;
//		return roster;
//	}
//

}
