
package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Box;

import judahzone.api.Tuning;
import judahzone.api.Live.LiveData;
import judahzone.gui.Gui;
import judahzone.util.Recording;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.RMSWidget;
import net.judah.gui.widgets.TunerWidget;


public class TunerKnobs extends KnobPanel {
    private static final Dimension size = new Dimension(WIDTH_KNOBS,
            HEIGHT_KNOBS - (2 + STD_HEIGHT + TunerWidget.TUNER_HEIGHT));

    @Getter private final KnobMode knobMode = KnobMode.Tuner;
    @Getter private final Box title = Box.createHorizontalBox();


    @Getter private final RMSWidget waveform = new RMSWidget(size);

    private final TunerWidget tuner; // GUI-only widget (activates TunerBase on zone)
    private Recording buffer = new Recording();
    private final Dimension mine = new Dimension(WIDTH_KNOBS - 10, HEIGHT_KNOBS - STD_HEIGHT - 1);

    public TunerKnobs(JudahZone judahZone) {
        Gui.resize(this, mine);
        setSize(mine);
        tuner = new TunerWidget(judahZone);
        add(tuner);
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
        g.drawImage(waveform, 0, TunerWidget.TUNER_HEIGHT, null);
    }

    public void process(float[][] buf) {
        buffer.add(buf);
        if (buffer.size() > 1) {
            MainFrame.update(new LiveData(waveform, buffer.getLeft(), buffer.getChannel(1)));
            buffer = new Recording();
        }
    }

    @Override public void update() {
    }

    @Override public void pad1() {
        tuner.setActive(!tuner.isActive());
    }

    /** Called from EDT when a Tuning arrives (see MainFrame.update handling). */
    public void update(Tuning tuning) {
        tuner.update(tuning);
    }

    public void turnOff() {
    	tuner.setActive(false);
    }

    public void turnOn() {
    	tuner.setActive(true);
    }

}