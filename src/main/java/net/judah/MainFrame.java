package net.judah;

import static net.judah.controllers.KnobMode.*;

import java.awt.*;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lombok.RequiredArgsConstructor;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPKmini;
import net.judah.drumz.DrumSample;
import net.judah.drumz.KitzView;
import net.judah.effects.gui.FxPanel;
import net.judah.looper.LoopWidget;
import net.judah.looper.SyncWidget;
import net.judah.midi.MidiGui;
import net.judah.midi.TimeSigGui;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.samples.Sample;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthEngines;
import net.judah.tracker.GridTab;
import net.judah.tracker.JudahBeatz;
import net.judah.tracker.Track;
import net.judah.util.*;

/** over-all layout and a background updates thread */
public class MainFrame extends JFrame implements Size, Runnable, Pastels {

    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Object effectsToken = "EFX";
    private static final Dimension clocksz = new Dimension(WIDTH_CLOCK, HEIGHT_MIXER);
    private static final Dimension mix = new Dimension(WIDTH_FRAME - WIDTH_CLOCK - 5, HEIGHT_MIXER);
	private static MainFrame instance;

    private final JTabbedPane tabs = new JTabbedPane();
    private final FxPanel controls;
    private final DJJefe mixer;
    private final MidiGui midiGui;
    private final GridTab beatBox;
    private SheetMusicPnl sheetMusic;
    private final JudahBeatz tracker;
    
    MainFrame(String name, FxPanel controls, MidiGui midiGuy, DJJefe djJefe, GridTab beatbox, TimeSigGui timeSig, JudahBeatz tracker) {
    	super(name);
        instance = this;
        this.controls = controls;
        this.midiGui = midiGuy;
        this.mixer = djJefe;
        this.beatBox = beatbox;
        this.tracker = tracker;
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
        try { setIconImage(Toolkit.getDefaultToolkit().getImage(
        		new File(Constants.ROOT, "icon.png").toURI().toURL()));
        } catch (Throwable t) {
        	RTLogger.log(this, t.getMessage()); }
        
        midiGui.setMaximumSize(clocksz);
        midiGui.setPreferredSize(clocksz);
        
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
        top.add(midiGui);
        mixer.setMaximumSize(mix);
        mixer.setPreferredSize(mix);
        top.add(mixer);

        tabs.setTabPlacement(JTabbedPane.BOTTOM); 
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setMaximumSize(TABS);
        tabs.setPreferredSize(TABS);
        
        tabs.add(tracker.getClass().getSimpleName(), tracker);
        tabs.add(beatbox.getName(), beatbox);
        tabs.add(KitzView.NAME, new KitzView());
        tabs.add(SynthEngines.NAME, SynthEngines.getInstance());
        try {
        	sheetMusic = new SheetMusicPnl(new File(Constants.SHEETMUSIC, "Four.png"), tabs.getBounds());
		    tabs.addTab(Constants.CUTE_NOTE + sheetMusic.getName(), sheetMusic);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        tabs.addChangeListener(new ChangeListener() {
        	@Override
			public void stateChanged(ChangeEvent e) {
        		Component selected = tabs.getSelectedComponent();
        		if (selected == SynthEngines.getInstance())
        			MPKmini.setMode(KnobMode.Synth);
        		else if (selected == tracker)
        			MPKmini.setMode(KnobMode.Track);
        }});
        
        JPanel console = new JPanel();
        console.setLayout(new BoxLayout(console, BoxLayout.Y_AXIS));
        JScrollPane output = Console.getInstance().getScroller();
        Dimension d = new Dimension(WIDTH_CONTROLS + 20, 100);
        output.setPreferredSize(d);
        output.setMaximumSize(d);
        console.add(output);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(timeSig);
        left.add(controls);
        left.add(console);
        JPanel bottom = new JPanel();
        bottom.add(left);
        bottom.add(tabs);
        JPanel content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(top);
        content.add(bottom);

        validate();
        setVisible(true);

        Thread updates = new Thread(this);
        updates.setPriority(2);
        updates.start();
        
    }

    public void sheetMusic(boolean fwd) {
    	if (sheetMusic == null || sheetMusic.getFile() == null) {
    		sheetMusic("Four.png");
    		return;
    	}
    	File[] files = Constants.SHEETMUSIC.listFiles();
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
    	sheetMusic(new File(Constants.SHEETMUSIC, s));
    }
    
    public void sheetMusic(File file) { 
    	new Thread(() ->{
	    	try {
		    	if (sheetMusic == null) {
		    		sheetMusic = new SheetMusicPnl(file, tabs.getBounds());
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
    
    public void addOrShow(Component tab, String title) {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (tabs.getComponentAt(i).equals(tab)) {
				tabs.setSelectedComponent(tab);
				return;
			}
		}
		tabs.add(title, tab);
		tabs.setSelectedComponent(tab);
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

	public static void updateCurrent() {
		updates.offer(effectsToken);
	}

    public static void updateTime() {
    	updates.offer(JudahZone.getClock());
    }
    
    public static void update(Object o) {
    	updates.offer(o);
    }

    @RequiredArgsConstructor
    private static class Focus extends Thread {
    	private final Object o;
    	@Override public void run() {
    		if (o instanceof Sample || o instanceof JudahSynth)
				instance.controls.setFocus((Channel)o);
			else if (o instanceof Channel) {
	    		instance.controls.setFocus((Channel)o);
	    		instance.mixer.highlight((Channel)o);
	    		update(o);
	    	}
	    	else if (o instanceof Track) {
	    		instance.mixer.highlight(null);
	    		MPKmini.setMode(KnobMode.Track);
	    		instance.tracker.setCurrent((Track)o);
	    	}
	    	else if (o instanceof KnobMode) {
	    		KnobMode knobs = (KnobMode)o;
	    		MPKmini.getLabel().setText(" " + knobs.name());
	    		MPKmini.getLabel().setBackground(knobs == FX1 ? BLUE : 
					knobs == FX2 ? BLUE.darker() :
					knobs == Clock ? YELLOW : GREEN);
	    		instance.midiGui.mode(knobs);
	    		instance.controls.getTuner().setChannel(null);
	    		if (knobs == KnobMode.FX1 || knobs == KnobMode.FX2) {
	    			instance.controls.setBorder(Constants.Gui.HIGHLIGHT);
	    			instance.tracker.setCurrent(null);
	    			instance.mixer.highlight(instance.controls.getCurrent().getChannel());
	    		}
	    		else {
	    			instance.controls.setBorder(Constants.Gui.NONE);
	    			instance.mixer.highlight(null);
	    			if (knobs == KnobMode.Track)
	    				instance.tracker.update(instance.tracker.getCurrent());
	    			else
	    				instance.tracker.setCurrent(null);
	    		}
	    		instance.invalidate();
	    	}
    	}
    }
    
    public static void setFocus(Object o) {
    	new Focus(o).start();
    }

	@Override
	public void run() {
		Object o = null;
		while (true) {
			
			o = updates.poll();
			if (o == null) {
				Constants.sleep(Constants.GUI_REFRESH);
				continue;
			}
			if (o instanceof TimeSigGui) 
				((TimeSigGui)o).update();
			else if (o instanceof JudahSynth) {
				JudahSynth synth = JudahZone.getSynth();
				synth.getView().update();
				mixer.update(synth);
				if (instance.controls.getChannel() == synth)
					instance.controls.getCurrent().update();
			}
			else if (o instanceof Channel) {
				Channel ch = (Channel)o;
				if (instance.mixer != null)
					instance.mixer.update(ch);
				if (instance.controls.getChannel() == ch)
					instance.controls.getCurrent().update();
			}
			else if (effectsToken == o) {
				instance.controls.getCurrent().update();
				instance.mixer.update(instance.controls.getChannel());
			}
			else if (o instanceof DrumSample) { // turn DrumPad green while playing
				Component c = instance.tabs.getSelectedComponent();
				if (c != instance.beatBox && c instanceof KitzView == false) return;
			}
			else if (o instanceof SyncWidget) 
				((SyncWidget)o).updateLoop();
			else if (o instanceof float[] /*PitchDetectionResult*/) {
				controls.getTuner().process((float[])o);
			}
			else if (o instanceof Track) {
				JudahZone.getTracker().update((Track)o);
			}
			else if (o instanceof LoopWidget) {
				((LoopWidget)o).update();
			}
			else  {
				instance.mixer.updateAll();
				instance.controls.getCurrent().update();
			}
		}
	}

	public static void startNimbus() {
		try {
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("ScrollBar.buttonSize", new Dimension(7,7));
			UIManager.put("nimbusBase", Pastels.EGGSHELL);
            UIManager.put("control", Pastels.EGGSHELL); 
            UIManager.put("nimbusBlueGrey", Pastels.MY_GRAY);
			Thread.sleep(111); // let numbus start up
		} catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }

	}

    
}
