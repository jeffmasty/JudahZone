package net.judah.seq;

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
import net.judah.gui.widgets.FileChooser;
import net.judah.gui.widgets.ModalDialog;
import net.judah.util.RTLogger;

public class ImportMidi extends JPanel {

	private final MidiTrack track;
	
	public ImportMidi(MidiTrack p, File f, int tracknum) {
		this.track = p;
		try {
			processSequence(f, tracknum);
			RTLogger.log(this, "track imported");
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}
	
	public ImportMidi(MidiTrack p, Sequence s) {
		this.track= p;
		trackDialog(s);
		
	}
	
	public ImportMidi(MidiTrack p) {
		this.track = p;
		// select and parse file
		File f = FileChooser.choose(track.getFolder());
		if (f == null) return;
		try {
			processSequence(f, -1);
			RTLogger.log(this, "track imported");
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	private void processSequence(File f, int tracknum) throws Exception {
		Sequence sequence = MidiSystem.getSequence(f);
		Track[] tracks = sequence.getTracks();
		if (tracknum < 0 || tracknum >= tracks.length)
			trackDialog(sequence);
		else {
			track.importTrack(sequence.getTracks()[tracknum], sequence.getResolution());
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
		select.setSelectedIndex(0);
		
		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		
		ok.addActionListener(e-> { // import track into bars
			track.importTrack(sequence.getTracks()[select.getSelectedIndex()], sequence.getResolution());
			ModalDialog.getInstance().dispose();
		});		
		cancel.addActionListener(e->ModalDialog.getInstance().dispose());
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(title);
		add(rez);
		add(Gui.wrap(select, trackData));
		add(Gui.wrap(ok, cancel));
		
		// open select dialog
		new ModalDialog(this, new Dimension(300, 200), null);

	}
	
}
