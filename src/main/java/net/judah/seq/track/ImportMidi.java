package net.judah.seq.track;

import java.awt.Dimension;
import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.FileChooser;
import net.judah.gui.widgets.ModalDialog;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** Dialog to import a single track from a standard MIDI file*/
public class ImportMidi extends JPanel {
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
		File f = FileChooser.choose(folder);
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
		for (int i = 0; i < tracks.length; i++) 
			if (tracks[i].size() > events) {
				events = tracks[i].size();
				focus = i;
			}
		select.setSelectedIndex(focus);
		
		JButton ok = new Btn("OK", e-> { // import track into bars
			track.importTrack(sequence.getTracks()[select.getSelectedIndex()], sequence.getResolution());
			ModalDialog.getInstance().dispose();});
		JButton cancel = new Btn("Cancel", e->ModalDialog.getInstance().dispose());
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(title);
		add(rez);
		add(Gui.wrap(select, trackData));
		add(Gui.wrap(ok, cancel));
		setName("Import MIDI");

		new ModalDialog(this, SIZE, null);

	}
	
}
