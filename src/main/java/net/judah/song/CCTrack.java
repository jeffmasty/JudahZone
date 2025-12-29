package net.judah.song;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import judahzone.api.Signature;
import net.judah.gui.Size;
import net.judah.midi.JudahClock;
import net.judah.seq.Steps;
import net.judah.seq.automation.Automation;
import net.judah.seq.automation.CCPopup;
import net.judah.seq.track.MidiTrack;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Pastels;

public class CCTrack extends Steps {

	private static final int width = Overview.TRACK.width - 18;
	private static final int height = Size.STD_HEIGHT;
	private static final Dimension SIZE = new Dimension(Overview.TRACK.width, height);
	private static final int HALF = height / 2 + 4;
	private static final Color HIGHLIGHT = Pastels.GREEN;
	private static final Color PEN = Color.DARK_GRAY;

	static final int OFFSET = 6;
	private int highlight = -1;

	private final JudahClock clock;
	private final CCPopup cc;

	private float unit;
	private int xunit;

	public CCTrack(MidiTrack t, Automation auto) {
		super(t);
		this.clock = track.getClock();
		this.cc = new CCPopup(track, this, true, auto);
		Gui.resize(this, SIZE);
		addMouseListener(this);
		setLayout(null);
		timeSig(clock.getTimeSig());
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		int[] ccs = cc.populate(track.getLeft(), track.getLeft() + track.getWindow());
		g.drawRect(0, 0, width, height);
		int beats = clock.getTimeSig().beats;
		int steps = clock.getSteps();
		int div = clock.getSubdivision();
		int count = 0;
		int x;

		for (int i = 0; i < 2 * steps; i++) {
			x = (int)(i * unit);
			g.setColor(PEN);
			if (highlight == i) {
				g.setColor(HIGHLIGHT);
				g.fillRect(x + 1, 1, xunit - 2 , height - 2);
				if (count % steps % div == 2) {
					g.setColor(PEN);
					g.drawString("+", x + OFFSET, HALF);
				}
			}
			else if (cc.getProg(i) != null)
				ccPad(g, x, 1, Pastels.PROGCHANGE);
			else if (ccs[i] > 0)
				ccPad(g, x, ccs[i], Pastels.CC);

			else if (isBeat(i)) {
				int beat = (1 + count / div);
				if (beat > beats)
					beat -= beats;
				if (highlight != i) {
					g.setColor(Pastels.FADED);
					g.fillRect(x, 0, xunit, height);
				}
				g.setColor(PEN);
				g.drawString("" + beat, x + OFFSET, HALF);
			}
			else if (count % steps % div == 2)
				g.drawString("+", x + OFFSET, HALF);

			g.drawLine(x, 0, x, height); // step grid // one more line?

			if (++count == steps)
				count = 0;
		}
		g.drawLine(width, 0, width, height);
	}

	private void ccPad(Graphics g, int x, int count, Color c) {
        g.setColor(c);
    	g.fillRect(x + 1, 1, xunit - 2, height - 2);
    	if (count > 1) {
			g.setColor(Color.BLACK);
			g.drawString("" + count, x + OFFSET, HALF);
    	}
	}


	public boolean isBeat(int i) {
		return i % clock.getSteps() % clock.getSubdivision() == 0;
	}

	public int toStep(int x) {
		return (int) (2 * clock.getSteps() * (x / (float)width));
	}

	public void step(int step) {
		if (track.isActive() == false)
			return;
		int actual = clock.isEven() ? step : step + clock.getSteps();
		if (highlight == actual)
			return;
		highlight = actual;
		repaint();
	}

	@Override public void mouseReleased(MouseEvent e) {
		cc.popup(e, toStep(e.getX()));
	}

	@Override
	public void timeSig(Signature sig) {
		unit = width / (sig.steps * 2f);
		xunit = (int) unit;
		repaint();
	}

}
