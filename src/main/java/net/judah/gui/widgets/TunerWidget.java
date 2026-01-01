package net.judah.gui.widgets;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import judahzone.api.Note;
import judahzone.api.Tuning;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judahzone.fx.Tuner;

/**
 * GUI-only tuner widget. All pitch detection / analysis has been moved into
 * net.judahzone.fx.TunerBase. This widget is responsible for display and for
 * activating / deactivating the analyzer on the JudahZone.
 *
 * When activated it will call:
 *   ((JudahZone)zone).setTuner(new TunerBase(t -> MainFrame.update(t)));
 * and when deactivated:
 *   zone.setTuner(null)
 *
 * The TunerBase listener will call MainFrame.update(tuning) which will arrive
 * on the EDT as a judahzone.api.Tuning object — those are handled by the
 * surrounding UI update loop which will call TunerKnobs.update(Tuning).
 */
public class TunerWidget extends Box implements Updateable {

    public static final int TUNER_HEIGHT = 49;
    private static final Dimension SLIDER = new Dimension(Size.WIDTH_KNOBS - 130, TUNER_HEIGHT - 4);
    private static final Dimension LBL = new Dimension(80, TUNER_HEIGHT - 12);

    @Getter private final int paramCount = 0; // GUI only
    @Getter private boolean active;
    private final JSlider tuning = new RainbowFader(e -> {/* no-op */});
    private final JButton toggle = new JButton("Tuner");
    boolean hertz = false; // display Midi Label

    private final JudahZone zone;

    public TunerWidget(JudahZone zone) {
        super(BoxLayout.X_AXIS);
        this.zone = zone;

        toggle.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e))
                    hertz = !hertz;
                else
                    setActive(!isActive());
            }
            ;
        });
        toggle.setFont(Gui.BOLD12);
        toggle.setOpaque(true);

        tuning.setOrientation(JSlider.HORIZONTAL);
        tuning.setMajorTickSpacing(50);
        tuning.setMinorTickSpacing(20);
        tuning.setPaintTicks(true);
        tuning.setOpaque(true);
        tuning.setEnabled(false);

        int strut = 15;
        add(Box.createHorizontalStrut(strut));
        add(Gui.resize(toggle, LBL));
        add(Box.createHorizontalStrut(strut));
        add(Gui.resize(tuning, SLIDER));
        add(Box.createHorizontalStrut(strut));
        doLayout();
        update();
    }

    @Override
    public void update() {
        if (!active)
            toggle.setText("Off");
        toggle.setBackground(active ? Pastels.GREEN : null);
    }

    /**
     * Update display from a Tuning produced by TunerBase listener. Called on EDT.
     */
    public void update(Tuning result) {
        if (!active || result == null)
            return;

        float frequency = result.frequency();
        Note note = result.note();
        if (frequency <= 0 || note == null)
            return;

        float diff = result.deviationHz(); // frequency - nearest
        float scaleFactor = calculateScaleFactor(note.octave());
        int sliderValue = 50 + Math.round(diff * scaleFactor);
        tuning.setValue(sliderValue);
        toggle.setText(hertz ? String.valueOf(Math.round(frequency)) : note.toString());
    }

    /** slider less sensitive to frequency diff as octave increases */
    private float calculateScaleFactor(int octave) {
        float baseScaleFactor = 0.8f; // Base scale factor for octave 0
        return (float) (baseScaleFactor * Math.pow(2, octave));
    }

    /**Activate or deactivate the RT tuner analyzer from EDT */
    public void setActive(boolean active) {

        if (this.active == active)
            return;
        this.active = active;
        if (active) {
            // create analyzer and register listener that posts updates via MainFrame.update(...)
            zone.setTuner(new Tuner(tuning -> MainFrame.update(tuning)));
        } else {
            zone.setTuner(null);
        }
		MainFrame.update(this);
    }

}