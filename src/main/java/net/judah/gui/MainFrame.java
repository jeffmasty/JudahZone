package net.judah.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
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
import net.judah.fx.LFO;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.fx.Row;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.knobs.SampleKnobs;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.LoopWidget;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.mixer.LoopMix;
import net.judah.seq.MidiTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.beatbox.BeatsTab;
import net.judah.seq.piano.PianoTab;
import net.judah.song.Scene;
import net.judah.song.ScenesView;
import net.judah.song.SongTab;
import net.judah.song.SongView;
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
	@Getter private final SongTab songs;
    @Getter private final Qwerty tabs;
    @Getter private SheetMusicPnl sheetMusic;
    @Getter private final MiniSeq miniSeq;

	private final Seq seq;
    private final Looper looper;
    private final MiniLooper loops;
	private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
	private final JudahMenu menu;
	private final FxPanel effects;
    private final DJJefe mixer;
    private final BeatsTab beatBox;
    private final MidiTab synthBox;
    private final JPanel left = new JPanel(); // clock, midi panel, fx controls
    private final JPanel knobHolder = new JPanel();
    
    public MainFrame(String name, FxPanel controls, DJJefe djJefe, Seq sequencer, Looper looper, SongTab songs) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = looper;
        this.seq = sequencer;
        this.songs = songs;

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
        
        mixer.setMaximumSize(MIXER_SIZE);
        mixer.setPreferredSize(MIXER_SIZE);
        mixer.setMinimumSize(MIXER_SIZE);
        try {
        	sheetMusic = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE);
        } catch (Exception e) { e.printStackTrace(); }

        tabs = new Qwerty(songs, beatBox, synthBox, sheetMusic);
        beatBox.init();
        
        mode.setFont(Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());
		});
        Gui.resize(mode, Size.COMBO_SIZE);
        
        miniSeq = new MiniSeq(seq.getTracks(), JudahZone.getClock(), songs);
        loops = new MiniLooper(looper, JudahZone.getClock());
        JPanel headquarters = new JPanel();
        headquarters.setLayout(new BoxLayout(headquarters, BoxLayout.LINE_AXIS));
        headquarters.add(loops);
        headquarters.add(miniSeq);
        knobHolder.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        knobHolder.setPreferredSize(KNOB_PANEL);
        knobHolder.setMaximumSize(KNOB_PANEL);
        knobHolder.setMinimumSize(KNOB_PANEL);
        knobHolder.setLayout(new BoxLayout(knobHolder, BoxLayout.PAGE_AXIS));

        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(Gui.wrap(menu));
        left.add(headquarters);
        left.add(knobHolder);
        left.add(effects);
        left.add(Console.getInstance().getScroller());
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
		else if (o instanceof DrumKit) {
			KitKnobs knobs = JudahZone.getDrumMachine().getKnobs();
			knobs.setKit((DrumKit)o);
			knobPanel(knobs);
		}
		
		if (o instanceof Channel) {
    		effects.setFocus((Channel)o);
    		mixer.highlight((Channel)o);
    		update(o);
    	}
		else if (o instanceof MultiSelect) {
			ArrayList<Channel> multiselect = (MultiSelect)o;
			Channel next = multiselect.get(multiselect.size()-1);
			mixer.highlight(multiselect);
			effects.addFocus(next);
		}
		else if (o instanceof ScenesView) 
			((ScenesView)o).fill();
		else if (o instanceof Scene) {
			songs.getSongView().setCurrent((Scene)o);
			miniSeq.sceneText();
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
    	else if (o instanceof KnobMode) {
    		knobMode((KnobMode)o);
    	}
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
			
			if (o instanceof Sampler) { // StepSample changed
    			JudahZone.getMidiGui().update();
    			continue;
    		}
			if (o instanceof Sample) {// will also hit channel update
				if (knobMode == KnobMode.Samples) 
					((SampleKnobs)knobs).update((Sample)o);
			}
			else if (o instanceof DrumSample) { // turn DrumPad green while playing
				if (knobs instanceof KitKnobs)
					((KitKnobs)knobs).update((DrumSample)o);
				if (tabs.getSelectedComponent() == beatBox) {
					beatBox.update((DrumSample)o);
				}
				if (effects.getChannel() == o)
					effects.getChannel().getGui().update(); 
			}
			else if (o instanceof JudahClock) {
				if (knobMode == KnobMode.Midi) 
					knobs.update();
				miniSeq.update();
				loops.update();
				mixer.update(looper.getLoopA()); // update loop A
			}
			else if (o instanceof Program) {
				Program prog = (Program)o;
				Program.update(prog);
				if (prog.getPort() instanceof JudahSynth)
					((JudahSynth)prog.getPort()).getSynthKnobs().update();
			}
			
			else if (o instanceof MidiReceiver) {
				if (o == seq.getSynthTracks().getCurrent().getMidiOut())
					synthBox.getCurrent().getInstrumentPanel().repaint();
				else if (o == beatBox.getTop().getCurrent().getMidiOut())
					beatBox.midiUpdate(true);
				else if (o == beatBox.getBottom().getCurrent().getMidiOut())
					beatBox.midiUpdate(false);
				if (knobMode == KnobMode.Kits && o == ((KitKnobs)knobs).getKit())
					knobs.update();
			}
			else if (o instanceof MidiTrack) {
				MidiTrack t = (MidiTrack)o;
				miniSeq.update(t);
				if (knobMode == KnobMode.Track)
					knobs.update();
				synthBox.update(t);
				beatBox.update(t);
				songs.update(t);
			}
			else if (o instanceof Scene) {
				songs.getSongView().getLauncher().update((Scene)o);
				miniSeq.sceneText();
			}
			else if (o == seq || o == songs)
				songs.update();
			
			if (o instanceof Channel) {
				Channel ch = (Channel)o;
				mixer.update(ch);
				if (effects.getChannel() == ch)
					effects.getChannel().getGui().update();
				if (knobs instanceof LFOKnobs && ((LFOKnobs)knobs).getChannel() == ch)
					knobs.update();
			}

			else if (o == mixer)
				mixer.updateAll();
			else if (o instanceof float[] /*PitchDetectionResult*/) {
				effects.getChannel().getLfoKnobs().getTuner().process((float[])o);
			}
			else if (o instanceof LoopWidget) {
				((LoopWidget)o).update();
			}
			else if (o instanceof LoopMix) {
				((LoopMix)o).update();
			}
			else if (o instanceof KnobData) {
				doKnob(((KnobData)o).idx, ((KnobData)o).data2);
			}
			else if (o instanceof LFO) {
				if (effects.getChannel().getLfo() == o)
					effects.getChannel().getGui().update();
			}
			else if (o instanceof Row)
				((Row)o).update();
			else if (o instanceof KnobPanel) 
				((KnobPanel)o).update();
			else if (o instanceof SongView) {
				((SongView)o).update();
				miniSeq.sceneText();
			}
			else  {
				mixer.updateAll();
				if (effects.getChannel() != null)
					effects.getChannel().getGui().update();
			}

		}
	}

	private void knobMode(KnobMode knobs) {
    	knobMode = knobs;
		KnobPanel focus = null;
		switch(knobs) {
			case Midi: focus = JudahZone.getMidiGui(); break;
			case Kits: focus = JudahZone.getDrumMachine().getKnobs(); 
				break;
			case Track: focus = seq.getKnobs(seq.getCurrent()); break;
			case LFO: focus = JudahZone.getFxRack().getChannel().getLfoKnobs(); break;
			case Synth: focus = JudahZone.getSynth1().getSynthKnobs(); 
			break;
			case Samples: focus = new SampleKnobs(JudahZone.getSampler()); 
				setFocus(JudahZone.getSampler());
				break;
		}
		mode.setSelectedItem(knobMode);
		if (focus != null)
			knobPanel(focus);
    }
    
    private void knobPanel(KnobPanel view) {
    	if (knobs == view) {
    		knobs.update();
    		return;
    	}
    	knobs = view;
    	knobHolder.removeAll();
    	knobHolder.invalidate();
    	JPanel title = new JPanel();
    	title.setLayout(new BoxLayout(title, BoxLayout.LINE_AXIS));
    	title.add(Box.createHorizontalStrut(4));
    	title.add(Gui.wrap(mode));
    	title.add(view.installing());
    	knobHolder.add(title);
    	knobHolder.add(knobs);
    	left.invalidate();
    	left.repaint();
    }
    
	private boolean doKnob(int idx, int data2) {
		if (knobs != null) {
			if (knobs.doKnob(idx, data2)) {
				MainFrame.update(knobs);
				return true;
			}
		}
		return false;
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
	}

	
}
