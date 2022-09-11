package net.judah;

import java.awt.*;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.gui.ControlPanel;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.LoopWidget;
import net.judah.looper.SyncWidget;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiGui;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.tracker.GridTab;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.*;

/** over-all layout and a background updates thread */
public class MainFrame extends JFrame implements Size, Runnable {

    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Object effectsToken = "EFX";
    private static final Dimension clocksz = new Dimension(WIDTH_CLOCK, HEIGHT_MIXER);
    private static final Dimension mix = new Dimension(WIDTH_FRAME - WIDTH_CLOCK - 9, HEIGHT_MIXER);
	private static MainFrame instance;

    private final JTabbedPane tabs = new JTabbedPane();
    private final DJJefe mixer = new DJJefe();
    @Getter private final ControlPanel controls = new ControlPanel();
    @Getter private final GridTab beatBox = new GridTab(); 
    @Getter private final PresetsGui presets = new PresetsGui(JudahZone.getPresets());
    private final MidiGui midiGui = JudahMidi.getInstance().getGui();
    private final Tracker tracker = JudahClock.getTracker();
    private SheetMusicPnl sheetMusic;
    
    MainFrame(String name) {
    	super(name);
        instance = this;
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
        
        midiGui.setMaximumSize(clocksz);
        midiGui.setPreferredSize(clocksz);
        mixer.setMaximumSize(mix);
        mixer.setPreferredSize(mix);
        
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
        top.add(midiGui);
        top.add(mixer);

        tabs.setTabPlacement(JTabbedPane.BOTTOM); 
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setMaximumSize(TABS);
        tabs.setPreferredSize(TABS);
        tabs.add("Tracks", tracker);

        try { setIconImage(Toolkit.getDefaultToolkit().getImage(
        		new File(Constants.ROOT, "icon.png").toURI().toURL()));
        } catch (Throwable t) {
        	RTLogger.log(this, t.getMessage()); }

        JPanel console = new JPanel();
        console.setLayout(new BoxLayout(console, BoxLayout.Y_AXIS));
        JScrollPane output = Console.getInstance().getScroller();
        Dimension d = new Dimension(WIDTH_CONTROLS + 20, 130);
        output.setPreferredSize(d);
        output.setMaximumSize(d);
        console.add(output);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
        left.add(JudahClock.getInstance().getGui());
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
        updates.setPriority(3);
        updates.start();
        
    }

    public void sheetMusic(boolean fwd) {
    	if (sheetMusic == null || sheetMusic.getFile() == null) {
    		MainFrame.get().sheetMusic(new File(Constants.SHEETMUSIC, "four.png"));
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
		    		sheetMusic.doLayout();
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

    public static MainFrame get() {
        return instance;
    }
    
    public static void updateCurrent() {
		updates.offer(effectsToken);
	}

    public static void updateTime() {
    	updates.offer(JudahClock.getInstance());
    }
    
    public static void update(Object o) {
    	updates.offer(o);
    }

    @RequiredArgsConstructor
    private static class Focus extends Thread {
    	private final Object o;
    	@Override public void run() {
	    	if (o instanceof Channel) {
	    		instance.controls.setFocus((Channel)o);
	    		instance.mixer.highlight((Channel)o);
	    	}
	    	else if (o instanceof Track) {
	    		instance.mixer.highlight(null);
	    		MPK.setMode(KnobMode.Tracks);
	    		Tracker.setCurrent((Track)o);
	    	}
	    	else if (o instanceof KnobMode) {
	    		KnobMode knobs = (KnobMode)o;
	    		JudahMidi.getInstance().getGui().mode(knobs);
	    		ControlPanel.getInstance().getTuner().setChannel(null);
	    		if (knobs == KnobMode.Effects1 || knobs == KnobMode.Effects2) {
	    			instance.controls.setBorder(Constants.Gui.HIGHLIGHT);
	    			Tracker.setCurrent(null);
	    			instance.mixer.highlight(instance.controls.getCurrent().getChannel());
	    		}
	    		else {
	    			instance.controls.setBorder(Constants.Gui.NONE);
	    			instance.mixer.highlight(null);
	    			if (knobs == KnobMode.Tracks)
	    				instance.tracker.update(Tracker.getCurrent());
	    			else
	    				Tracker.setCurrent(null);
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

			if (o instanceof Channel) {
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
			else if (o instanceof SyncWidget) 
				((SyncWidget)o).updateLoop();
			else if (o instanceof float[] /*PitchDetectionResult*/) {
				controls.getTuner().process((float[])o);
			}
			else if (o instanceof Track) {
				tracker.update((Track)o);
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
            UIManager.put("nimbusBase", Pastels.EGGSHELL);
            UIManager.put("control", Pastels.EGGSHELL); 
            UIManager.put("nimbusBlueGrey", Pastels.MY_GRAY);
            UIManager.put("ScrollBar.buttonSize", new Dimension(7,7));
        } catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }

	}

    
}
