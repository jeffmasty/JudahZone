package net.judah.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
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
import net.judah.sampler.Sample;
import net.judah.sampler.Sampler;
import net.judah.scope.Scope;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiView;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.seq.chords.*;
import net.judah.seq.piano.PianoTab;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.Programmer;
import net.judah.song.Overview;
import net.judah.song.Scene;
import net.judah.song.SceneLauncher;
import net.judah.song.SongView;
import net.judah.song.setlist.SetlistView;
import net.judah.synth.JudahSynth;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** over-all layout and background updates thread */
public class MainFrame extends JFrame implements Size, Runnable, Pastels {

    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
	private static MainFrame instance;
	private static final ExecutorService focus = Executors.newFixedThreadPool(20);
	@Getter private static KnobMode knobMode = KnobMode.Midi;
	@Getter private final Qwerty tabs;
    @Getter private KnobPanel knobs;
    
    private final MiniSeq miniSeq;
	private final Overview songs;
    private final HQ hq;
    private SheetMusicPnl sheetMusic;
	private final Seq seq;
    private final Looper looper;
    private final MiniLooper loops;
    private final ChordTrack chords;
    private final MidiGui midiGui;
    private final DrumMachine drumMachine;
    private final Sampler sampler;
    private final Scope scope;
	private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
	private final JudahMenu menu = new JudahMenu(Size.WIDTH_KNOBS);
	private final FxPanel effects;
    private final DJJefe mixer;
    private final BeatsTab beatBox;
    private final MidiTab synthBox;
    private final JPanel knobHolder = new JPanel();
    private final JPanel knobTitle = new JPanel();
    private final PresetsView presets;
    private final SetlistView setlists;
    
    public MainFrame(String name, JudahClock clock, FxPanel controls, DJJefe djJefe, Seq sequencer, Looper loopr, 
    		Overview songz, ChordTrack chordz, MidiGui gui, DrumMachine drums, Sampler samp, Scope scope, Channel focus) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = loopr;
        this.seq = sequencer;
        this.songs = songz;
        this.chords = chordz;
        this.midiGui = gui;
        this.drumMachine = drums;
        this.sampler = samp;
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
				MidiTrack track = ((MidiTab)tabs.getSelectedComponent()).getCurrent().getTrack();
				focus(track.getMidiOut());
			}
			else if (tabs.getSelectedComponent() instanceof Overview) 
				focus(KnobMode.Midi);
		});

        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());
		});

        JPanel mini = new JPanel();
        mini.setLayout(new BoxLayout(mini, BoxLayout.LINE_AXIS));
        mini.add(Box.createHorizontalStrut(1));
        mini.add(miniSeq); mini.add(loops);

        Gui.resize(knobTitle, new Dimension(KNOB_PANEL.width - 2, Size.STD_HEIGHT + 4));
        Gui.resize(knobHolder, KNOB_PANEL);
        Gui.resize(mode, new Dimension(Size.COMBO_SIZE.width, Size.STD_HEIGHT + 2));
        Gui.resize(mixer, MIXER_SIZE);

        knobTitle.setLayout(new BoxLayout(knobTitle, BoxLayout.LINE_AXIS));
        knobTitle.add(Box.createHorizontalStrut(5));
        knobTitle.add(mode);
        knobHolder.setLayout(new BoxLayout(knobHolder, BoxLayout.PAGE_AXIS));
        knobHolder.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        knobHolder.add(knobTitle);

        JPanel left = new JPanel(); // clock, midi panel, fx controls
        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(Gui.wrap(menu));
        left.add(Gui.wrap(hq));
        left.add(Gui.wrap(mini));
        left.add(knobHolder);
        left.add(effects);
        left.add(Gui.wrap(Console.getInstance().getTicker()));
        
        JPanel right = new JPanel(); // mixer & main view
        right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
        right.add(tabs);
        right.add(mixer);
        right.add(Box.createVerticalGlue());
        
        JPanel content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.LINE_AXIS));
        content.add(left);
        content.add(right);
        content.add(Box.createHorizontalGlue());
        
        beatBox.getTop().setCurrent(seq.getDrumTracks().get(0));
        beatBox.getBottom().setCurrent(seq.getDrumTracks().get(2));
        mixer.updateAll();

        validate();
        pack();
        Constants.sleep(2 * Constants.GUI_REFRESH);
        setFocus(focus);
        setVisible(true);
        knobMode(KnobMode.Tools);
        tabs.setSelectedIndex(0);
        tabs.requestFocusInWindow();
        Constants.sleep(Constants.GUI_REFRESH);
        knobMode(KnobMode.Midi);

        Thread updates = new Thread(this);
        updates.setPriority(2);
        updates.start();
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
		    		sheetMusic = new SheetMusicPnl(file, Size.TAB_SIZE);
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
		if (o instanceof KnobMode) 
			knobMode((KnobMode)o);
		else if (o instanceof KnobPanel) 
			knobPanel((KnobPanel)o);
		
		if (o instanceof Channel) {
			Channel ch = (Channel)o;
    		effects.setFocus(ch);
    		mixer.highlight(ch);
    		update(o);
    		if (knobMode == KnobMode.LFO)
    			knobPanel(ch.getLfoKnobs());
		}
		else if (o instanceof MultiSelect) { // TODO LFO/Compressor
			ArrayList<Channel> multiselect = (MultiSelect)o;
			Channel next = multiselect.get(multiselect.size()-1);
			mixer.highlight(multiselect);
			effects.addFocus(next);
		}
		else if (o instanceof Scene) {
			songs.getSongView().setCurrent((Scene)o);
			hq.sceneText();
			songs.update();
		} 
    	else if (o instanceof TrackList) { 
    		TrackList t = (TrackList)o;
    		if (o == seq.getTracks()) {
    			miniSeq.update();
    			if (knobMode == KnobMode.Track)
    				knobPanel(seq.getKnobs(t.getCurrent()));
    		}
    		else if (o == seq.getSynthTracks()) {
    			synthBox.changeTrack();
    			tabs.title(synthBox);
    		} else if (o == beatBox.getTop() || o == beatBox.getBottom()) {
    			beatBox.changeTrack();
    		}
    		SetCombo.override();
    	}
    	else if (o instanceof KnobMode) 
    		knobMode((KnobMode)o);
    	
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
				Constants.sleep(Constants.GUI_REFRESH);
				continue;
			}
			try {
				if (updates.contains(o))
					continue;
			} catch (ConcurrentModificationException e) {
				RTLogger.warn(this, "concurrent modification on update " + o);
			}
			
			if (o instanceof Channel) {
				Channel ch = (Channel)o;
				mixer.update(ch);
				if (effects.getChannel() == ch)
					effects.getChannel().getGui().update();
				if (knobs instanceof LFOKnobs && ((LFOKnobs)knobs).getChannel() == ch)
					knobs.update();
				else if (ch instanceof ZoneMidi) {  // TODO update
					if (o == seq.getSynthTracks().getCurrent().getMidiOut())
						synthBox.getCurrent().getInstrumentPanel().repaint();
					else if (o == beatBox.getTop().getCurrent().getMidiOut())
						beatBox.midiUpdate(true);
					else if (o == beatBox.getBottom().getCurrent().getMidiOut())
						beatBox.midiUpdate(false);
				}
				else if (ch instanceof Sample && knobMode == KnobMode.Samples) 
					sampler.getView().update((Sample)o);
			}
			else if (o instanceof MidiTrack) {
				MidiTrack t = (MidiTrack)o;
				PlayWidget.update(t);
				miniSeq.update(t);
				seq.getKnobs(t).update();
				synthBox.update(t);
				beatBox.update(t);
				songs.update(t);
				Programmer.update(t);
			}
			else if (o instanceof Actives) {
				Actives a = (Actives)o;
				if (a.getChannel() >= 9 && knobMode == KnobMode.Kits && ((KitKnobs)knobs).getKit() == o)
					((KitKnobs)knobs).update(a);
				else if (synthBox.isVisible() && synthBox.getCurrent().getTrack().getActives() == a)
					synthBox.getCurrent().getInstrumentPanel().repaint();
			}
			else if (o instanceof Sampler) // StepSample changed
    			midiGui.update();
			else if (o instanceof JudahClock) {
				hq.length();
				hq.metronome();
				mixer.update(looper.getLoopA()); 
			}
			else if (o instanceof Program) {
				Program prog = (Program)o;
				Program.update(prog);
				if (prog.getPort() instanceof JudahSynth)
					((JudahSynth)prog.getPort()).getSynthKnobs().update();
			}
			else if (o instanceof Scene) {
				hq.sceneText();
				songs.getSongView().update((Scene)o);
			}
			else if (o == seq || o == songs)
				songs.update();
			else if (o == mixer)
				mixer.updateAll();
			else if (o instanceof SceneLauncher) 
				((SceneLauncher)o).fill();
			else if (o instanceof Updateable) 
				((Updateable)o).update();
			else if (o instanceof KnobData) 
				doKnob(((KnobData)o).idx, ((KnobData)o).data2);
			else if (o instanceof LFO) {
				if (effects.getChannel().getLfo() == o)
					effects.getChannel().getGui().update(); // heavy?
			}
			else if (o instanceof SongView) {
				((SongView)o).getLauncher().update();
				hq.sceneText();
			}
			else if (o instanceof Chord) {
				chords.getView().update((Chord)o);
				chords.getChordSheet().update((Chord)o);
				ChordScroll.scroll();
			}
			else if (o instanceof Section) {
				chords.getView().setSection((Section)o);
				chords.getChordSheet().setSection((Section)o);
				SectionCombo.setSection((Section)o);
				ChordPlay.update();
			}
			else if (o instanceof ChordTrack) {
				chords.getView().updateDirectives();
				chords.getChordSheet().updateDirectives();
			}
			else if (o instanceof float[][]) 
				scope.update((float[][])o);
			
			else RTLogger.log(this, "unknown " + o.getClass().getSimpleName() + " update: " + o.toString()); 
		}
	}

	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		mode.setSelectedItem(knobMode);
    	KnobPanel focus = null;
		switch(knobs) {
			case Midi: focus = midiGui; break;
			case Kits: focus = drumMachine.getKnobs(); break;
			case Track: focus = seq.getKnobs(seq.getCurrent()); break;
			case LFO: focus = effects.getChannel().getLfoKnobs(); break;
			case DCO: focus = JudahZone.getSynth1().getSynthKnobs(); break;
			case Samples: focus = sampler.getView(); break;
			case Presets: focus = presets; break;
			case Setlist: focus = setlists; break;
			case Tools: focus = scope;
		}
		knobPanel(focus);
    }
    
    private void knobPanel(final KnobPanel view) {
    	synchronized (knobHolder) {
	    	if (knobs == view) {
	    		knobs.update();
	    		return;
	    	}
	    	if (knobs != null)
	    		knobTitle.remove(knobs.getTitle());
	    	knobs = view;
	    	if (knobMode != view.getKnobMode()) {
	    		knobMode = view.getKnobMode();
	    		mode.setSelectedItem(knobMode);
	    	}
	 
	    	knobTitle.add(view.getTitle());
	    	knobTitle.doLayout();
	    	
	    	knobHolder.removeAll();
	    	knobHolder.add(knobTitle);
	    	knobHolder.add(knobs);
	    	knobHolder.repaint();
	    	Constants.sleep(2 * Constants.GUI_REFRESH);
    	}
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
            Thread.sleep(111); // let nimbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }
	}

	// assign keyboard input
	public static void qwerty() {
		instance.tabs.requestFocusInWindow();
	}

	public void edit(MidiTrack track) {
		if (track.isDrums()) {
			TrackList target;
			if (track == seq.getDrumTracks().get(0) || track == seq.getDrumTracks().get(1))
				target = beatBox.getTop();
			else 
				target = beatBox.getBottom();
			target.setCurrent(track);
			tabs.setSelectedComponent(beatBox);
		}
		else {
			synthBox.getTracks().setCurrent(track);
			tabs.setSelectedComponent(synthBox);
		}
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