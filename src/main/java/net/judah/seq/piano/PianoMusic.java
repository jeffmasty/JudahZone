package net.judah.seq.piano;

import static net.judah.seq.MidiTools.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.sound.midi.ShortMessage;

import lombok.Getter;
import net.judah.gui.Pastels;
import net.judah.midi.JudahClock;
import net.judah.seq.*;
import net.judah.seq.beatbox.BeatsSize;

public class PianoMusic extends MusicGrid implements BeatsSize {

	private final MidiTrack track;
	private final PianoSteps steps;
	private final JudahClock clock;
	private final Piano piano;

	private final int width, height;
	@Getter private final Pianist pianist;
	@Getter private final Measure scroll;
	
	public PianoMusic(Rectangle r, MidiView view, PianoSteps currentBeat, Piano roll, MidiTab tab) {
		setBounds(r);
		width = r.width;
		height = r.height;
		this.track = view.getTrack();
		this.clock = track.getClock();
		this.scroll = view.getScroll();
		this.steps = currentBeat;
		this.piano = roll;
		pianist = new Pianist(piano, this, view , tab);
		addMouseListener(pianist);
		addMouseMotionListener(pianist);
		addMouseWheelListener(pianist);
	}
	
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int count = clock.getSteps();
		float unit = height / (2f * count);
		float ratio = height / (2f * track.getBarTicks());
		
		g.setColor(Pastels.FADED);
		int x; // columns (notes)
		for (int i = 0; i < piano.total(); i++) {
			x = i * KEY_WIDTH;
			if (piano.isLabelC(i)) {
				g.setColor(Pastels.BLUE);
				g.fillRect(x, 0, (int)unit, height);
				g.setColor(Pastels.FADED);
			}
			g.drawLine(x, 0, x, height);
		}

		int y; // rows (steps)
		for (int i = 0; i < 2 * count; i++) {
			y = (int) (i * unit);
			if (steps.isBeat(i))
				g.fillRect(0, y, width, (int) unit);
			else 
				g.drawLine(0, (int)(y + unit), width, (int)(y + unit));
			if (steps.isBar(i)) {
				g.setColor(Color.GRAY);
				g.drawLine(0, y, width, y);
				g.setColor(Pastels.FADED);
			}			
		}

		scroll.populate();
		int yheight;
		for (MidiPair p : scroll) {
			
			if (p.getOn().getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)p.getOn().getMessage();
			x = KEY_WIDTH * Piano.data1ToGrid(s.getData1());
			y = (int) (p.getOn().getTick() * ratio);
			
			yheight = (int) ((p.getOff().getTick() - p.getOn().getTick()) * ratio);
			if (pianist.getSelected().isNoteSelected(p.getOn().getTick(), s.getData1()))
				g.setColor(highlightColor(s.getData2()));
			else 
				g.setColor(velocityColor(s.getData2()));
			g.fill3DRect(x, y, KEY_WIDTH, yheight, true);
		}
		g.setColor(Pastels.FADED);
		g.drawRect(0, 0, width-1, height - 1); // border
	}


	@Override
	public Musician getMusician() {
		return pianist;
	}

	
}
