package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Box;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.scope.Live.LiveData;
import net.judah.gui.widgets.RMSWidget;
import net.judah.gui.widgets.Tuner;
import net.judah.gui.widgets.Tuner.Tuning;
import net.judah.util.Recording;
import net.judah.util.Threads;


public class TunerKnobs extends KnobPanel {
	private static final Dimension size = new Dimension(WIDTH_KNOBS,
			HEIGHT_KNOBS - (2 + STD_HEIGHT + Tuner.TUNER_HEIGHT));

	@Getter private static TunerKnobs instance = new TunerKnobs();
	@Getter private final KnobMode knobMode = KnobMode.Tuner;
    @Getter private final Box title = Box.createHorizontalBox();

    private final RMSWidget waveform = new RMSWidget(size);

	private final Tuner tuner = new Tuner();
	private Recording buffer = new Recording();
	private final Dimension mine = new Dimension(WIDTH_KNOBS - 10, HEIGHT_KNOBS - STD_HEIGHT - 1);

	private TunerKnobs() {
		Gui.resize(this, mine);
		setSize(mine);
		add(tuner);
		tuner.setActive(true);
		repaint();
	}

	@Override public boolean doKnob(int idx, int value) {
		if (idx < 0 || idx > 7)
			return false;

		Threads.execute(()->{
			float floater = value * 0.01f;
			switch (idx) {
			//  TODO 0 - 4 selected gains
			//  case 5 -> waveform.setXScale(1 - floater);
			    case 6 -> waveform.setYScale(floater);
			    case 7 -> waveform.setIntensity(floater);
			    default -> { return; }
			}
			repaint();
		});
		return true;
	}

	@Override public void paint(Graphics g) {
		super.paint(g); // tuner
        g.drawImage(waveform, 0, Tuner.TUNER_HEIGHT, null);
	}

	public void process(float[][] buf) {
		buffer.add(buf);
		if (tuner.isActive())
			MainFrame.update(new Tuning(tuner, buf));
		if (buffer.size() > 1) {
			MainFrame.update(new LiveData(waveform, buffer));
			buffer = new Recording();
		}
	}

	@Override public void update() {
	}

	@Override public void pad1() {
		tuner.setActive(!tuner.isActive());
	}


}
