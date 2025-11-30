package net.judah.gui.midiimport;

import java.awt.event.ActionEvent;

import javax.sound.midi.Sequence;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.seq.SynthRack;
import net.judah.seq.Seq;
import net.judah.seq.beatbox.RemapView;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.NoteTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.util.RTLogger;

public class ImportTable extends JTable {

	static final int MIDI_COL = 0;
	static final int INFO_COL = 1;
	static final int ZONE_COL = 2;
	static final int BTN_COL = 3;

	private final JComboBox<NoteTrack> combo = new JComboBox<NoteTrack>();
	private final Sequence sequence;
	private final ImportModel model;
	int row;

	public ImportTable(Sequence seq) {

		super(new ImportModel(seq));
		model = (ImportModel) getModel();
		sequence = seq;

		Seq trax = JudahZone.getSeq();
		for (NoteTrack t : SynthRack.getSynthTracks())
			combo.addItem(t);
		for (NoteTrack t :  trax.getDrumTracks())
			combo.addItem(t);
		combo.addActionListener(e->model.setValueAt(combo.getSelectedItem(), row, ZONE_COL));

		Action importCol = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				int midiTrack = Integer.valueOf( e.getActionCommand() );
				NoteTrack track = (NoteTrack)model.getValueAt(midiTrack, ZONE_COL);
				track.importTrack(sequence.getTracks()[midiTrack], sequence.getResolution());
				RTLogger.log(this, "Midi File: " + midiTrack + " to " + track);
				TabZone.edit(track);
				if (track instanceof DrumTrack drumz)
					MainFrame.setFocus(new RemapView(drumz));
			}};
		new ButtonColumn(this, importCol, BTN_COL);

		Action preview = new AbstractAction() {
			@Override public void actionPerformed(ActionEvent e) {
				// 1. create a PianoTrack based on track in midi file
				// 2. open PianoTrack in a PianoView
				// 3. TODO menu import options on PianoView, sequencer sweep
				int midiTrack = Integer.valueOf( e.getActionCommand() );
				String name = "" + model.getValueAt(midiTrack, MIDI_COL);
				NoteTrack target = (NoteTrack)model.getValueAt(midiTrack, ZONE_COL);
				try {
					NoteTrack stub = new PianoTrack(name, target);
					stub.importTrack(sequence.getTracks()[midiTrack], sequence.getResolution());
					TabZone.edit(stub);
				} catch (Throwable t) {
					RTLogger.warn(this, t);
				}
			}};
		new ButtonColumn(this, preview, INFO_COL);
	}

	@Override public TableCellEditor getCellEditor(int row, int column) {
        this.row = row;
        if (ZONE_COL == convertColumnIndexToModel(column))
        	return new DefaultCellEditor(combo);
        return super.getCellEditor(row, column);
    }

}
