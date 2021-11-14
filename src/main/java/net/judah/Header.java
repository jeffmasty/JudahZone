package net.judah;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.judah.clock.JudahClock;
import net.judah.util.Console;
import net.judah.util.Tuner;

public class Header extends JPanel {

    public Header(Rectangle bounds) {
        setBounds(bounds);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JPanel clock = JudahClock.getInstance().getGui();
        JTextField input = Console.getInstance().getInput();
        JScrollPane output = Console.getInstance().getScroller();
        JPanel console = new JPanel();

        Dimension clockSz = new Dimension(WIDTH_CLOCK, bounds.height - 10);
        clock.setPreferredSize(clockSz);
        clock.setMaximumSize(clockSz);

        Dimension consoleSz = new Dimension(WIDTH_SONG - WIDTH_CLOCK - WIDTH_TUNER, bounds.height);
        console.setPreferredSize(consoleSz);
        input.setPreferredSize(new Dimension(consoleSz.width - 10 - WIDTH_TUNER, STD_HEIGHT));
        output.setPreferredSize(new Dimension(consoleSz.width - 2, bounds.height - 38));

        JPanel tuner = new Tuner();
        tuner.setPreferredSize(new Dimension(WIDTH_TUNER, STD_HEIGHT));

        JPanel inputTuner = new JPanel();
        inputTuner.setLayout(new BoxLayout(inputTuner, BoxLayout.X_AXIS));
        inputTuner.add(input);
        inputTuner.add(tuner);

        console.add(output);
        console.add(inputTuner);

        add(clock);
        JPanel loops = new JPanel();
        loops.setPreferredSize(new Dimension(WIDTH_TUNER, HEIGHT_CONSOLE));
        add(loops);
        add(console);
        doLayout();
    }

}
