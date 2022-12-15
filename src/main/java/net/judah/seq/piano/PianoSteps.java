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
import lombok.Setter;
import net.judah.gui.Pastels;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.seq.Bar;
import net.judah.seq.MidiSize;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;
import net.judah.seq.Steps;

public class PianoSteps extends Steps implements MidiSize, MouseMotionListener, MouseListener {

	static final int OFFSET = STEP_WIDTH / 2 - 5;
	private final JudahClock clock;
	private final MidiTrack track;
	private final MidiView view;
	private final int width, height;
	private int highlight = -1;

	@Getter @Setter private int start = 0;
	@Getter private int total;
	@Getter private int unit;
	private final ArrayList<Integer> playing = new ArrayList<>();
	
	public PianoSteps(Rectangle r, MidiTrack track, MidiView view) {
		this.track = track;
		this.view = view;
		this.clock = track.getClock();
		this.width = r.width;
		this.height = r.height;
		total = 2 * clock.getSteps();
		unit = height / total;
		setBounds(r);
		addMouseMotionListener(this);
		addMouseListener(this);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawRect(0, 0, width, height);

		int steps = clock.getSteps();
		int div = clock.getSubdivision();
		total = 2 * clock.getSteps();
		unit = height / total;
		
		int count = start;
		
		int y;
		for (int i = 0; i < 2 * steps; i++) {
			y = i * unit;

			if (highlight == i) {
				g.setColor(isBeat(i) ? Pastels.YELLOW.darker() : Pastels.YELLOW);
				g.fillRect(1, y + 2, width - 2, unit - 3);
				g.setColor(Color.BLACK);
			}
			
			if (isBeat(i)) { 
				int beat = (1 + count / 4);
				if (beat > 4) beat -= 4;
				if (i != highlight) {
					g.setColor(Pastels.FADED);
					g.fillRect(0, y, width, unit);
				}
				g.setColor(Color.BLACK);
				g.drawString("" + beat, OFFSET, y + unit - 3);
			}
			else if (count % steps % div == 2) {
				g.drawString("+", OFFSET, y + unit - 3);
			}
			
			g.drawLine(0, y + unit, width, y + unit);

			if (++count == steps)
				count = 0;
		}
		
		
	}

	public boolean isBeat(int row) {
		return (row + start) % clock.getSteps() % clock.getSubdivision() == 0;
	}

	public boolean isBar(int row) {
		return (row + start) % clock.getSteps() == 0;
	}

	public void highlight(Point p) {
		int replace = p == null ? -1 : (int) ( p.y / (float)height * clock.getSteps() * 2);
		if (replace == highlight)
			return;
		highlight = replace;
		repaint();
	}

	private void send(int cmd, int data1) {
		track.getMidiOut().send(Midi.create(cmd, track.getCh(), 
				data1, view.getVelocity().getValue()), JudahMidi.ticker());
	}
	
	/** play notes for given step */
	@Override public void mousePressed(MouseEvent e) {
		notesForPoint(e.getPoint());
		for (Integer data1 : playing)
			send(NOTE_ON, data1);
	}
	
	public static int toStep(int y, int total) {
		return (int) (total * (y / (float)BOUNDS_GRID.height));
	}

	private void notesForPoint(Point point) {
		playing.clear();
		int step = toStep(point.y, total);
		if (step < 0 || step > total)
			return;
		float steps = clock.getSteps() * 2;
		long start = (long) (2 * track.getTicks() * step / steps);
		step++;
		long end = (long) (2 * track.getTicks() * step / steps);
		Bar target = null;
		if (start < track.getTicks()) 
			target = view.getSnippet().one;
		else {
			target = view.getSnippet().two;
			start -= track.getTicks();
			end -= track.getTicks();
		}
		for (MidiEvent e : target) {
			if (e.getTick() <= start)
				if (((ShortMessage)e.getMessage()).getCommand() == NOTE_ON) {
					if (e.getTick() < end)
						playing.add(((ShortMessage)e.getMessage()).getData1());
				}
				else if (((ShortMessage)e.getMessage()).getCommand() == NOTE_OFF)
					playing.remove((Integer)((ShortMessage)e.getMessage()).getData1());
		}
	}

	/** play notes for new given step */
	@Override public void mouseDragged(MouseEvent e) {	
		 // TODO merge copy to playing   send new note ons/offs 
		 highlight(e.getPoint());
		 ArrayList<Integer> copy = new ArrayList<>();
		 playing.forEach(x->copy.add(x));
		 notesForPoint(e.getPoint());
		 for (int i : copy) 
			 if (playing.contains(i) == false)
				 send(NOTE_OFF, i);
		 for (int i : playing) 
			 if (copy.contains(i) == false)
				 send(NOTE_ON, i);
	}
	
	/** stop playing notes */
	@Override public void mouseReleased(MouseEvent e) { 
		for (Integer data1 : playing) // create and send note off
			send(NOTE_OFF, data1);
	}

	@Override public void mouseExited(MouseEvent e) { 
		highlight(null);
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		highlight(e.getPoint());
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	
}
