package net.judah.seq.piano;

import java.awt.event.ActionEvent;

import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.widgets.Click;
import net.judah.gui.MainFrame;
import net.judah.midi.Actives;
import net.judah.midi.Panic;
import net.judah.seq.track.PianoTrack;

public class Pedal extends Click implements Updateable {

	private final PianoTrack track;
	private final Actives buffer;

	public Pedal(PianoTrack t) {
		super("PDL");
		this.track = t;
		this.buffer = track.getActives();
		setFont(Gui.FONT10);
		addActionListener(e->click(e));
		setToolTipText("Right: Panic");
	}

	private void click(ActionEvent e) {
		if (rightClick) {
			setPressed(false);
			new Panic(track);
		}
		else
			setPressed(!buffer.isPedal());
	}

	@Override public void update() {
		setBackground(buffer.isPedal() ? Pastels.GREEN : null);
	}

	public void setPressed(boolean hold) {
		buffer.setPedal(hold);
		MainFrame.update(this);
	}

}
