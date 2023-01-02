package net.judah.widgets;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.judah.seq.MidiTrack;

public class TrackFolder extends JComboBox<File> implements ListCellRenderer<File> {

	private final MidiTrack track;
	private ActionListener l = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			track.load((File)getSelectedItem());
		}};
			
	public TrackFolder(MidiTrack t) {
		this.track = t;
		setRenderer(this);
		fill();
	}
	
	public void fill() {
		removeActionListener(l);
		removeAll();
		for (File f : track.getFolder().listFiles()) {
			if (f.isFile()) {
				addItem(f);
				if (f.equals(track.getFile()))
					setSelectedItem(f.getName());
			}
		}
		addActionListener(l);
	}

	private final JLabel render = new JLabel();
	@Override
	public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected,
			boolean cellHasFocus) {
		render.setText(value == null ? "?" : value.getName());
		return render;
	}

	public void update() {
		if (getSelectedItem() == null) {
			if (track.getFile() != null)
				fill();
		}
		else if (false == getSelectedItem().equals(track.getFile()))
			fill();
	}

	
}
