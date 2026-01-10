package net.judah.seq.track;

import static net.judah.gui.Size.COMBO_SIZE;
import static net.judah.gui.Size.MEDIUM;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import judahzone.gui.Actionable;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.widgets.Btn;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.PlayBtn;
import net.judah.gui.widgets.RecordWidget;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.SynthRack;
import net.judah.seq.track.Computer.Update;

public abstract class TrackMenu extends Box implements MouseListener {

    public static class SendTo extends JMenu {
        public SendTo(MidiTrack source) {
            super("SendTo...");
            for (MidiTrack t : source.isDrums() ?
                    JudahZone.getInstance().getDrumMachine().getTracks() :
                    SynthRack.getSynthTracks()) {
                if (t != source)
                    add(new Actionable(t.getName(), evt -> t.load(source)));
            }
        }
    }

    protected final NoteTrack track;
    protected final MusicBox grid;
    protected final MenuActions menuActions;
    protected final ButtonGroup cue = new ButtonGroup();
    protected final ButtonGroup gate = new ButtonGroup();
    protected final JMenuBar menu = new JMenuBar();
    protected final JMenu file;
    protected final JMenu edit = new JMenu("Edit");
    protected final JMenu tools = new JMenu("Tools");
    protected final JMenu cues = new JMenu("Cue");
    protected final JMenu quantization = new JMenu("Quantization");
    protected final PlayBtn play;
    protected RecordWidget capture;
    protected final Program program;
    protected final Programmer programmer;
    protected TrackVol velocity;

    public TrackMenu(MusicBox g) {
        super(BoxLayout.X_AXIS);
        track = g.getTrack();
        grid = g;
        menuActions = new MenuActions(track.getEditor(), grid);
        file = new JMenu(track.getName());
        addMouseListener(this);
        play = new PlayBtn(track);
        programmer = new Programmer(track);
        velocity = new TrackVol(track);
        program = new Program(track);

        initializeMenus();
    }

    private void initializeMenus() {
        fileSetup();
        fileMenu();

        // Build tools menu using MenuActions
        menuActions.buildToolsMenu(tools);

        menu.add(file);
        menuActions.buildEditMenu(edit);
        if (track.isSynth()) {
            menu.add(tools);
            menu.add(edit);
        } else {
            // Consolidate for drums (no space)
            file.add(tools);
            file.add(edit);
            menu.add(file);
        }

        add(Box.createHorizontalStrut(2));
        add(play);
        if (track.isSynth()) {
            capture = new RecordWidget(track);
            add(capture);
        }
        add(menu);
        add(Gui.resize(program, track.isDrums() ? MEDIUM : COMBO_SIZE));
        add(new Btn(Icons.SAVE, e -> track.save()));
        add(programmer);

        if (track.isDrums())
            file.setFont(Gui.BOLD12);

        // Force menu to validate after all items are added
        revalidate();
        repaint();
    }

    public final void updateCue() {
        int i = 0;
        Enumeration<AbstractButton> it = cue.getElements();
        while (it.hasMoreElements()) {
            if (track.getCue().ordinal() == i++)
                it.nextElement().setSelected(true);
            else
                it.nextElement();
        }
    }

    public final void updateGate() {
        int i = 0;
        Enumeration<AbstractButton> it = gate.getElements();
        while (it.hasMoreElements()) {
            if (track.getGate().ordinal() == i++)
                it.nextElement().setSelected(true);
            else
                it.nextElement();
        }
    }

    private void fileSetup() {
        for (Cue c : Cue.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(c.name());
            if (track.getCue() == c)
                item.setSelected(true);
            cue.add(item);
            cues.add(item);
            item.addActionListener(e -> track.setCue(c));
        }
        for (Gate g : Gate.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(g.name());
            if (track.getGate() == g)
                item.setSelected(true);
            gate.add(item);
            quantization.add(item);
            item.addActionListener(e -> track.setGate(g));
        }
    }

    private void fileMenu() {
        file.removeAll();

        if (MainFrame.isBundle()) {
            file.add(new Actionable("Clear", e -> track.clear()));
            file.add(new Actionable("Import...", e -> new ImportMidi(track)));
            file.add(new Actionable("Export...", e -> track.saveAs()));
        } else {
            file.add(new Actionable("New", e -> track.clear()));
            file.add(new Actionable("Open", e -> track.load()));
            file.add(new Actionable("Save", e -> track.save()));
            file.add(new Actionable("Save As...", e -> track.saveAs()));
        }
        file.add(cues);
        file.add(quantization);
    }

    @Override public void mouseClicked(MouseEvent e) { }
    @Override public void mouseReleased(MouseEvent e) { }
    @Override public void mouseEntered(MouseEvent e) { }
    @Override public void mouseExited(MouseEvent e) { }
    @Override public void mousePressed(MouseEvent e) { }

    public void update() {
        updateCue();
        program.update();
        fileMenu();
    }

    public void update(Update type) {
        if (Update.PROGRAM == type)
            program.update();
        else if (Update.CUE == type)
            updateCue();
        else if (Update.GATE == type)
            updateGate();
        else if (Update.CYCLE == type)
            programmer.getCycle().update();
        else if (Update.CAPTURE == type && capture != null)
            capture.update();
        else if (Update.PLAY == type)
            play.update();
        else if (Update.CURRENT == type)
            programmer.getCurrent().update();
        else if (Update.LAUNCH == type)
            programmer.liftOff();
        else if (Update.FILE == type)
            programmer.liftOff();
        else if (Update.AMP == type)
            velocity.update();
//        else if (Update.EDIT == type)
//            programmer.liftOff();
    }
}