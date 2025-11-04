package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.List;

import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.seq.track.MidiTrack;

/** Display keys of a piano above PianoBox */
public class PianoKeys extends JPanel implements MouseListener, MouseMotionListener, Size {
	public static final List<Integer> BLACK_KEYS = List.of(1, 3, 6, 8, 10);

	private final PianoView view;
	private final MidiTrack track;
	private int width, height;
	private int highlight = -1;
	private int pressed;
	@Getter private int octave = 4;
	private HashSet<Integer> actives = new HashSet<>();

	public PianoKeys(MidiTrack t, PianoView view) {
		this.view = view;
		this.track = t;
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void resized(int w, int h) {
		width = w;
		height = h;
		Gui.resize(this, new Dimension(w, h));
		repaint();
	}

	/** label Octave C
	 * @return true if data1 is integral of 12*/
	public boolean isLabelC(int data1) {
		return data1% 12 == 0;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, width, height);
		int x;

		float noteWidth = view.scaledWidth;
		int keyWidth = (int)noteWidth;

		track.getActives().data1(actives);
		for (int i = 0; i < view.range + 1; i++) {
			x = (int) (i * noteWidth);
			g.drawRect(x, 0, keyWidth, KEY_HEIGHT);
			if (actives.contains(gridToData1(i)))
				rect(g, Pastels.GREEN, x, keyWidth);
			else if (highlight == i) {
				if (BLACK_KEYS.contains(i % 12))
					rect(g, Pastels.YELLOW.darker(), x, keyWidth);
				else
					rect(g, Pastels.YELLOW, x, keyWidth);
			}
			else if (BLACK_KEYS.contains(i % 12))
				g.fillRect(x, 0, keyWidth, KEY_HEIGHT);
			else
				rect(g, Color.WHITE, x, keyWidth);

			if (isLabelC(i + view.tonic))
				g.drawString("" + (i + view.tonic)/12, x + 2, 18);
		}
	}

	private void rect(Graphics g, Color c, int x, int keyWidth) {
		g.setColor(c);
		g.fillRect(x + 1, 1, keyWidth - 1, KEY_HEIGHT - 1);
		g.setColor(Color.GRAY);
	}

	/**Convert x-axis note to midi note*/
	public int gridToData1(int idx) {
			return idx + view.tonic;
	}
	/**Convert midi note to x-axis note */
	public int data1ToGrid(int midi) {
		return midi - view.tonic;
	}

	public void highlight(int data1) {
		int replace = data1 < 0 ? -1 : data1ToGrid(data1);
		if (highlight == replace)
			return;
		highlight = replace;
		repaint();
	}

	public boolean setOctave(boolean up) {
		octave += up ? 1 : -1;
		if (octave < 1)
			octave = 8;
		if (octave > 8)
			octave = 8;
		return true;
	}

	//////  MOUSE  //////

	@Override public void mousePressed(MouseEvent e) {
		sound(false);
		pressed = view.getGrid().toData1(e.getPoint());
		sound(true);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		highlight = (int) (e.getX() / view.scaledWidth);
		repaint();
	}

	@Override public void mouseDragged(MouseEvent e) {
		int next = view.getGrid().toData1(e.getPoint());
		if (next == pressed) return;
		mouseMoved(e);
		sound(false);
		pressed = next;
		sound(true);
	}
	@Override public void mouseReleased(MouseEvent e) {
		sound(false);
	}

	private void sound(boolean on) {
		if (pressed < 0) return;
		ShortMessage msg = Midi.create(on ? Midi.NOTE_ON : Midi.NOTE_OFF, track.getCh(), pressed, (int) (track.getState().getAmp() * 127));
		if (track.getMidiOut() instanceof MidiInstrument)
			JudahMidi.queue(msg, ((MidiInstrument)track.getMidiOut()).getMidiPort());
		else
			track.getMidiOut().send(msg, JudahMidi.ticker());
		if (!on)
			pressed = -1;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}


}
