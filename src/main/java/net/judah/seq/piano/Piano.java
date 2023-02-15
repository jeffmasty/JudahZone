package net.judah.seq.piano;

import static java.awt.event.KeyEvent.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.beatbox.BeatsSize;

public class Piano extends JPanel implements BeatsSize, MouseListener, MouseWheelListener, MouseMotionListener {

	private final MidiTab tab;
	private final MidiTrack track;
	private final int width, height;
	private int highlight = -1;
	private int pressed;
	@Getter private int octave = 4;
	
	public Piano(Rectangle r, MidiTrack t, MidiTab tab) {
		this.tab = tab;
		this.track = t;
		width = r.width;
		height = r.height;
		setBounds(r);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}
	
	/** label Octave C
	 * @return true if idx is integral of 12, ignore first and last Octave*/
	public boolean isLabelC(int idx) {
		return idx % 12 == 0 && idx != 0; 
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		List<Integer> actives = track.getMidiOut().getActives();
		g.setColor(Color.GRAY);
		g.drawRect(0, 0, width, height);
		int x;
		for (int i = 0; i < PianoBox.VISIBLE_NOTES; i++) {
			x = i * KEY_WIDTH;
			g.drawRect(x, 0, KEY_WIDTH, KEY_HEIGHT);
			if (actives.contains(gridToData1(i))) 
				rect(g, Pastels.GREEN, x);
			else if (highlight == i) {
				if (BLACK_KEYS.contains(i % 12))
					rect(g, Pastels.YELLOW.darker(), x);
				else
					rect(g, Pastels.YELLOW, x);
			}
			else if (BLACK_KEYS.contains(i % 12))
				g.fillRect(x, 0, KEY_WIDTH, KEY_HEIGHT);
			else 
				rect(g, Color.WHITE, x);
			
			if (isLabelC(i))
				g.drawString("" + i/12, x + 2, 18);
		}
	}

	private void rect(Graphics g, Color c, int x) {
		g.setColor(c);
		g.fillRect(x + 1, 1, KEY_WIDTH - 1, KEY_HEIGHT - 1);
		g.setColor(Color.GRAY);
	}
	
	/**Convert x-axis note to midi note*/
	public static int gridToData1(int idx) {
			return idx + NOTE_OFFSET;
	}
	/**Convert midi note to x-axis note */
	public static int data1ToGrid(int midi) {
		return midi - NOTE_OFFSET;
	}
	
	public void highlight(int data1) {
		int replace = data1 < 0 ? -1 : data1ToGrid(data1);
		if (highlight == replace)
			return;
		highlight = replace;
		repaint();
	}

	//////  MOUSE  //////  
	
	@Override public void mousePressed(MouseEvent e) {
		off();
		pressed = tab.getCurrent().getGrid().toData1(e.getPoint());
		on();
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {
		highlight = e.getX() / BeatsSize.KEY_WIDTH;  
		repaint();
	}
	
	@Override public void mouseDragged(MouseEvent e) {
		int next = tab.getCurrent().getGrid().toData1(e.getPoint());
		if (next == pressed) return;
		mouseMoved(e);
		off();
		pressed = next;
		on();
	}
	@Override public void mouseReleased(MouseEvent e) {
		off();
	}

	private void on() {
		if (pressed < 0) return;
		track.getMidiOut().send(Midi.create(Midi.NOTE_ON, track.getCh(), pressed, 99), JudahMidi.ticker());
	}
	
	private boolean off() {
		if (pressed < 0) return false;
		track.getMidiOut().send(Midi.create(Midi.NOTE_OFF, track.getCh(), pressed, 99), JudahMidi.ticker());
		pressed = -1;
		return true;
	}

	/**@return Z to COMMA keys are white keys, and black keys, up to 12, no match = -1*/
	public int chromaticKeyboard(final int keycode) {
		switch(keycode) {
			case VK_Z: return 0; // low C
			case VK_S: return 1; 
			case VK_X: return 2;
			case VK_D: return 3;
			case VK_C: return 4;
			case VK_V: return 5; // F
			case VK_G : return 6;
			case VK_B : return 7; // G
			case VK_H: return 8; 
			case VK_N: return 9;
			case VK_J: return 10;
			case VK_M: return 11;
			case VK_COMMA: return 12;// high C
			
			default: return -1;
		}
	}
	
	public boolean setOctave(boolean up) {
		octave += up ? 1 : -1;
		if (octave < 1)
			octave = 8;
		if (octave > 8)
			octave = 8;
		return true;
	}
	
	public boolean keyPressed(final int keycode) { 
		int note = chromaticKeyboard(keycode);
		if (note < 0) {
			if (keycode == VK_UP) 
				return setOctave(true); 
			else if (keycode == VK_DOWN)
				return setOctave(false);
			else 
				return false;
		}
		track.getMidiOut().send(Midi.create(Midi.NOTE_ON, track.getCh(), note + (octave * 12), 99), JudahMidi.ticker());
		return true;
	}
	public boolean keyReleased(int keycode) {
		int note = chromaticKeyboard(keycode);
		if (note < 0)
			return false;
		track.getMidiOut().send(Midi.create(Midi.NOTE_OFF, track.getCh(), note + (octave * 12), 99), JudahMidi.ticker());
		return true;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent wheel) {
// 		boolean up = wheel.getPreciseWheelRotation() < 0;
//		int velocity = track.getVelocity() * 100 + (up ? 5 : -1); // ??5
//		if (velocity < 0)
//			velocity = 0;
//		if (velocity > 127)
//			velocity = 127;
//		track.setVelocity(velocity); // velocity converted to float
//		RTLogger.log(this, "Track velocity: " + velocity);
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
