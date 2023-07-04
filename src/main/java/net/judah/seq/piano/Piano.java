package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.sound.midi.ShortMessage;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Pastels;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.MidiTab;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.track.MidiTrack;

public class Piano extends JPanel implements BeatsSize, MouseListener, MouseMotionListener {

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
		sound(false);
		pressed = tab.getCurrent().getGrid().toData1(e.getPoint());
		sound(true);
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
		if (track.getMidiOut() == JudahZone.getFluid()) 
			JudahMidi.queue(msg, JudahZone.getFluid().getMidiPort().getPort());
		else 
			track.getMidiOut().send(msg, JudahMidi.ticker());
		if (!on)
			pressed = -1;
	}
	
	public boolean setOctave(boolean up) {
		octave += up ? 1 : -1;
		if (octave < 1)
			octave = 8;
		if (octave > 8)
			octave = 8;
		return true;
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
