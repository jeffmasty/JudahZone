package net.judah.beatbox;

import static net.judah.util.Size.*;

import java.awt.Rectangle;

import javax.swing.JDesktopPane;

import lombok.Getter;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.util.Console;
import net.judah.util.Pastels;

public class BeatsView extends JDesktopPane implements TimeListener, Pastels {

    private static final int height = HEIGHT_TABS - 30;
    private static final Rectangle BTN_BOUNDS = new Rectangle(0, 0, WIDTH_CLOCK, height);
    private static final Rectangle KIT_BOUNDS = new Rectangle(WIDTH_CLOCK, 0, WIDTH_TUNER, height);
    private static final Rectangle GRID_BOUNDS = new Rectangle(WIDTH_TUNER + WIDTH_CLOCK, 0,
            WIDTH_SONG - WIDTH_TUNER - WIDTH_CLOCK, height);

    @Getter private static BeatsView instance;
    private final JudahClock clock = JudahClock.getInstance();

    @Getter private final Buttons buttons;
    @Getter private final GridView grid2;
    @Getter private KitPanel kitPanel;

    public BeatsView() {

        setLayout(null);
        BeatBox sequencer = clock.getSequencer(9);
        buttons = new Buttons(sequencer);
        grid2 = new GridView(GRID_BOUNDS);

        buttons.setBounds(BTN_BOUNDS);
        grid2.setBounds(GRID_BOUNDS);

        add(grid2);
        add(buttons);

        initialize(sequencer);

        clock.addListener(this);
        instance = this;
    }

    public void initialize(BeatBox sequencer) {
        new Thread(() -> {
            Console.info("Initialize channel " + sequencer.getMidiChannel() + " sequence");
            buttons.setSequencer(sequencer);

            if (kitPanel != null)
                remove(kitPanel);
            kitPanel = sequencer.getCurrent().createTracks();
            kitPanel.setBounds(KIT_BOUNDS);
            add(kitPanel);

            doLayout();
            grid2.repaint();


        }).start();
    }

    @Override
    public void update(Property prop, Object value) {
        // TODO
    }

    /**@return currently viewed sequencer/channel */
    public BeatBox getSequencer() {
        return JudahClock.getInstance().getSequencer(
                buttons.getChannelCombo().getSelectedIndex());
    }

    /**@return currently viewed pattern data*/
    public Grid getCurrent() {
        return JudahClock.getInstance().getSequencer(
                buttons.getChannelCombo().getSelectedIndex()).getCurrent();
    }

    public void updateKit(BeatBox seq, KitPanel tracks) {
        new Thread( () -> {
            grid2.repaint();
            remove(kitPanel);
            kitPanel = tracks;
            kitPanel.setBounds(KIT_BOUNDS);
            add(kitPanel);
            buttons.setSequencer(seq);
        }).start();
    }

    public void update() {
        grid2.repaint();
        buttons.update();
    }

    /**@return the default step count in order to gate notes */
    public int getNoteOff() {
        return (int)buttons.getGate().getSelectedItem();
    }
}
