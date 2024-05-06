package net.judah.seq.piano;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.api.Signature;
import net.judah.gui.Pastels;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiView;
import net.judah.seq.Steps;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.seq.track.MidiTrack;

public class PianoSteps extends Steps implements BeatsSize, MouseMotionListener, MouseListener {

	static final int OFFSET = STEP_WIDTH / 2 - 5;
	private final MidiTrack track;
	private final MidiView view;
	private final JudahClock clock;
	private final int width, height;
	private int highlight = -1;

	@Getter private int start = 0;
	@Getter private float total;
	@Getter private float unit;
	
	private Integer on, off;
	
	public PianoSteps(Rectangle r, MidiView view) {
		this.view = view;
		this.track = view.getTrack();
		this.clock = track.getClock();
		this.width = r.width;
		this.height = r.height;
		setBounds(r);
		timeSig(view.getTrack().getClock().getTimeSig());
		addMouseMotionListener(this);
		addMouseListener(this);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawRect(0, 0, width, height);
		int beats = clock.getTimeSig().beats;
		int steps = clock.getSteps();
		int div = clock.getSubdivision();
		int count = start;
		
		int y;
		for (int i = 0; i < 2 * steps; i++) {
			y = (int)(i * unit);

			if (highlight == i) {
				g.setColor(isBeat(i) ? Pastels.YELLOW.darker() : Pastels.YELLOW);
				g.fillRect(1, y + 1, width - 2, (int) unit - 3);
				g.setColor(Color.BLACK);
			}
			
			if (isBeat(i)) { 
				int beat = (1 + count / div);
				if (beat > beats) beat -= beats;
				if (i != highlight) {
					g.setColor(Pastels.FADED);
					g.fillRect(0, y, width, (int)unit);
				}
				g.setColor(Color.BLACK);
				g.drawString("" + beat, OFFSET, y + (int)unit - 3);
			}
			else if (count % steps % div == 2) {
				g.drawString("+", OFFSET, y + (int)unit - 3);
			}
			
			g.drawLine(0, y + (int)unit, width, y + (int)unit);

			if (++count == steps)
				count = 0;
		}
	}

//	@Override not used
	public void setStart(int start) {
		this.start = start;
		repaint();
	}

	public boolean isBeat(int row) {
		return (row + start) % clock.getSteps() % clock.getSubdivision() == 0;
	}

	public boolean isBar(int row) {
		return (row + start) % clock.getSteps() == 0;
	}

	@Override
	public void highlight(Point p) {
		int replace = p == null ? -1 : (int) ( p.y / (float)height * clock.getSteps() * 2);
		if (replace == highlight)
			return;
		highlight = replace;
		repaint();
	}

	public int toStep(int y) {
		return (int) (total * (y / (float)height));
	}

	/** play notes for given step */
	@Override public void mousePressed(MouseEvent e) {
		on = toStep(e.getPoint().y);
	}
	
	/** play notes for new given step */
	@Override public void mouseDragged(MouseEvent e) {	
		 off = toStep(e.getPoint().y) + 1;
	}
	
	/** add actives to track*/
	@Override public void mouseReleased(MouseEvent e) { 
		off = toStep(e.getPoint().y) + 1;
		if (track.getActives().size() == 0) 
			return;
		
		int step = track.getResolution() / clock.getSubdivision();
		int ch = track.getCh();
		int data2 = (int) (track.getAmp() * 127f);
		long begin = track.getLeft() + on * step;
		long end = track.getLeft() + off * step - 1;
		on = off = null;
		ArrayList<MidiPair> notes = new ArrayList<>();
		for (ShortMessage m : track.getActives()) {
			notes.add(new MidiPair(
					new MidiEvent(Midi.create(NOTE_ON, ch, m.getData1(), data2), begin),
					new MidiEvent(Midi.create(NOTE_OFF, ch, m.getData1(), 127), end)));
		}
		view.getGrid().push(new Edit(Type.NEW, notes));
	}

	@Override public void mouseExited(MouseEvent e) { 
		highlight(null);
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		if (on != null)
			highlight(e.getPoint());
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }

	@Override
	public void timeSig(Signature sig) {
		total = 2 * sig.steps;
		unit = height / total;
	}
	
}
