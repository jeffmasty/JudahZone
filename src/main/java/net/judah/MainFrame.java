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
import lombok.extern.log4j.Log4j;
import net.judah.beatbox.BeatsView;
import net.judah.clock.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;
import net.judah.mixer.SyncWidget;
import net.judah.sequencer.Sequencer;
import net.judah.song.SheetMusic;
import net.judah.tracks.Track;
import net.judah.tracks.Tracker;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahMenu;
import net.judah.util.Pastels;
import net.judah.util.Size;

@Log4j
public class MainFrame extends JFrame implements Size, Runnable {

    private static MainFrame instance;
    private final JDesktopPane songPanel;
    private final DJJefe mixer;
    private MusicPanel sheetMusic;
    private final ControlPanel controls;
    @Getter private final Tracker tracker;
   
    //private Overview overview = new Overview();
    
    private final JTabbedPane tabs;
    private final JPanel content;
    private final String prefix;

    private static BlockingQueue<Object> updates = new LinkedBlockingQueue<>();
    private static final Object effectsToken = "EFX";
    
    MainFrame(String name) {
        super(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content = (JPanel)getContentPane();
        content.setBackground(Pastels.EGGSHELL);
        content.setLayout(null);
        
        instance = this;
        prefix = name;
        
        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    Console.info("right click");
                }
            }
        });

        songPanel = new JDesktopPane();
        songPanel.setLayout(null);
        songPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        
        Rectangle footerBounds = new Rectangle(0, HEIGHT_TABS, WIDTH_SONG - 1, HEIGHT_MIXER);
        mixer = new DJJefe(footerBounds);
        
        tabs.setBounds(0, 0, WIDTH_SONG - 1, HEIGHT_TABS);
        
        tracker = JudahClock.getInstance().getTracker();
        tabs.add("Tracks", tracker);
        
        songPanel.add(tabs);
        songPanel.add(mixer);
        songPanel.setBounds(0, 0, WIDTH_SONG, HEIGHT_FRAME);
        controls = new ControlPanel();
        controls.setBounds(WIDTH_SONG + 5, 0, WIDTH_MIXER, HEIGHT_FRAME - 5);
        content.add(songPanel);
        content.add(controls);
        JudahMenu.setMixerPane(controls);
        try { setIconImage(Toolkit.getDefaultToolkit().getImage(
                new File(Constants.ROOT, "icon.png").toURI().toURL()));
        } catch (Throwable t) {log.error(t.getMessage(), t); }
        
        setForeground(Color.DARK_GRAY);
        
        setSize(WIDTH_FRAME, HEIGHT_FRAME);
        // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 2) {
            JFrame dummy = new JFrame(screens[1].getDefaultConfiguration());
            setLocationRelativeTo(dummy);
            dummy.dispose();
        }
        setLocation(1, 0);
        invalidate();
        setVisible(true);
        
        Thread updates = new Thread(this);
        updates.setPriority(4);
        updates.start();
    }
        

    public void beatBox() {
        BeatsView gui = new BeatsView();
        tabs.add("BeatBox", gui);
        tabs.setSelectedComponent(gui);

    }
//    public void beatBox() {
//        BeatBoxGui gui = JudahClock.getInstance().getOld().getGui();
//        tabs.add("BeatBox", gui);
//        tabs.setSelectedComponent(gui);
//    }
//    public void noteBox() {
//        NoteBoxGui gui = JudahClock.getInstance().getNoteBox().getGui();
//        tabs.add("NoteBox", gui);
//        tabs.setSelectedComponent(gui);
//    }

    public void closeTab(SongPane c) {
        c.getSequencer().stop();
        tabs.remove(c);
    }

    public void sheetMusicOff() {
        if (sheetMusic == null) return;
        sheetMusic.setVisible(false);
        songPanel.remove(sheetMusic);
        sheetMusic = null;
    }

    public void sheetMusic() {
        if (Sequencer.getCurrent() == null || Sequencer.getCurrent().getSheetMusic() == null)
            return;
        if (sheetMusic == null) {
            try {
                sheetMusic = new MusicPanel(Sequencer.getCurrent().getSheetMusic());
                sheetMusic.setBounds(tabs.getBounds());
                tabs.setVisible(false);
                songPanel.removeAll();
                songPanel.add(sheetMusic);
                songPanel.add(mixer);
                sheetMusic.doLayout();
            } catch (IOException e) { Console.warn(Sequencer.getCurrent().getSheetMusic().getAbsolutePath(), e); }
        }
        else {
            sheetMusicOff();
            songPanel.removeAll();
            songPanel.add(tabs);
            tabs.setVisible(true);
            songPanel.add(mixer);

        }

        songPanel.doLayout();
        songPanel.invalidate();
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
            labelImage.setPreferredSize(new Dimension(get().songPanel.getWidth(), get().songPanel.getHeight()-200));
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

    public static void setFocus(Object o) {
    	if (o instanceof Channel) {
    		instance.controls.setFocus((Channel)o);
    	}
    	else if (o instanceof Track) {
    		instance.tracker.setFocus((Track)o);
    	}
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
        } catch (Exception e) { log.info(e.getMessage(), e); }

	}




    
}
