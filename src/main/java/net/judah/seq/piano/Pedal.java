package net.judah.seq.piano;

import java.awt.event.ActionEvent;

import judahzone.api.MidiOut.MidiPiano;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.widgets.Click;
import net.judah.gui.MainFrame;
import net.judah.seq.track.PianoTrack;

public class Pedal extends Click implements Updateable {

	private final MidiPiano pedal;
	private final PianoTrack track;

	public Pedal(PianoTrack t, MidiPiano midiOut) {
		super("PDL");
		this.track = t;
		this.pedal = midiOut;
		setFont(Gui.FONT10);
		addActionListener(e->click(e));
		setToolTipText("Right: Panic");
	}

	private void click(ActionEvent e) {
		if (rightClick) {
			setPressed(false);
			track.panic();
		}
		else
			setPressed(!pedal.isPedal());
	}

	@Override public void update() {
		setBackground(pedal.isPedal() ? Pastels.GREEN : null);
	}

	public void setPressed(boolean hold) {
		pedal.setPedal(hold);
		MainFrame.update(this);
	}

}
