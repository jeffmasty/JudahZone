package net.judah.gui;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
import javax.swing.border.LineBorder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.controllers.KnobData;
import net.judah.controllers.KnobMode;
import net.judah.controllers.Knobs;
import net.judah.controllers.Qwerty;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.effects.LFO;
import net.judah.effects.gui.FxPanel;
import net.judah.effects.gui.MultiSelect;
import net.judah.effects.gui.Row;
import net.judah.gui.knobs.KitKnobs;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.knobs.LFOKnobs;
import net.judah.gui.knobs.SamplerKnobs;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.samples.Sample;
import net.judah.samples.Sampler;
import net.judah.seq.EditTab;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.synth.JudahSynth;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.Icons;
import net.judah.util.RTLogger;
import net.judah.widgets.LoopWidget;
import net.judah.widgets.SyncWidget;

/** over-all layout and a background updates thread */
public class MainFrame extends JFrame implements Size, Runnable, Pastels {

    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
	private static MainFrame instance;

	@Getter private static KnobMode knobMode = KnobMode.Midi;
    private final JComboBox<KnobMode> mode = new JComboBox<>(KnobMode.values());
	@Getter private final JTabbedPane tabs = new JTabbedPane();
    private final FxPanel effects;
    private final DJJefe mixer;
    private final EditTab beatBox;
    private final EditTab synthBox;
    
    private final Seq seq;
    private final Looper looper;
    private final MiniSeq miniSeq;
    private final MiniLooper loops;
    @Getter private SheetMusicPnl sheetMusic;
    private final JPanel left = new JPanel(); // clock, midi panel, fx controls
    private final JPanel knobHolder = new JPanel();
    @Getter private KnobPanel knobs;
    
    public MainFrame(String name, FxPanel controls, DJJefe djJefe, Seq sequencer, Looper looper) {
    	super(name);
        instance = this;
        this.effects = controls;
        this.mixer = djJefe;
        this.looper = looper;
        this.seq = sequencer;

        synthBox = new EditTab(seq.getSynthTracks());
    	beatBox = new EditTab(seq.getDrumTracks());

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
        try { setIconImage(Icons.load("icon.png").getImage());
        } catch (Throwable t) { RTLogger.warn(this, t); }
        
        mixer.setMaximumSize(MIXER_SIZE);
        mixer.setPreferredSize(MIXER_SIZE);
        mixer.setMinimumSize(MIXER_SIZE);
        tabs.setMaximumSize(TAB_SIZE);
        tabs.setPreferredSize(TAB_SIZE);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.add(Seq.NAME, seq);
        tabs.add(beatBox.getTracks().getCurrent().getName(), beatBox);
        tabs.add(synthBox.getTracks().getCurrent().getName(), synthBox);
        try {
        	sheetMusic = new SheetMusicPnl(new File(Folders.getSheetMusic(), "Four.png"), TAB_SIZE);
		    tabs.addTab(Constants.CUTE_NOTE + sheetMusic.getName(), Constants.wrap(sheetMusic));
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        JScrollPane output = Console.getInstance().getScroller();
        Dimension d = new Dimension(WIDTH_KNOBS - 4, 60);
        output.setPreferredSize(d);
        output.setMaximumSize(d);
        knobHolder.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        knobHolder.setPreferredSize(KNOB_PANEL);
        knobHolder.setMaximumSize(KNOB_PANEL);
        knobHolder.setMinimumSize(KNOB_PANEL);
        knobHolder.setLayout(new BoxLayout(knobHolder, BoxLayout.PAGE_AXIS));
        mode.setFont(Constants.Gui.BOLD13);
        mode.addActionListener(e->{
			if (knobMode != mode.getSelectedItem())
				setFocus(mode.getSelectedItem());
		});
        Constants.resize(mode, Size.COMBO_SIZE);
        
        miniSeq = new MiniSeq(seq.getTracks(), seq.getClock());
        loops = new MiniLooper(looper, seq.getClock());
        JPanel headquarters = new JPanel();
        headquarters.setLayout(new BoxLayout(headquarters, BoxLayout.LINE_AXIS));
        headquarters.add(miniSeq);
        headquarters.add(loops);
        
        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(Constants.wrap(new JudahMenu(Size.WIDTH_KNOBS, looper)));
        left.add(headquarters);
        left.add(knobHolder);
        left.add(effects);
        left.add(output);
        
        JPanel right = new JPanel(); // mixer & main view
        right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
        right.add(mixer);
        right.add(tabs);
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
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new Qwerty(seq));
        setFocus(KnobMode.Midi);
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
    	
    	new Thread(() ->{
	    	try {
		    	if (sheetMusic == null) {
		    		sheetMusic = new SheetMusicPnl(file, Size.TAB_SIZE);
		    		tabs.addTab(Constants.CUTE_NOTE + sheetMusic.getName(), sheetMusic);
		    		tabs.setSelectedComponent(sheetMusic);
		    	}
		    	else {
		    		sheetMusic.setImage(file);
		    		tabs.setSelectedComponent(sheetMusic);
		    		for (int i = 0; i < tabs.getTabCount(); i++) {
		    			if (tabs.getComponentAt(i) == sheetMusic)
		    				tabs.setTitleAt(i, Constants.CUTE_NOTE + sheetMusic.getName());
		    		}
		    	}
	    	} catch (Throwable e) {
	    		RTLogger.warn(this, file.getAbsolutePath() + " " + e.getMessage());
	    	}
    	}).run();
    }

    public Component getTab() {
    	return instance.tabs.getSelectedComponent();
    }
    
    public void addOrShow(Component tab, String title) {
    	if (tab instanceof Knobs)
    		setFocus(((Knobs)tab).getKnobMode());
    	new Thread(() ->{
			for (int i = 0; i < tabs.getTabCount(); i++) {
				if (tabs.getComponentAt(i).equals(tab)) {
					tabs.setSelectedComponent(tab);
					return;
				}
			}
			tabs.add(title, tab);
			tabs.setSelectedComponent(tab);
		}).start();
	}
	
    public void closeTab(Component tab) {
        tabs.remove(tab);
        if (tab instanceof SheetMusic)
        	sheetMusic = null;
    }

	public static void changeTab(boolean fwd) {
		new Thread(() -> instance.tab(fwd)).start();
	}

	private void tab(boolean fwd) {
		int idx = tabs.getSelectedIndex() + (fwd ? 1 : -1);
		if (idx >= tabs.getTabCount())
			idx = 0;
		if (idx < 0)
			idx = tabs.getTabCount() - 1;
		tabs.setSelectedIndex(idx);
	}

    public static void setFocus(Object o) {
    	new Focus(o).start();
    }
    
    @RequiredArgsConstructor
    private static class Focus extends Thread {
    	private final Object o;
    	@Override public void run() {
    		if (o instanceof KnobMode) {
    			instance.knobMode((KnobMode)o);
    		}
    		else if (o instanceof KnobPanel) {
    			instance.knobPanel((KnobPanel)o);
    		}
    		else if (o instanceof DrumKit) {
    			instance.knobPanel( ((DrumKit)o).getKnobs());
    		}
    		
    		if (o instanceof Channel) {
	    		instance.effects.setFocus((Channel)o);
	    		instance.mixer.highlight((Channel)o);
	    		update(o);
	    	}
    		else if (o instanceof MultiSelect) {
    			ArrayList<Channel> multiselect = (MultiSelect)o;
    			Channel next = multiselect.get(multiselect.size()-1);
    			instance.mixer.highlight(multiselect);
    			instance.effects.addFocus(next);
    		}
	    	else if (o instanceof MidiTrack) {
//	    		instance.seq.setCurrent((MidiTrack)o);
	    	}
	    	else if (o instanceof TrackList) {
	    		TrackList t = (TrackList)o;
	    		if (o == instance.seq.getDrumTracks()) {
	    			instance.beatBox.changeTrack(t.getCurrent().getView());
	    		}
	    		else if (o == instance.seq.getSynthTracks()) {
	    			instance.synthBox.changeTrack(t.getCurrent().getView());
	    		}
	    		else if (o == instance.seq.getTracks()) {
	    			instance.miniSeq.update();
	    			if (knobMode == KnobMode.Track)
	    				instance.knobPanel(instance.seq.getKnobs(t.getCurrent()));
	    		}
	    	}
	    	else if (o instanceof KnobMode) {
	    		instance.knobMode((KnobMode)o);
	    	}
    	}
    }
    
    public static void updateCurrent() {
		if (instance.effects.getChannel() != null)
			updates.offer(instance.effects.getChannel());
	}

    public static void updateTime() {
    	updates.offer(JudahZone.getClock());
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
			if (o instanceof Sample) // will also hit channel update
				if (knobMode == KnobMode.Samples) {
					((SamplerKnobs)knobs).update((Sample)o);
				}
			if (o instanceof DrumSample) { // turn DrumPad green while playing
				if (knobs instanceof KitKnobs)
					((KitKnobs)knobs).update((DrumSample)o);
				// TODO
//				else if (beatbox.isShowing() && tracker.getCurrent() instanceof DrumTrack) {
//					DrumTrack track = (DrumTrack)tracker.getCurrent();
//					for (DrumSample s : track.getKit().getSamples()) {
//						if (s == o)
//							((DrumEdit)track.getEdit()).getKit().update(s);
//					}
//				}
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

			if (o instanceof MidiReceiver) {
				if (o == seq.getSynthTracks().getCurrent().getMidiOut())
					seq.getSynthTracks().getCurrent().getView().getInstrumentPanel().repaint();
						
				else if (o == seq.getDrumTracks().getCurrent().getMidiOut())
					seq.getDrumTracks().getCurrent().getView().getInstrumentPanel().repaint();
					
				else if (knobMode == KnobMode.Kits && o == ((KitKnobs)knobs).getKit())
					knobs.update();
			}
			else if (o instanceof MidiTrack) {
				instance.miniSeq.update();
				if (knobMode == KnobMode.Track)
					knobs.update();
				if (((MidiTrack) o).viewVisible())
					((MidiTrack) o).getView().update();
			}
			else if (o == seq)
				seq.update();
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
			else if (o instanceof SyncWidget) 
				((SyncWidget)o).update();
			else if (o instanceof float[] /*PitchDetectionResult*/) {
				effects.getChannel().getLfoKnobs().getTuner().process((float[])o);
			}
			else if (o instanceof LoopWidget) {
				((LoopWidget)o).update();
			}
			else if (o instanceof JudahSynth) {
				mixer.update((JudahSynth)o);
				if (effects.getChannel() == o)
					effects.getChannel().getGui().update();
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
			case Kits: focus = JudahZone.getDrumMachine().getActive().getKnobs(); 
				break;
			case Track: focus = seq.getKnobs(seq.getCurrent()); break;
			case LFO: focus = JudahZone.getFxRack().getChannel().getLfoKnobs(); break;
			case Synth: focus = JudahZone.getSynth1().getSynthKnobs(); 
			break;
			case Samples: focus = new SamplerKnobs(JudahZone.getSampler()); 
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
    	title.add(Constants.wrap(mode));
    	title.add(view.installing());
    	knobHolder.add(title);
    	knobHolder.add(knobs);
    	left.invalidate();
    	left.repaint();
    }
    
	private boolean doKnob(int idx, int data2) {
		if (knobs != null) {
			boolean result = knobs.doKnob(idx, data2);
			if (result)
				MainFrame.update(knobs);
			return result;
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
            UIManager.getLookAndFeel().getDefaults().put("Button.contentMargins", new Insets(6, 8, 6, 8));

            Thread.sleep(111); // let numbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }

	}
    
}
