package net.judah.widgets;


import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Pastels;
import net.judah.seq.MidiTrack;
import net.judah.seq.Scheduler;

public class TrackButton extends JLabel {
	
	@Getter private final MidiTrack track;
	private final Scheduler state;
	private int now = -1;
    private int next = -1;
	
	public TrackButton(MidiTrack t) {
		setHorizontalAlignment(SwingConstants.CENTER);
		this.track = t;
		state = track.getScheduler();
		setBorder(null);
		setOpaque(true);
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getButton() == 3) 
					JudahZone.getSeq().getTracks().setCurrent(track);
				else 
					track.setActive(!track.isActive());
			}});
		update();
	}
	
	public void update() {
		setBackground(track.isActive() ? Pastels.GREEN : null);
		if (track.getCurrent() == now && state.getNext() == next) 
			return;
		now = track.getCurrent(); 
		next = state.getNext();
		setText(now + "|" + next);
	}
	
	
}
