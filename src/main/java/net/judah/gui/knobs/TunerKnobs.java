package net.judah.gui.knobs;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Box;

import judahzone.api.Tuning;
import judahzone.fx.analysis.Waveform;
import judahzone.gui.Gui;
import judahzone.util.AudioMetrics.RMS;
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

    private final JudahZone zone;
    @Getter private final RMSWidget waveform = new RMSWidget(size);
    private final Waveform analyzer;
    private final TunerWidget tuner; // GUI-only widget (activates TunerBase on zone)

    private final Dimension mine = new Dimension(WIDTH_KNOBS - 10, HEIGHT_KNOBS - STD_HEIGHT - 1);

    // track analyzer registration so we can re-register when the panel is shown again
    private volatile boolean analyzerRegistered = false;

    public TunerKnobs(JudahZone judahZone) {
        this.zone = judahZone;
        Gui.resize(this, mine);
        setSize(mine);
        analyzer = new Waveform(rms -> MainFrame.update(rms));

        tuner = new TunerWidget(judahZone);
        add(tuner);

        // turnOn:
        zone.registerAnalyzer(analyzer);
        analyzerRegistered = true;
        tuner.setActive(true);
        repaint();
    }

    /** Ensure analyzer is registered and tuner active */
    public void turnOn() {
        if (!analyzerRegistered) {
            zone.registerAnalyzer(analyzer);
            analyzerRegistered = true;
        }
        tuner.setActive(true);
    }

    /** Deactivate tuner and unregister analyzer */
    public void turnOff() {
        tuner.setActive(false);
        if (analyzerRegistered) {
            zone.unregisterAnalyzer(analyzer);
            analyzerRegistered = false;
        }
    }

    @Override public boolean doKnob(int idx, int value) {
        if (idx < 0 || idx > 7)
            return false;

        Threads.execute(()->{
            float floater = value * 0.01f;
            switch (idx) {
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

    @Override public void update() {
    }

    @Override public void pad1() {
        tuner.setActive(!tuner.isActive());
    }

    public void update(Tuning tuning) {
        tuner.update(tuning);
    }

    public void update(RMS rms) {
        waveform.accept(rms);
        repaint();
    }

}
