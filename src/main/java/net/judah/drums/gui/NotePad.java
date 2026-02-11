package net.judah.drums.gui;

import java.awt.event.MouseEvent;

import javax.sound.midi.Receiver;
import javax.swing.SwingUtilities;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import net.judah.midi.JudahMidi;

public class NotePad implements Gui.Mouse {

	private final int ch, data1, velocity;
	private final Receiver out;
	private final Runnable rightClick;

	public NotePad(Receiver out, byte data1, int ch) {
		this(out, data1, ch, 100, null);
	}

	public NotePad(Receiver out, byte data1, int ch, int velocity, Runnable onRightClick) {
		this.data1 = data1;
		this.out = out;
		this.ch = ch;
		this.velocity = velocity;
		this.rightClick = onRightClick;
	}

	@Override public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) || rightClick == null)
			sound();
		if (rightClick != null && SwingUtilities.isRightMouseButton(e))
			rightClick.run();
	}

	public void sound() {
		Midi click = Midi.create(Midi.NOTE_ON, ch, data1, velocity);
		out.send(click, JudahMidi.ticker());
	}


}
