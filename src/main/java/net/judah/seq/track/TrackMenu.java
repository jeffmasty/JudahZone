package net.judah.seq.track;

import static net.judah.gui.Size.COMBO_SIZE;
import static net.judah.gui.Size.MEDIUM;

import java.awt.Dimension;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import judahzone.gui.Actionable;
import judahzone.gui.Gui;
import judahzone.gui.Icons;
import judahzone.util.Threads;
import judahzone.widgets.Btn;
import net.judah.JudahZone;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.PlayBtn;
import net.judah.gui.widgets.RecordWidget;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.SynthRack;
import net.judah.seq.track.Computer.Update;


/** •  TrackMenu: Common file, edit, tools menus (condensed for drums), velocity
    •  PianoMenu: Range, octave navigation, gate, arpeggiator mode, duration, transpose
	•  DrumMenu: CC mapping, recording toggle, remap/clean/condense tools 	*/
public abstract class TrackMenu extends Box { // to update Programmer (length) implement TrackListener

	protected final JMenu file = new JMenu("File");
	protected final JMenu edit = new JMenu("Edit");
	protected final JMenu tools = new JMenu("Tools");
	private final TrackVol velocity;
	protected final NoteTrack track;
    protected final MusicBox grid;

    protected final MenuActions menuActions;
    protected final ButtonGroup cue = new ButtonGroup();
    protected final ButtonGroup gate = new ButtonGroup();
    protected final JMenuBar menu = new JMenuBar();
    protected final JMenu cues = new JMenu("Cue");
    protected final JMenu quantization = new JMenu("Quantization");
    protected final PlayBtn play;
    protected RecordWidget capture;
    protected final Program program;
    protected final Programmer programmer;

	public TrackMenu(MusicBox grid) {
		super(BoxLayout.X_AXIS);
	    this.track = grid.getTrack();
	    this.grid = grid;
	    setMaximumSize(new Dimension(3000, Size.KNOB_HEIGHT));
	    menuActions = new MenuActions(track.getEditor(), grid);
	    play = new PlayBtn(track);
	    programmer = new Programmer(track);
	    velocity = new TrackVol(track);
	    program = new Program(track);
	    capture = new RecordWidget(track);

	    buildLocal();

	    menu.add(file);

	    // add standard issue
	    add(Box.createHorizontalStrut(2));
	    add(play);
	    add(capture);
	    add(menu);
	    add(Gui.resize(program, track.isDrums() ? MEDIUM : COMBO_SIZE));
	    add(new Btn(Icons.SAVE, e -> track.save()));
	    add(programmer);
	    Threads.execute(() ->
		    SwingUtilities.invokeLater(()-> {
		    	childMenus();
		    	add(velocity);
		    	revalidate();
	    }));
	}

	private void buildLocal() {
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
        fileMenu();
	    menuActions.buildToolsMenu(tools);
	    menuActions.buildEditMenu(track.isDrums() ? file : edit);
	}

	/** Override to add instrument-specific menus (call super())*/
	protected abstract void childMenus();


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

    private void fileMenu() {  // weird case switching to Bundle, drums will miss tools/edit?
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
        else if (Update.CAPTURE == type && capture != null) // legacy null
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
//        else if (Update.EDIT == type) // TODO TrackListener.data changes affect length
//            programmer.liftOff();
    }
}