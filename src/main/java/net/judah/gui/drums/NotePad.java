package net.judah.gui.drums;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.sound.midi.Receiver;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import judahzone.api.DrumType;
import judahzone.api.Midi;
import judahzone.gui.Gui;
import net.judah.drums.Drumset;
import net.judah.midi.JudahMidi;

public class NotePad extends JLabel implements Gui.Mouse {

	private int velocity;
	private int data1;
	private final Receiver out;
	private final Runnable rightClick;

	public NotePad(Receiver out, int data1) {
	    this(out, data1, 100, null);
	}

	public NotePad(Drumset kit, DrumType type) {
	    super(type.name(), JLabel.CENTER);
	    out = kit;
	    data1 = type.getData1();
	    velocity = 100;
	    rightClick = null;
	    init();
	    setFont(Gui.BOLD11);
	}

	public NotePad(Receiver out, int data1, int velocity, Runnable onRightClick) {
	    this.data1 = data1;
	    this.out = out;
	    this.velocity = velocity;
	    this.rightClick = onRightClick;
	    init();
	}

	private void init() {
	    // Ensure the label receives mouse events and gives a visual affordance.
	    addMouseListener(this);
	    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void rebase(DrumType drum) {
	    this.data1 = drum.getData1();
	    setText(drum.name());
	}

	@Override public void mousePressed(MouseEvent e) {
	    if (rightClick != null && SwingUtilities.isRightMouseButton(e)) {
	        rightClick.run();
	        return;
	    }
	    sound();
	}

	public void sound() {
	    Midi click = Midi.create(Midi.NOTE_ON, data1, velocity);
	    out.send(click, JudahMidi.ticker());
	}

}