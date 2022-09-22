package net.judah.util;

import static net.judah.util.Constants.NL;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Level;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.midi.MidiListener;

@Log4j
public class Console implements MidiListener {

    private static Console instance;
    public static Console getInstance() {
        if (instance == null) instance = new Console();
        return instance;
    }
    
    @Getter @Setter private static Level level = Level.DEBUG;
    @Getter private final JScrollPane scroller;
    private final JTextArea textarea = new JTextArea(5, 30);

    @Getter private ArrayList<ConsoleParticipant> participants = new ArrayList<>();

    private Console() {

        textarea.setEditable(false);
		textarea.setForeground(Color.BLUE.darker()/* new Color(1, 77, 13) *//* dark green */);
        
        scroller = new JScrollPane(textarea);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        //participants.add(this);
        participants.add(JudahZone.getFluid().getConsole());

        instance = this;
    }


    /** output to console (not for realtime) */
    static void addText(String in) {
        log.debug(in);
        String s = in == null ? null : new String(in);
        if (instance == null || instance.textarea == null) {
            return;
        }
        if (s == null) {
            s = "null" + NL;
        }

        if (false == s.endsWith(NL))
            s = s + NL;

        instance.textarea.append(s);

        instance.textarea.setCaretPosition(instance.textarea.getDocument().getLength() - 1);
        instance.scroller.getVerticalScrollBar().setValue( instance.scroller.getVerticalScrollBar().getMaximum() - 1 );
        instance.scroller.getHorizontalScrollBar().setValue(0);
        instance.scroller.invalidate();

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

//    private void midiListen() {
//        midiListen = !midiListen;
//        ArrayList<MidiListener> listeners = Sequencer.getCurrent().getListeners();
//        if (midiListen)
//            listeners.add(this);
//        else
//            listeners.remove(this);
//    }


    @Override
    public PassThrough getPassThroughMode() {
        return PassThrough.ALL;
    }


    @Override
    public void feed(Midi midi) {
        addText("midilisten: " + midi);
    }

}
