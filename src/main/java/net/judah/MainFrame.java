package net.judah;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.controllers.KnobMode;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.LoopWidget;
import net.judah.looper.SyncWidget;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.sequencer.Sequencer;
import net.judah.song.SheetMusic;
import net.judah.song.SonglistTab;
import net.judah.tracker.GridTab;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;
import net.judah.util.Size;

public class MainFrame extends JFrame implements Size, Runnable {

    private static final BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Object effectsToken = "EFX";

	private static MainFrame instance;

	private final Tracker tracker;
    private final DJJefe mixer;
    private final ControlPanel controls;
    private final JComponent songlist;
    private final JPanel content;
    private final String prefix;
    private MusicPanel sheetMusic;
    @Getter private final JTabbedPane tabs = new JTabbedPane();
    @Getter private GridTab beatBox; 

    
    MainFrame(String name) {
    	super(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        instance = this;
        prefix = name;
        
        mixer = new DJJefe();
        JPanel midiGui = JudahMidi.getInstance().getGui();
        Dimension clocksz = new Dimension(Size.WIDTH_CLOCK, Size.HEIGHT_MIXER);
        midiGui.setMaximumSize(clocksz);
        midiGui.setPreferredSize(clocksz);
        
        Dimension mix = new Dimension(Size.WIDTH_FRAME - Size.WIDTH_CLOCK - 9, Size.HEIGHT_MIXER);
        
        mixer.setMaximumSize(mix);
        mixer.setPreferredSize(mix);
        
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
        top.add(midiGui);
        top.add(mixer);
        // tabs.setTabPlacement(JTabbedPane.BOTTOM); 
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addMouseListener(new MouseAdapter() {
        	@Override public void mousePressed(MouseEvent e) {
        		if (e.getButton() == MouseEvent.BUTTON2) 
        			Console.info("right click");
        	}});
        tabs.setMaximumSize(TABS);
        tabs.setPreferredSize(TABS);

        JudahClock clock = JudahMidi.getClock();
        tracker = clock.getTracker();
        
        beatBox = new GridTab();
        songlist = new SonglistTab(Constants.defaultSetlist);
        controls = new ControlPanel();
        
        tabs.add("Tracks", tracker);
        tabs.add("BeatBox", beatBox);
        
        tabs.add("Setlist", songlist);
        tabs.add("Presets", new PresetsGui(JudahZone.getPresets()));

        content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
        content.add(top);
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.LINE_AXIS));
        content.add(bottom);
        bottom.add(controls);
        bottom.add(tabs);

        try { setIconImage(Toolkit.getDefaultToolkit().getImage(
        		new File(Constants.ROOT, "icon.png").toURI().toURL()));
        } catch (Throwable t) {
        	RTLogger.log(this, t.getMessage()); }

        setForeground(Color.DARK_GRAY);
        setSize(WIDTH_FRAME, HEIGHT_FRAME);
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 2) {
        	JFrame dummy = new JFrame(screens[1].getDefaultConfiguration());
        	setLocationRelativeTo(dummy);
        	dummy.dispose();
        }
        setLocation(1, 0);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        validate();
        setVisible(true);

        Thread updates = new Thread(this);
        updates.setPriority(4);
        updates.start();
        
    }

    public void closeTab(SongPane c) {
        c.getSequencer().stop();
        tabs.remove(c);
    }

    public void sheetMusicOff() {
        if (sheetMusic == null) return;
        sheetMusic.setVisible(false);
        tabs.remove(sheetMusic);
        sheetMusic = null;
    }

    public void sheetMusic() {
        if (Sequencer.getCurrent() == null || Sequencer.getCurrent().getSheetMusic() == null)
            return;
        if (sheetMusic == null) {
            try {
                sheetMusic = new MusicPanel(Sequencer.getCurrent().getSheetMusic());
                sheetMusic.setBounds(tabs.getBounds());
                tabs.addTab("SheetMusic", sheetMusic);
                sheetMusic.doLayout();
            } catch (IOException e) { Console.warn(Sequencer.getCurrent().getSheetMusic().getAbsolutePath(), e); }
        }
        else {
            sheetMusicOff();
        }
        tabs.invalidate();
        Console.getInstance().getInput().grabFocus();
    }

    private class MusicPanel extends JPanel {
        Image image;
        JLabel labelImage;

        MusicPanel(File musicImage) throws IOException {

            image = ImageIO.read(musicImage);
            labelImage = new SheetMusic();
            labelImage.setIcon(new ImageIcon(image));
            labelImage.setIcon(new ImageIcon(image));


            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;

            constraints.gridy = 1;
            constraints.gridx = 0;
            constraints.gridwidth = 3;
            labelImage.setMaximumSize(new Dimension(650, 470));
            labelImage.setPreferredSize(new Dimension(get().tabs.getWidth(), get().tabs.getHeight()-200));
            add(labelImage, constraints);

        }
    }

    public void openPage(SongPane page) {
        for(int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getTitleAt(i).contains(Constants.CUTE_NOTE))
                tabs.setTitleAt(i, tabs.getTitleAt(i).replace(Constants.CUTE_NOTE, ""));
        String name = Constants.CUTE_NOTE + page.getName();
        tabs.add(name, page);
        tabs.setSelectedComponent(page);
        setTitle(prefix + " - " + page.getName());
        // sheetMusic();
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
	    		instance.tracker.setCurrent((Track)o);
	    	}
	    	else if (o instanceof KnobMode) {
	    		KnobMode knobs = (KnobMode)o;
	    		JudahMidi.getInstance().getGui().mode(knobs);
	    		ControlPanel.getInstance().getTuner().setChannel(null);
	    		if (knobs == KnobMode.Effects1 || knobs == KnobMode.Effects2) {
	    			instance.controls.setBorder(Constants.Gui.HIGHLIGHT);
	    			instance.tracker.setCurrent(null);
	    			instance.mixer.highlight(instance.controls.getCurrent().getChannel());
	    		}
	    		else {
	    			instance.controls.setBorder(Constants.Gui.NONE);
	    			instance.mixer.highlight(null);
	    			if (knobs == KnobMode.Tracks)
	    				instance.tracker.setCurrent(Tracker.getCurrent());
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
	
	
	public static void startNimbus() {
		try {
			
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            UIManager.put("nimbusBase", Pastels.EGGSHELL);
            UIManager.put("control", Pastels.EGGSHELL); 
            UIManager.put("nimbusBlueGrey", Pastels.MY_GRAY);
        } catch (Exception e) { RTLogger.log(MainFrame.class, e.getMessage()); }

	}






    
}
