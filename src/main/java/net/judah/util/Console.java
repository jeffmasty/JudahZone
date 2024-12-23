package net.judah.util;

import static net.judah.util.Constants.NL;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
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
public class Console extends KnobPanel {

	public static interface Participant {
		void process(String[] input);
	}

    @Getter private static Console instance = new Console();
    @Getter @Setter private static Level level = Level.DEBUG;
    @Getter private final JLabel ticker = new JLabel("", JLabel.LEFT);
    private final JScrollPane scroller = new JScrollPane();
    @Getter private ArrayList<Participant> participants = new ArrayList<>();
    private final JTextArea textarea = new JTextArea(3, 28);
    @Getter private final KnobMode knobMode = KnobMode.LOG;
    @Getter private final Component title = Gui.wrap(new JLabel(""));

    private Console() {
    	setLayout(new GridLayout(1, 1));
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	Gui.resize(ticker, new Dimension(Size.WIDTH_KNOBS - 6, Size.STD_HEIGHT - 4));
    	textarea.setEditable(false);
		textarea.setForeground(Color.BLUE.darker());
		ticker.setOpaque(true);
		ticker.setForeground(Color.BLUE.darker());
		ticker.setBackground(Pastels.BUTTONS);


    	scroller.setViewportView(textarea);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroller);
    }

    /** output to console (not for realtime) */
    public static void addText(String in) {
        log.debug(in);
        instance.ticker.setText(in);
        if (in == null)
            in = "null" + NL;
        if (false == in.endsWith(NL))
            in += NL;

        instance.textarea.append(new String(in));
        instance.textarea.setCaretPosition(instance.textarea.getDocument().getLength() - 1);

        instance.scroller.getVerticalScrollBar().setValue( instance.scroller.getVerticalScrollBar().getMaximum() - 1 );
        instance.scroller.getHorizontalScrollBar().setValue(0);
        instance.invalidate();
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
//    @Override public PassThrough getPassThroughMode() {
//        return PassThrough.ALL; }
//    @Override public void feed(Midi midi) {
//        addText("midilisten: " + midi); }
//    private void midiListen() {
//        midiListen = !midiListen;
//        ArrayList<MidiListener> listeners = Sequencer.getCurrent().getListeners();
//        if (midiListen)
//            listeners.add(this);
//        else
//            listeners.remove(this);
//    }

