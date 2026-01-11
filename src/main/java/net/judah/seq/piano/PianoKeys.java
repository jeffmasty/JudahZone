package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;

import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;

import judahzone.api.Midi;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import lombok.Getter;
import net.judah.gui.Size;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.seq.piano.PianoView.Orientation;
import net.judah.seq.track.NoteTrack;

/** Display keys of a piano above/beside PianoBox, respecting orientation. */
public class PianoKeys extends JPanel implements Gui.Mouser, Size {
	public static final List<Integer> BLACK_KEYS = List.of(1, 3, 6, 8, 10);

	private final PianoView view;
	private final NoteTrack track;
	private int width, height;
	private int highlight = -1;
	private int pressed = -1;
	@Getter private int octave = 4;
	private HashSet<Integer> actives = new HashSet<>();

	private Orientation orientation = Orientation.NOTES_X;
	private float keyHeight; // pixels per note
	private int visible;     // range + 1

	public PianoKeys(NoteTrack t, PianoView v) {
		view = v;
		track = t;
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void resized(int w, int h) {
		width = w;
		height = h;
		Gui.resize(this, new Dimension(w, h));
		calculateUnits();
		repaint();
	}

	/** Precompute units based on current orientation and dimensions. */
	public void calculateUnits() {
		visible = view.range + 1;
		if (orientation == Orientation.NOTES_X) {
			keyHeight = width / (float) visible;
		} else {
			keyHeight = height / (float) visible;
		}
	}

	/** Label Octave C: true if data1 is integral of 12 */
	public boolean isLabelC(int data1) {
		return data1 % 12 == 0;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, width, height);

		if (orientation == Orientation.NOTES_X) {
			paintNotesX(g);
		} else {
			paintNotesY(g);
		}
	}

	/** NOTES_X: notes laid out horizontally (left to right). */
	private void paintNotesX(Graphics g) {
		track.getActives().data1(actives);
		for (int i = 0; i < visible; i++) {
			int x = (int) (i * keyHeight);
			int midi = i + view.tonic;
			drawKeyRect(g, i, midi, x, 0, (int) keyHeight, UNIT);
			if (isLabelC(midi))
				g.drawString("" + (midi / 12), x + 2, 18);
		}
	}

	/** NOTES_Y: notes laid out vertically (top to bottom, high to low). */
	private void paintNotesY(Graphics g) {
		track.getActives().data1(actives);
		for (int i = 0; i < visible; i++) {
			int y = (int) (i * keyHeight);
			/* High notes on top, low notes on bottom */
			int midi = (view.tonic + view.range) - i;
			drawKeyRect(g, i, midi, 0, y, UNIT, (int) keyHeight);
			if (isLabelC(midi))
				g.drawString("" + (midi / 12), 4, y + 14);
		}
	}

	private void drawKeyRect(Graphics g, int i, int data1, int x, int y, int w, int h) {
		g.drawRect(x, y, w, h);
		/* Determine black-key-ness using MIDI pitch modulo 12 */
		boolean isBlack = BLACK_KEYS.contains((data1 % 12 + 12) % 12);
		if (actives.contains(data1))
			rect(g, Pastels.GREEN, x, y, w, h);
		else if (highlight == data1)
			rect(g, isBlack ? Pastels.YELLOW.darker() : Pastels.YELLOW, x, y, w, h);
		else if (isBlack)
			g.fillRect(x + 1, y + 1, w - 1, h - 1);
		else
			rect(g, Color.WHITE, x, y, w, h);
	}

	private void rect(Graphics g, Color c, int x, int y, int w, int h) {
		g.setColor(c);
		g.fillRect(x + 1, y + 1, w - 1, h - 1);
		g.setColor(Color.GRAY);
	}

	public void highlight(int data1) {
		if (data1 < view.tonic || data1 > view.tonic + view.range)
			data1 = -1;
		if (data1 == highlight)
			return;
		highlight = data1;
		repaint();
	}

	public boolean setOctave(boolean up) {
		octave += up ? 1 : -1;
		if (octave < 1) octave = 1;
		if (octave > 8) octave = 8;
		return true;
	}

	@Override public void mousePressed(MouseEvent e) {
		sound(false);
		int midi = toData1(e.getX(), e.getY());
		pressed = Math.max(-1, Math.min(view.tonic + view.range, midi));
		sound(true);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int midi = toData1(e.getX(), e.getY());
		if (midi < view.tonic || midi > view.tonic + view.range)
			midi = -1;
		if (midi != highlight) {
			highlight = midi;
			repaint();
		}
	}

	@Override public void mouseDragged(MouseEvent e) {
		int nextMidi = toData1(e.getX(), e.getY());
		if (nextMidi == pressed) return;
		mouseMoved(e);
		sound(false);
		pressed = Math.max(-1, Math.min(view.tonic + view.range, nextMidi));
		sound(true);
	}

	@Override public void mouseReleased(MouseEvent e) {
		sound(false);
	}

	/** Convert screen coordinates to MIDI data1, respecting orientation. */
	private int toData1(int x, int y) {
		if (keyHeight <= 0) return -1;
		int offset;
		if (orientation == Orientation.NOTES_X) {
			offset = (int) (x / keyHeight);
		} else {
			offset = (int) (y / keyHeight);
		}
		int midi = view.tonic + offset;
		if (orientation == Orientation.NOTES_Y) {
			/* In NOTES_Y, high notes are at top (i=0), low notes at bottom */
			midi = (view.tonic + view.range) - offset;
		}
		return midi;
	}

	private void sound(boolean on) {
		if (pressed < 0) return;
		ShortMessage msg = Midi.create(on ? Midi.NOTE_ON : Midi.NOTE_OFF,
			track.getCh(), pressed, (int) (track.getState().getAmp() * 127));
		if (track.getMidiOut() instanceof MidiInstrument)
			JudahMidi.queue(msg, ((MidiInstrument)track.getMidiOut()).getMidiPort());
		else
			track.getMidiOut().send(msg, JudahMidi.ticker());
		if (!on)
			pressed = -1;
	}

	public void setOrientation(Orientation o, int w, int h) {
		orientation = o;
		width = w;
		height = h;
		calculateUnits();
		repaint();
	}
}
