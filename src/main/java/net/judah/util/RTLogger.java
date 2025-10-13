package net.judah.util;

import static net.judah.util.Constants.NL;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Level;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;

@Log4j
/** LinkedBlockingQueue to capture log statements and post them off the real-time thread */
public class RTLogger extends KnobPanel {

    public static interface Participant { void process(String[] input); }
    @Getter @Setter private static Level level = Level.INFO;
    private record Log(String clazz, String msg, boolean warn) { }
    /** Status bar, last line of log */
    @Getter private static final JLabel ticker = new JLabel("", JLabel.LEFT);
    @Getter private static final ArrayList<Participant> participants = new ArrayList<>();
    private static final JScrollPane scroller = new JScrollPane();
    private static final JTextArea textarea = new JTextArea(3, 28);

    private static final BlockingQueue<Log> debugQueue = new LinkedBlockingQueue<>(1024);
	public static final RTLogger instance = new RTLogger(Size.KNOB_PANEL);

    public static void log(Object o, String msg) {
        debugQueue.offer(new Log(o instanceof String
        		? o.toString() : o.getClass().getSimpleName(), msg, false));
    }
    public static void log(Class<?> c, String msg) {
    	log(c.getSimpleName(), msg);
    }

    public static void warn(Object o, String msg) {
        debugQueue.offer(new Log(o instanceof String
        		? o.toString() : o.getClass().getSimpleName(), msg, true));
    }

    public static void warn(Class<?> c, String msg) {
    	warn(c.getSimpleName(), msg);
    }

    public static void warn(Object o, Throwable e) {
        warn(o, e.getLocalizedMessage());
        e.printStackTrace();
    }

    @Getter private final KnobMode knobMode = KnobMode.Log;
    @Getter private final JComponent title = Gui.wrap(new JLabel(""));

    private RTLogger(Dimension size) {
    	textarea.setEditable(false);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    	EventQueue.invokeLater(() -> {
        	Gui.resize(scroller, size);
        	Gui.resize(ticker, new Dimension(WIDTH_KNOBS - 6, STD_HEIGHT - 4));
			textarea.setForeground(Color.BLUE.darker());
			ticker.setOpaque(true);
			ticker.setForeground(Color.BLUE.darker());
			ticker.setBackground(Pastels.BUTTONS);
	    	scroller.setViewportView(textarea);
	        add(scroller);
    	});
        /* Sleep between checking the debugQueue for messages */
		Thread.startVirtualThread(()->{
			try {
				Log dat;
				final int refresh = 2 * Constants.GUI_REFRESH;
				while (true) {

		            dat = debugQueue.poll();
		            if (dat == null) {
		            	Thread.sleep(refresh);
		            	continue;
		            }

		            if (dat.warn)
		                info(dat.clazz + " WARN: " + dat.msg);
		            else
		                info(dat.clazz + ": " + dat.msg);

				}
			} catch (Exception e) {
				System.err.println(e);
			}
		});

    }

    /** output to console (not for realtime) */
    public static void addText(String in) {
        log.debug(in);
        ticker.setText(in);
        if (in == null)
            in = "null" + NL;
        if (false == in.endsWith(NL))
            in += NL;

        textarea.append(new String(in));
        textarea.setCaretPosition(textarea.getDocument().getLength());
        scroller.getHorizontalScrollBar().setValue(0);
        scroller.getVerticalScrollBar().setValue(scroller.getVerticalScrollBar().getMaximum());
        instance.invalidate();
        scroller.repaint();
    }

    public static void newLine() {
        addText("" + NL);
    }

    static void warn(Throwable t) {
        addText("WARN " + t.getMessage());
        log.warn(t.getMessage(), t);
    }

    static void warn(String s, Throwable t) {
        addText("WARN " + s);
        if (t != null) log.warn(s, t);
    }

    static void info(String s) {
        if (level == Level.DEBUG || level == Level.INFO || level == Level.TRACE)
            addText(s);
    }

    public static void debug(String s) {
        if (level == Level.DEBUG || level == Level.TRACE)
            addText("debug " + s);
    }

	@Override
	public boolean doKnob(int idx, int value) {
		return false;
	}

	@Override
	public void update() {
	}

	@Override
	public void pad1() {
	}

}

//public interface MidiListener {
//	enum PassThrough {ALL, NONE, NOTES, NOT_NOTES}
//	void feed(Midi midi);
//	PassThrough getPassThroughMode();}
//  @Override public PassThrough getPassThroughMode() {
//      return PassThrough.ALL; }
//  @Override public void feed(Midi midi) {
//      addText("midilisten: " + midi); }
//  private void midiListen() {
//      midiListen = !midiListen;
//      ArrayList<MidiListener> listeners = Sequencer.getCurrent().getListeners();
//      if (midiListen)
//          listeners.add(this);
//      else
//          listeners.remove(this);
//  }


