package net.judah.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.ZoneMidi;
import net.judah.controllers.KnobData;
import net.judah.controllers.Qwerty;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.LFO;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.PresetsView;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.ModalDialog;
import net.judah.looper.Looper;
import net.judah.midi.Actives;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.omni.Zwing;
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;
import net.judah.scope.Scope;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiView;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.seq.chords.Chord;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.ChordScroll;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.chords.Section;
import net.judah.seq.chords.SectionCombo;
import net.judah.seq.piano.PianoTab;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Programmer;
import net.judah.song.Overview;
import net.judah.song.Scene;
import net.judah.song.SceneLauncher;
import net.judah.song.SongView;
import net.judah.song.setlist.SetlistView;
import net.judah.synth.taco.TacoSynth;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** over-all layout and background updates thread */
public class MainFrame extends JFrame implements Size, Runnable, Pastels, Zwing {

	private static MainFrame instance;
    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
	private static final ExecutorService focus = Executors.newVirtualThreadPerTaskExecutor();
	@Getter private static KnobMode knobMode = KnobMode.MIDI;

	@Getter private final Qwerty tabs;
    @Getter private KnobPanel knobs;
    @Getter private final BeatsTab beatBox;
    @Getter private final MidiTab synthBox;

    private final DJJefe mixer;
	private final FxPanel effects;
    private final MiniSeq miniSeq;
	private final Overview songs;
    private final HQ hq;
    private SheetMusicPnl sheetMusic;
	private final Seq seq;
    private final Looper looper;
    private final MiniLooper loops;
    private final ChordTrack chords;
    private final MidiGui midiGui;
    private final Sampler sampler;
	private final JudahMenu menu = new JudahMenu(WIDTH_KNOBS);
	private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
    private final JComponent knobHolder = Box.createVerticalBox();
    private final Scope scope;
    private final PresetsView presets;
    private final SetlistView setlists;
    private final KitKnobs kitKnobs;

    public MainFrame(String name, JudahClock clock, FxPanel controls, DJJefe djJefe, Seq sequencer, Looper loopr,
    		Overview songz, MidiGui gui, DrumMachine drums, Scope scope, Channel focus) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = loopr;
        this.seq = sequencer;
        this.songs = songz;
        this.chords = seq.getChords();
        this.midiGui = gui;
        this.kitKnobs = drums.getKnobs();
        this.sampler = seq.getSampler();
        this.scope = scope;
        hq = new HQ(clock, looper, songs, chords);
        miniSeq = new MiniSeq(seq.getTracks(), clock);
        loops = new MiniLooper(looper, clock);
        presets = new PresetsView(JudahZone.getPresets());
        setlists = new SetlistView(JudahZone.getSetlists(), songs);
        synthBox = new PianoTab(seq.getSynthTracks());
    	beatBox = new BeatsTab(seq.getDrumTracks());

    	setLocationByPlatform(true);
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setForeground(Color.DARK_GRAY);
        setSize(WIDTH_FRAME, HEIGHT_FRAME);
        setLocation(1, 0);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 2) { // dual monitor
        	JFrame dummy = new JFrame(screens[1].getDefaultConfiguration());
        	setLocationRelativeTo(dummy);
        	dummy.dispose();
        }
        try {
        	setIconImage(Icons.get("icon.png").getImage());
        	sheetMusic = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE);
        } catch (Exception e) { RTLogger.warn(this, e); }

        tabs = new Qwerty(sheetMusic, songs, beatBox, synthBox, chords.getChordSheet());
        tabs.addChangeListener(change->{
			if (tabs.getSelectedComponent() == null) return;
			if (tabs.getSelectedComponent() instanceof MidiTab) {
				MidiTrack track = ((MidiTab)tabs.getSelectedComponent()).getCurrent();
				focus(track.getMidiOut());
			}
			else if (tabs.getSelectedComponent() instanceof Overview)
				focus(KnobMode.MIDI);
		});

        mode.setFont(BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());
		});

        JPanel mini = new JPanel();
        mini.setLayout(new BoxLayout(mini, BoxLayout.LINE_AXIS));
        mini.add(Box.createHorizontalStrut(1));
        mini.add(miniSeq); mini.add(loops);

        size(knobHolder, KNOB_PANEL);
        size(mode, new Dimension(Size.COMBO_SIZE.width, STD_HEIGHT + 2));
        size(mixer, MIXER_SIZE);
        knobHolder.setBorder(new LineBorder(Pastels.MY_GRAY, 1));

        // clock, midi panel, fx controls
        JComponent left = Box.createVerticalBox();
        left.add(Gui.wrap(menu));
        left.add(Gui.wrap(hq));
        left.add(Gui.wrap(mini));
        left.add(knobHolder);
        left.add(effects);
        left.add(Gui.wrap(RTLogger.getTicker()));

        // mixer & main view
        JComponent right = Box.createVerticalBox();
        right.add(tabs);
        right.add(mixer);
        right.add(Box.createVerticalGlue());

        JPanel content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.LINE_AXIS));
        content.add(left);
        content.add(right);
        content.add(Box.createHorizontalGlue());

        mixer.updateAll();
        validate(); pack();

        Thread.ofVirtual().start(this);
        EventQueue.invokeLater(()->{
	        setFocus(focus);
	        tabs.setSelectedIndex(0);
	        tabs.requestFocusInWindow();
	        seq.getTracks().setCurrent(seq.getTracks().getFirst());
	        beatBox.changeTrack();
	        knobMode(KnobMode.MIDI);
	        setVisible(true);
        });
    }

    public void sheetMusic(boolean fwd) {
    	if (sheetMusic == null || sheetMusic.getFile() == null) { // startup
    		sheetMusic("Four.png");
    		return;
    	}
    	File[] files = Folders.getSheetMusic().listFiles();
    	File current = sheetMusic.getFile();
    	int i;
    	for (i = 0; i < files.length; i++)
    		if (files[i].equals(current))
    			break;
    	sheetMusic(files[Constants.rotary(i, files.length, fwd)]);
    }

    public void sheetMusic(String s) {
    	sheetMusic(new File(Folders.getSheetMusic(), s));
    }

    public void sheetMusic(File file) {
    	focus.execute(()->{
	    	try {
		    	if (sheetMusic == null) {
		    		sheetMusic = new SheetMusicPnl(file, TAB_SIZE);
		    		tabs.addTab(Constants.CUTE_NOTE + sheetMusic.getName(), sheetMusic);
		    	}
		    	else {
		    		sheetMusic.setImage(file);
		    		tabs.title(sheetMusic);
		    	}
		    	if (menu.getSheets().isSelected())
		    		tabs.setSelectedComponent(sheetMusic);

	    	} catch (Throwable e) {
	    		RTLogger.warn(this, e);
	    	}
    	});
    }

	public static void changeTab(boolean fwd) {
		focus.execute(() -> instance.tabs.tab(fwd));
	}

    public static void setFocus(Object o) {
    	focus.execute(()->instance.focus(o));
    }

    private void focus(Object o) {
		if (o instanceof KnobMode mode) {
			knobMode(mode);
			return;
		}
		if (o instanceof KnobPanel p) {
			knobPanel(p);
			return;
		}

		if (o instanceof Channel ch) {
    		effects.setFocus(ch);
    		mixer.highlight(ch);
    		//scope.channelSelected(ch);
    		update(o);
    		if (knobMode == KnobMode.LFO)
    			knobPanel(ch.getLfoKnobs());
		}
		else if (o instanceof MultiSelect multiselect) { // TODO LFO/Compressor
			Channel next = multiselect.get(multiselect.size()-1);
			mixer.highlight(multiselect);
			effects.addFocus(next);
			//scope.channelSelected(next);
		}
		else if (o instanceof Scene scene) {
			songs.getSongView().setCurrent(scene);
			hq.sceneText();
			songs.update();
		}

		else if (o instanceof MidiTab tab) {
			tab.changeTrack();
			miniSeq.update();
			tabs.title(tab);
		}
		else if (o instanceof TrackList current) {
			MidiTrack hello = current.getCurrent();
			miniSeq.update();
			menu.trax(hello);
			if (knobMode == KnobMode.TRACK)
				knobPanel(seq.getKnobs(hello));
		}

    }

    public static void update(Object o) {
    	updates.offer(o);
    }

    /** GUI feed updates off Real-Time thread */
	@Override public void run() {
		Object o = null;
		while (true) {

			o = updates.poll();
			if (o == null) {
				Threads.sleep(Constants.GUI_REFRESH);
				continue;
			}
			try {
				if (updates.contains(o))
					continue;
			} catch (ConcurrentModificationException e) {
				RTLogger.warn(this, "concurrent modification on update " + o);
			}

			try { // to keep the GUI thread running
			if (o instanceof Channel ch) {
				mixer.update(ch);
				if (effects.getChannel() == ch)
					effects.getChannel().getGui().update();
				if (knobs instanceof LFOKnobs && ((LFOKnobs)knobs).getChannel() == ch)
					knobs.update();
				else if (ch instanceof ZoneMidi) {  // TODO update
					if (o == synthBox.getCurrent().getMidiOut())
						synthBox.getView().getInstrumentPanel().repaint();
					else if (o == beatBox.getTop().getCurrent().getMidiOut())
						beatBox.midiUpdate(true);
					else if (o == beatBox.getBottom().getCurrent().getMidiOut())
						beatBox.midiUpdate(false);
				}
				else if (ch instanceof Sample && knobMode == KnobMode.SAMPLE)
					sampler.getView().update((Sample)o);
			}
			else if (o instanceof MidiTrack t) {
				PlayWidget.update(t);
				miniSeq.update(t);
				seq.getKnobs(t).update();
				synthBox.update(t);
				beatBox.update(t);
				songs.update(t);
				Programmer.update(t);
				//midiGui.update(t);
			}
			else if (o instanceof Actives a) {
				if (a.getChannel() >= 9 && knobMode == KnobMode.KITS && ((KitKnobs)knobs).getKit() == o)
					((KitKnobs)knobs).update(a);
				else if (synthBox.isVisible() && synthBox.getCurrent().getActives() == a)
					synthBox.getView().getInstrumentPanel().repaint();
			}
			else if (o instanceof Sampler) // StepSample changed
    			midiGui.update();
			else if (o instanceof JudahClock) {
				hq.length();
				hq.metronome();
				mixer.update(looper.getLoopA());
			}
			else if (o instanceof Program prog) {
				Program.update(prog);
				if (prog.getMidiOut() instanceof TacoSynth)
					((TacoSynth)prog.getMidiOut()).getSynthKnobs().update();
			}
			else if (o instanceof Scene) {
				hq.sceneText();
				songs.getSongView().update((Scene)o);
			}
			else if (o == seq || o == songs)
				songs.update();
			else if (o == mixer)
				mixer.updateAll();
			else if (o instanceof SceneLauncher launcher)
				launcher.fill();
			else if (o instanceof Updateable updateable)
				updateable.update();
			else if (o instanceof KnobData data)
				doKnob(data.idx(), data.data2());
			else if (o instanceof LFO) {
				if (effects.getChannel().getLfo() == o)
					effects.getChannel().getGui().update(); // heavy?
			}
			else if (o instanceof SongView songView) {
				songView.getLauncher().update();
				hq.sceneText();
			}
			else if (o instanceof Chord chord) {
				chords.getView().update(chord);
				chords.getChordSheet().update(chord);
				ChordScroll.scroll();
			}
			else if (o instanceof Section sec) {
				chords.getView().setSection(sec);
				chords.getChordSheet().setSection(sec);
				SectionCombo.setSection(sec);
				ChordPlay.update();
			}
			else if (o instanceof ChordTrack) {
				chords.getView().updateDirectives();
				chords.getChordSheet().updateDirectives();
			}
			else if (o instanceof float[][])
				scope.update((float[][])o);
			else if (o instanceof String name) {
				MidiTrack t = seq.byName(name);
				miniSeq.update(t);
				PlayWidget.update(t);
			}

			else RTLogger.log(this, "unknown " + o.getClass().getSimpleName() + " update: " + o.toString());
			} catch (Throwable t) {
				RTLogger.warn(this, t);
			}
		}
	}

	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		mode.setSelectedItem(knobMode);
    	KnobPanel focus = null;
		switch(knobs) {
			case MIDI: focus = midiGui; break;
			case KITS: focus = kitKnobs; break;
			case TRACK: focus = seq.getKnobs(seq.getCurrent()); break;
			case LFO: focus = effects.getChannel().getLfoKnobs(); break;
			case TACO: focus = JudahZone.getTacos().taco.getSynthKnobs(); break;
			case SAMPLE: focus = sampler.getView(); break;
			case PRESETS: focus = presets; break;
			case SETLIST: focus = setlists; break;
			case TOOLS: focus = scope; break;
			case LOG: focus = RTLogger.instance; break;
		}
		knobPanel(focus);
    }

    private void knobPanel(final KnobPanel view) {
    	if (knobs == view) {
    		knobs.update();
    		return;
    	}

    	knobs = view;
    	if (knobMode != view.getKnobMode()) {
    		knobMode = view.getKnobMode();
    		mode.setSelectedItem(knobMode);
    	}

    	JComponent knobTitle = Box.createHorizontalBox();
        knobTitle.add(Box.createHorizontalStrut(5));
        knobTitle.add(mode);
    	knobTitle.add(Gui.resize(view.getTitle(), KNOB_TITLE));
    	knobHolder.removeAll();
    	knobHolder.add(knobTitle);
    	knobHolder.add(knobs);
    	knobHolder.validate();
    	knobHolder.repaint();
    }

	private void doKnob(int idx, int data2) {
		if (knobs != null && knobs.doKnob(idx, data2))
			update(knobs);
	}

	public static void startNimbus() {
		try {
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("ScrollBar.buttonSize", new Dimension(8,8));
            UIManager.put("nimbusBase", Pastels.EGGSHELL);
            UIManager.put("control", Pastels.EGGSHELL);
            UIManager.put("nimbusBlueGrey", Pastels.MY_GRAY);
            UIManager.getLookAndFeel().getDefaults().put("Button.contentMargins", new Insets(5, 5, 5, 5));
            UIManager.getLookAndFeel().getDefaults().put("JToggleButton.contentMargins", new Insets(1, 1, 1, 1));
            Thread.sleep(10); // let nimbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }
	}

	// assign keyboard input
	public static void qwerty() {
		instance.tabs.requestFocusInWindow();
	}

	public void edit(MidiTrack track) {
		MidiTab tab = track.isDrums() ? beatBox : synthBox;
		tab.setCurrent(track);
		tabs.setSelectedComponent(tab);
		focus(track.getMidiOut());
	}

	public static MidiView getMidiView(MidiTrack t) {
		if (t.isDrums())
			return instance.beatBox.getView(t);
		else
			return instance.synthBox.getView(t);
	}

	public static void set() {
		if (ModalDialog.getInstance() != null) {
			ModalDialog.getInstance().dispatchEvent(new WindowEvent(
                ModalDialog.getInstance(), WindowEvent.WINDOW_CLOSING));
		}
		else if (SetCombo.getSet() != null)
			SetCombo.set();
		else if (instance.tabs.getSelectedComponent() instanceof MidiTab) {
			((MidiTab)instance.tabs.getSelectedComponent()).getMusician().delete();
		}
	}

	public static MiniSeq miniSeq() { return instance.miniSeq; }

}