package net.judah.seq.piano;

import static net.judah.seq.MidiTools.velocityColor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.midi.JudahClock;
import net.judah.seq.MidiSize;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;
import net.judah.seq.Note;
import net.judah.seq.Snippet;

public class PianoGrid extends JPanel implements MidiSize {

	private final MidiTrack track;
	private final PianoSteps steps;
	private final JudahClock clock;
	private final Piano piano;

	private final int width, height;
	@Getter private final Pianist pianist;
	@Getter private final Snippet notes;
	
	public PianoGrid(Rectangle r, MidiView view, PianoSteps currentBeat, Piano roll) {
		setBounds(r);
		width = r.width;
		height = r.height;
		this.track = view.getTrack();
		this.clock = track.getClock();
		this.notes = view.getSnippet();
		this.steps = currentBeat;
		this.piano = roll;
		pianist = new Pianist(track, piano, this, steps, view);
		addMouseListener(pianist);
		addMouseMotionListener(pianist);
		addMouseWheelListener(pianist);
	}
	
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int count = clock.getSteps();
		int unit = steps.getUnit();
		g.setColor(Pastels.FADED);
		int x; // columns (notes)
		for (int i = 0; i < piano.total(); i++) {
			x = i * KEY_WIDTH;
			if (piano.isLabelC(i)) {
				g.setColor(Pastels.BLUE);
				g.fillRect(x, 0, unit, height);
				g.setColor(Pastels.FADED);
			}
			g.drawLine(x, 0, x, height);
		}

		int y; // rows (steps)
		for (int i = 0; i < 2 * count; i++) {
			y = i * unit;
			if (steps.isBeat(i))
				g.fillRect(0, y, width, unit);
			else 
				g.drawLine(0, y + unit, width, y + unit);
			if (steps.isBar(i)) {
				g.setColor(Color.GRAY);
				g.drawLine(0, y, width, y);
				g.setColor(Pastels.FADED);
			}			
		}

		notes.setStartref((long) (steps.getStart() * track.getResolution() / (float)clock.getSubdivision()));
		track.publishView(notes);

		Note note = notes.poll();
		while (note != null) {
			int yon = (int) (note.onFactor * height);
			int yheight = (int) ((note.offFactor - note.onFactor) * height);
			if (yon >=0 && yheight >= 0) {
				g.setColor(velocityColor(note.getData2()));
				g.fill3DRect((note.getData1() - NOTE_OFFSET) * KEY_WIDTH, yon, KEY_WIDTH, yheight, true);
			}
			note = notes.poll();
		}
		g.setColor(Pastels.FADED);
		g.drawRect(0, 0, width-1, height-2); // border

	}

	public boolean keyPressed(KeyEvent e) {
		return piano.keyPressed(e.getKeyCode());
	}

	public boolean keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
			pianist.deleteKey(); return true;
		default: return piano.keyReleased(e.getKeyCode());
		
		}
	}

	
}
