package net.judah.seq.track;

import java.awt.Dimension;
import java.io.File;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.ModalDialog;
import net.judah.seq.Meta;
import net.judah.seq.MidiConstants;
import net.judahzone.gui.Gui;

/** Dialog to import a single track from a standard MIDI file*/

public class ImportMidi extends JPanel implements MidiConstants {

	public static final Dimension SIZE = new Dimension(300, 200);

	private final MidiTrack track;

	public ImportMidi(MidiTrack p, Sequence s) {
		this.track= p;
		trackDialog(s);
	}

	public ImportMidi(MidiTrack p) {
		this.track = p;
		// select and parse file
		File folder = track.isDrums() ? Folders.getImportDrums() : Folders.getImportMidi();
		File f = Folders.choose(folder);
		if (f == null) return;
		try {
			Sequence sequence = MidiSystem.getSequence(f);
			Track[] tracks = sequence.getTracks();
			var tracknum = -1;
			if (tracks.length == 1)
				tracknum = 0;
			if (tracknum < 0 || tracknum >= tracks.length)
				trackDialog(sequence);
			else
				track.importTrack(sequence.getTracks()[tracknum], sequence.getResolution());
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	public void trackDialog(Sequence sequence) {
		Track[] tracks = sequence.getTracks();
		Integer[] ints = new Integer[tracks.length];
		for (int i = 0; i < ints.length; i++)
			ints[i] = i;
		JLabel title = new JLabel("Import");
		JLabel rez = new JLabel("tracks: " + tracks.length);
		JLabel trackData = new JLabel("");
		JComboBox<Integer> select = new JComboBox<>(ints);
		select.addActionListener(e-> {
			int idx = select.getSelectedIndex();
			Track t = tracks[idx];
			String txt = t.size() + " events " + t.ticks() / (track.getClock().getMeasure() * sequence.getResolution()) + " bars";
			trackData.setText(txt);
		});
		// pre-select largest track
		int focus = 0;
		int events = 0;
		for (int i = 0; i < tracks.length; i++) {
			Track t = tracks[i];
			if (t.size() > events) {
				for (int j = 0; j < t.size(); j++) {
					MidiEvent me = t.get(j);
					if (me.getMessage() instanceof MetaMessage meta) {
						Meta type = Meta.getType(meta);
						if (type == null)
							RTLogger.log(this, "Skipping type " + meta.getType() + " payload: " + new String(meta.getData()));
						if (Meta.TRACK_NAME == type)
							RTLogger.log(this, "Track " + i + " NAME: " + new String(meta.getData()));
						else if (Meta.INSTRUMENT == type)
							RTLogger.log(this, "INSTR: " + new String(meta.getData()));
					}
				}
				events = t.size();
				focus = i;
			}
		}
		select.setSelectedIndex(focus);

		JButton ok = new Btn("OK", e-> { // import track into bars
			track.importTrack(sequence.getTracks()[select.getSelectedIndex()], sequence.getResolution());
			ModalDialog.getInstance().dispose();});
		JButton cancel = new Btn("Cancel", e->ModalDialog.getInstance().dispose());

		add(title);
		add(rez);
		add(Gui.wrap(select, trackData));
		add(Gui.wrap(ok, cancel));
		setName("Import MIDI");

		new ModalDialog(this, SIZE);

	}

}
