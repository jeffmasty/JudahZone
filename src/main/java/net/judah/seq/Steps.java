package net.judah.seq;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.sound.midi.Track;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Signature;
import net.judah.seq.track.MidiTrack;

// base edit/undo a Track, particularly CCs
public abstract class Steps extends JPanel implements MouseListener {

	@Getter protected final MidiTrack track;
	protected final Track t;

	public Steps(MidiTrack midi) {
		track = midi;
		t = track.getT();
	}

	public abstract void timeSig(Signature sig);

	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }


}
