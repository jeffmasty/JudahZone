package net.judah.tracker.todo;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import lombok.Getter;
import net.judah.tracker.Track;

public class JudahPiano extends JTable implements MouseListener {

	@Getter private final Track track;
	private PianoModel model;
	
	public JudahPiano(Track t, Dimension size) {
		track = t;
		setPreferredScrollableViewportSize(size);
        setFillsViewportHeight(true);
        model = new PianoModel(this);
        setModel(model);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(model);
        setRowSelectionAllowed(false);
        addMouseListener(this);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
	
	
}
