package net.judah.gui;



import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.controllers.KnobData;
import net.judah.controllers.Qwerty;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.Sample;
import net.judah.drumkit.Sampler;
import net.judah.fx.Gain;
import net.judah.fx.LFO;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.PresetsGui;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.settable.Program;
import net.judah.gui.settable.SetCombo;
import net.judah.gui.widgets.ModalDialog;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.seq.MidiTab;
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
import net.judah.song.SongTrack;
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
	private static final ExecutorService focus = Executors.newFixedThreadPool(10);

	@Getter private static KnobMode knobMode = KnobMode.Midi;
    @Getter private KnobPanel knobs;
	@Getter private final Overview songs;
    @Getter private final Qwerty tabs;
    @Getter private SheetMusicPnl sheetMusic;
    @Getter private final MiniSeq miniSeq;
    @Getter private final HQ hq;

	private final Seq seq;
    private final Looper looper;
    private final MiniLooper loops;
    private final ChordTrack chords;
	private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
	private final JudahMenu menu;
	private final FxPanel effects;
    private final DJJefe mixer;
    private final BeatsTab beatBox;
    private final MidiTab synthBox;
    private final JPanel left = new JPanel(); // clock, midi panel, fx controls
    private final JPanel knobHolder = new JPanel();
    
    public MainFrame(String name, JudahClock clock, FxPanel controls, DJJefe djJefe, 
    		Seq sequencer, Looper looper, Overview songs, ChordTrack chords) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = looper;
        this.seq = sequencer;
        this.songs = songs;
        this.chords = chords;
        synthBox = new PianoTab(seq.getSynthTracks());
    	beatBox = new BeatsTab(seq.getDrumTracks());
    	menu = new JudahMenu(Size.WIDTH_KNOBS, looper);
    	
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setForeground(Color.DARK_GRAY);
        setSize(WIDTH_FRAME, HEIGHT_FRAME);
        setLocation(1, 0);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 2) {
        	JFrame dummy = new JFrame(screens[1].getDefaultConfiguration());
        	setLocationRelativeTo(dummy);
        	dummy.dispose();
        }
        try { setIconImage(Icons.get("icon.png").getImage());
        } catch (Throwable t) { RTLogger.warn(this, t); }

        Gui.resize(mixer, MIXER_SIZE);
        try {
        	sheetMusic = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE);
        } catch (Exception e) { e.printStackTrace(); }

        tabs = new Qwerty(songs, beatBox, synthBox, sheetMusic, chords.getChordSheet());
        tabs.addChangeListener(change->{
			if (tabs.getSelectedComponent() == null) return;
			if (tabs.getSelectedComponent() instanceof MidiTab) {
				MidiTrack track = ((MidiTab)tabs.getSelectedComponent()).getCurrent().getTrack();
				focus(track.getMidiOut());
				focus(seq.getKnobs(track));
			}
			else if (tabs.getSelectedComponent() instanceof Overview) 
				focus(KnobMode.Midi);
			
		});

        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());
		});
        Gui.resize(mode, Size.COMBO_SIZE);
        
        hq = new HQ(clock, looper, songs, chords);
        miniSeq = new MiniSeq(seq.getTracks(), clock);
        loops = new MiniLooper(looper, clock);
        
        knobHolder.setLayout(new BoxLayout(knobHolder, BoxLayout.PAGE_AXIS));
        knobHolder.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        Gui.resize(knobHolder, KNOB_PANEL);

        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(Gui.wrap(menu));
        left.add(hq);
        left.add(Gui.wrap(miniSeq, loops));
        left.add(knobHolder);
        left.add(effects);
        left.add(Gui.wrap(Console.getInstance().getScroller()));
        left.add(Box.createVerticalGlue());
        
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
        
        beatBox.init();
        mixer.updateAll();
        validate();
        setVisible(true);

        Thread updates = new Thread(this);
        updates.setPriority(2);
        updates.start();
        setFocus(KnobMode.Midi);
        tabs.requestFocusInWindow();
    }

    public void sheetMusic(boolean fwd) {
    	if (sheetMusic == null || sheetMusic.getFile() == null) {
    		sheetMusic("Four.png");
    		return;
    	}
    	File[] files = Folders.getSheetMusic().listFiles();
    	File current = sheetMusic.getFile();
    	int i;
    	for (i = 0; i < files.length; i++)
    		if (files[i].equals(current))
    			break;
    	i = i + (fwd ? 1 : -1);
    	if (i >= files.length) 
    		i = 0;
    	sheetMusic(files[i]);
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
		else if (o instanceof SceneLauncher) 
			((SceneLauncher)o).fill();
		else if (o instanceof Scene) {
			songs.getSongView().setCurrent((Scene)o);
			hq.sceneText();
			songs.update();
		} 
		
		else if (o instanceof MidiTrack) {
			RTLogger.log("MainFrame ", "focused MidiTrack " + o);
			// TODO focus TrackKnobs?
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
    	}
    	else if (o instanceof KnobMode) 
    		knobMode((KnobMode)o);
    	
    }

    // every MIDI controller change?
    public static void updateCurrent() {
		if (instance.effects.getChannel() != null)
			updates.offer(instance.effects.getChannel());
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
			if (updates.contains(o))
				continue;
			
			if (o instanceof Channel) {
				Channel ch = (Channel)o;
				mixer.update(ch);
				if (effects.getChannel() == ch)
					effects.getChannel().getGui().update();
				if (knobs instanceof LFOKnobs && ((LFOKnobs)knobs).getChannel() == ch)
					knobs.update();
				else if (ch instanceof MidiReceiver) {
					if (o == seq.getSynthTracks().getCurrent().getMidiOut())
						synthBox.getCurrent().getInstrumentPanel().repaint();
					else if (o == beatBox.getTop().getCurrent().getMidiOut())
						beatBox.midiUpdate(true);
					else if (o == beatBox.getBottom().getCurrent().getMidiOut())
						beatBox.midiUpdate(false);
				}
				else if (ch instanceof DrumSample) { 
					if (knobs instanceof KitKnobs) // turn DrumPad green while playing
						((KitKnobs)knobs).update((DrumSample)o);
				}				
				else if (ch instanceof Sample && knobMode == KnobMode.Samples) 
					JudahZone.getSampler().getView().update((Sample)o);
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
				if (JudahZone.getMidi().getKeyboardSynth() == o)
					JudahZone.getMidiGui().updateSynth();
			}
			else if (o instanceof Looper) 
				loops.getLoopWidget().update();
			else if (o instanceof Sampler) // StepSample changed
    			JudahZone.getMidiGui().update();
			else if (o instanceof JudahClock) {
				hq.length();
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
			else if (o instanceof float[] /*PitchDetectionResult*/) 
				effects.getChannel().getLfoKnobs().getTuner().process((float[])o);
			else if (o instanceof Updateable) 
				((Updateable)o).update();
			else if (o instanceof KnobData) 
				doKnob(((KnobData)o).idx, ((KnobData)o).data2);
			else if (o instanceof LFO) {
				if (effects.getChannel().getLfo() == o)
					// TODO heavy?
					effects.getChannel().getGui().update();
			}
			else if (o instanceof SongView) {
				((SongView)o).update();
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
			
			else if (o instanceof Gain) {
				for (DrumKit kit : JudahZone.getDrumMachine().getKits())
					if (kit.getGain() == o) {
						if (effects.getChannel() == kit)
							kit.getGui().update();
						for (SongTrack t : songs.getTracks())
							if (t.getTrack().getMidiOut() == kit)
								t.update();
					}
			}
			else RTLogger.log(this, "unknown " + o.getClass().getSimpleName() + " update: " + o.toString()); 
			//{mixer.updateAll(); if (effects.getChannel() != null) effects.getChannel().getGui().update();}
		}
	}

	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		mode.setSelectedItem(knobMode);
    	KnobPanel focus = null;
		switch(knobs) {
			case Midi: focus = JudahZone.getMidiGui(); break;
			case Kits: focus = JudahZone.getDrumMachine().getKnobs(); 
				break;
			case Track: focus = seq.getKnobs(seq.getCurrent()); break;
			case LFO: focus = JudahZone.getFxRack().getChannel().getLfoKnobs(); break;
			case DCO: focus = JudahZone.getSynth1().getSynthKnobs(); 
			break;
			case Samples: focus = JudahZone.getSampler().getView(); 
				// setFocus(JudahZone.getSampler());
				break;
			case Presets:
				focus = new PresetsGui(JudahZone.getPresets(), JudahZone.getMidiGui());
				break;
			case Setlist:
				focus = new SetlistView(JudahZone.getSetlists(), JudahZone.getMidiGui());
				break;
		}
		knobPanel(focus);
    }
    
    private void knobPanel(KnobPanel view) {
    	if (knobs == view) {
    		knobs.update();
    		return;
    	}
    	knobs = view;
    	if (knobMode != view.getKnobMode()) {
    		knobMode = view.getKnobMode();
    		mode.setSelectedItem(knobMode);
    	}

    	knobHolder.removeAll();
    	JPanel title = new JPanel();
    	title.setLayout(new BoxLayout(title, BoxLayout.LINE_AXIS));
    	title.add(Gui.wrap(mode));
    	title.add(view.installing());
    	title.doLayout();
    	knobHolder.add(title);
    	knobHolder.add(knobs);
    	left.repaint();
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
            Thread.sleep(111); // let numbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }
	}

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
		focus(seq.getKnobs(track));
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
}
