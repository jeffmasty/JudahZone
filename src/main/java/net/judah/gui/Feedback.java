package net.judah.gui;

import static judahzone.util.Constants.NL;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import judahzone.util.RTLogger;
import judahzone.util.RTLogger.Participant;
import lombok.Getter;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Pastels;

/** GUI wrapper for LogService; registers/unregisters itself as a listener */
public class Feedback extends KnobPanel {

    @Getter private final JLabel ticker = new JLabel("", JLabel.LEFT);
    private final JScrollPane scroller = new JScrollPane();
    private final JTextArea textarea = new JTextArea(3, 28);
    private final Consumer<RTLogger.LogEvent> listener = this::handleLogEvent;

    @Getter private static final ArrayList<Participant> participants = new ArrayList<>();

    public Feedback(final Dimension size) {
        super();
        textarea.setEditable(false);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        EventQueue.invokeLater(() -> {
            Gui.resize(scroller, size);
            textarea.setForeground(Color.BLUE.darker());
            ticker.setOpaque(true);
            ticker.setForeground(Color.BLUE.darker());
            ticker.setBackground(Pastels.BUTTONS);
            ticker.setBorder(Gui.SUBTLE);
            scroller.setViewportView(textarea);
            add(scroller);
        });

        // register to receive log events
        RTLogger.registerConsumer(listener);
    }

    /** handle a LogEvent on any thread; marshall UI changes to EDT */
    private void handleLogEvent(RTLogger.LogEvent ev) {
        EventQueue.invokeLater(() -> {
            String line = ev.source() + (ev.warn() ? " WARN: " : ": ") + ev.message() + NL;
            ticker.setText(ev.message());
            textarea.append(line);
            textarea.setCaretPosition(textarea.getDocument().getLength());
            scroller.getHorizontalScrollBar().setValue(0);
            scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
            this.invalidate();
            scroller.repaint();
        });
    }

    /** output to console (not for realtime) */
    public void addText(String in) {
        RTLogger.debug(this, in);
        ticker.setText(in);
        if (in == null)
            in = "null" + NL;
        if (false == in.endsWith(NL))
            in += NL;

        textarea.append(new String(in));
        textarea.setCaretPosition(textarea.getDocument().getLength());
        scroller.getHorizontalScrollBar().setValue(0);
        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
        invalidate();
        scroller.repaint();
    }


    /** when the GUI disposed, avoid leaks */
    public void dispose() {
        RTLogger.unregisterConsumer(listener);
    }

    // keep KnobPanel contract if needed
    @Override
	public KnobMode getKnobMode() { return KnobMode.Log; }
    @Override
	public JComponent getTitle() { return Gui.wrap(new JLabel("")); }
}