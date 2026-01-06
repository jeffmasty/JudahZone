package net.judah.gui.midiimport;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.table.DefaultTableModel;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import net.judah.seq.SynthRack;
import net.judah.seq.Meta;
import net.judah.seq.track.MidiTrack;

/* MidiFile Import Table
 *
 *Trck#	 Notes
 *Name   [ # ]  Assign[]  [Import]
 */
public class ImportModel extends DefaultTableModel implements MidiConstants {

	static final String[] cols = new String[] {"Source", "Notes", "AssignTo", "Action"};
	static final String IMPORT = "Import";

	public ImportModel(final Sequence sequence) {
		super (cols, 0);

		MidiTrack basic = SynthRack.getSynthTracks().getFirst();

		MidiMessage msg;
		String name;
		for (int i = 0;i < sequence.getTracks().length; i++) {

			Track t = sequence.getTracks()[i];
			name = i + ":";
			int noteCount = 0;
			for (int n = 0; n < t.size(); n++) {
				msg = t.get(n).getMessage();
				if (msg instanceof ShortMessage shrt && Midi.isNoteOn(shrt))
					noteCount++;
				else if (msg instanceof MetaMessage meta) {
					Meta type = Meta.getType(meta);
					if (type == Meta.TRACK_NAME || type == Meta.DEVICE || type == Meta.INSTRUMENT)
						name += new String(meta.getData()) + " ";
				}
			}
			addRow(new Object[] {name, noteCount, basic, IMPORT});
		}
	}

	@Override
	public Class<?> getColumnClass(int idx) {
		switch (idx) {
			case 0: return String.class;
			case 1: return Integer.class;
			case 2: return MidiTrack.class;
			case 3: return String.class;
		}
		return super.getColumnClass(idx);
	}


}
