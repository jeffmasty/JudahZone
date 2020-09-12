package net.judah;

import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.fluid.FluidSynth;
import net.judah.jack.JackTab;
import net.judah.midi.MidiClient;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.song.SonglistTab;
import net.judah.util.MenuBar;
import net.judah.util.RTLogger;
import net.judah.util.Tab;


/* Starting my jack sound system: 
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone {

    public static final boolean DEBUG = true;
    static boolean noJack;

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    // TODO
    public static final File defaultSetlist = new File("/home/judah/git/JudahZone/resources/Songs/list1.songs"); 
    public static final File defaultFolder = new File("/home/judah/git/JudahZone/resources/Songs/"); 
    
    
	@Getter private static JFrame frame;
	private static JTabbedPane tabbedPane;
	private static ArrayList<Tab> tabs = new ArrayList<>();
	
	private final Services services = new Services();
	private final CommandHandler commander = new CommandHandler();
	private final Carla effects;
	private final FluidSynth fluid; 
	private final Mixer mixer;
	private final MidiClient midi;
	
    public static void main(String[] args) {

		try {
			new JudahZone();
			while (true) {
				RTLogger.poll();
			}
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

	private JudahZone() throws Throwable {
    	Runtime.getRuntime().addShutdownHook(new ShutdownHook());

    	midi = new MidiClient(commander);
    	services.add(midi);

    	fluid = new FluidSynth(midi);
    	services.add(fluid);
    	
		mixer = new Mixer();
		services.add(mixer);
		
    	effects = new Carla(); 
    	services.add(effects);
    	
    	commander.initializeCommands();
    	
    	Thread.sleep(750);

    	tabs.add(new SonglistTab(defaultSetlist));
    	
		tabs.add(mixer.getGui());
		tabs.add(new JackTab());
    	tabs.add(fluid.getGui());
        startUI();
	}

	private class ShutdownHook extends Thread {

		@Override public void run() {
      	  	for (Service service : services)
      	  		service.close();
		}
	}

    private void startUI() {
        try { UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) { log.info(e.getMessage(), e); }

        //Create and set up the window.
        frame = new JFrame(JUDAHZONE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setJMenuBar(new MenuBar());
        
        JPanel content = (JPanel)frame.getContentPane();
        content.setLayout(new GridLayout(1, 1));

        tabbedPane = new JTabbedPane();
        for (int i = 0; i < tabs.size(); i++) {
        	Tab tab = tabs.get(i);
        	tabbedPane.insertTab(tab.getTabName(), null, tab, "Alt-" + (i+1), i);
        	tabbedPane.setMnemonicAt(i, 0x31 + i); // KeyEvent.VK_1 = 0x31;
        }

        // enables scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        //Display the window.
        content.add(tabbedPane);
        frame.setLocation(50, 50);
        frame.setSize(800, 550);
        frame.setVisible(true);
    }

	public static void openTab(Tab tab) {
    	tabbedPane.insertTab(tab.getTabName(), null, tab, null, tabbedPane.getTabCount());
    	tabbedPane.setSelectedComponent(tab);
    	tabs.add(tab);
	}

	public static void closeTab(Tab tab) {
		tabbedPane.remove(tab);
		tabs.remove(tab);
	}
    
	
}

// unused services
// @Getter private final Rakarrack rack = null; 
// @Getter private final MetroService metronome;
//			metronome = new MetroService();
//			services.add(metronome);
//			rack = new Rakarrack();
//			services.add(rack);
//			Modhost modhost = new Modhost();
//			services.add(modhost);
//			tabs.add(modhost.getGui());

