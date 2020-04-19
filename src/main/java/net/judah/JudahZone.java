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
import net.judah.instruments.Rakarrack;
import net.judah.jack.JackUI;
import net.judah.metronome.Metronome;
import net.judah.midi.MidiClient;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.settings.Settings;


/* Starting my jack sound system: 
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone {

    public static final boolean DEBUG = true;
    static boolean noJack;

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();
    
    
    public static final File SOUND_FONT = new File("/usr/share/sounds/sf2/FluidR3_GM.sf2"); // fluid-soundfount-gm package, 150mb
    
    public static final String CARLA_SHELL_COMMAND = "/usr/local/bin/carla ";
    public static final File CARLA_SETTINGS = 
    		new File(JudahZone.class.getClassLoader().getResource("JudahZone.carxp").getFile());
    
    public static final int CARLA_PORT = 22753;
    
    
    public static final File RAKARRACK_SETTINGS = 
    		new File(JudahZone.class.getClassLoader().getResource("JudahZone.rkr").getFile());
    
    @Getter private final Settings settings;
	@Getter private static final Services services = new Services();
	@Getter private final CommandHandler commander;
	@Getter private final Carla effects;
	@Getter private final FluidSynth fluid; 
	@Getter private final Mixer mixer;
    @Getter private final Metronome metronome;
	@Getter private final MidiClient midi;
	@Getter private final Rakarrack rack; 

    ArrayList<Tab> tabs = new ArrayList<>();

    public static void main(String[] args) {

    	if ("true".equals(processArgs("--noJack", args))) 
    		noJack = true;

    	Settings settings = processArgs(args);
		try {
			new JudahZone(settings);
			while (true) {
				RTLogger.poll();
			}
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

	private JudahZone(Settings settings) throws Throwable {
    	this.settings = settings;
    	Runtime.getRuntime().addShutdownHook(new ShutdownHook());

    	commander = new CommandHandler(settings.getMappings());
    	services.add(commander);

    	metronome = new Metronome();
    	services.add(metronome);
    	
    	midi = new MidiClient(commander, metronome);
    	services.add(midi);

    	rack = null;
//    	rack = new Rakarrack(RAKARRACK_SETTINGS.getAbsolutePath());
//    	services.add(rack);
    	
    	fluid = new FluidSynth(midi);
    	services.add(fluid);
    	

		mixer = new Mixer(services, Settings.DEFAULT_SETTINGS.getPatchbay());
		services.add(mixer);
		
    	effects = new Carla(CARLA_SHELL_COMMAND, CARLA_SETTINGS, CARLA_PORT);
    	services.add(effects);
    	
		//		try {
		//			Modhost modhost = new Modhost();
		//			services.add(modhost);
		//			tabs.add(modhost.getGui());
		//		} catch (Exception e) {
		//			log.error(e.getMessage(), e);
		//		}
    	
    	commander.initializeCommands();
    	
    	Thread.sleep(900);

		tabs.add(mixer.getGui());
    	tabs.add(metronome.getGui());
		tabs.add(new JackUI());
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
        JFrame frame = new JFrame("JudahZone");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel content = (JPanel)frame.getContentPane();
        content.setLayout(new GridLayout(1, 1));

        JTabbedPane tabbedPane = new JTabbedPane();
        for (int i = 0; i < tabs.size(); i++) {
        	Tab tab = tabs.get(i);
        	tabbedPane.insertTab(tab.getTabName(), null, tab, "Alt-" + (i+1), i);
        	tabbedPane.setMnemonicAt(i, 0x31 + i); // KeyEvent.VK_1 = 0x31;
        }

        // enables scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        //Display the window.
        content.add(tabbedPane);
        frame.setLocation(70, 70);
        frame.setSize(340, 445);
        frame.setVisible(true);
    }

    static String processArgs(String string, String[] args) {
    	if (args == null) return null;
    	for (int i = 0; i < args.length; i++)
    		if (args[i].equals(string) && i + 1 < args.length)
    				return args[++i];
		return null;
	}

    private static Settings processArgs(String[] args) {
    	// TODO
		return Settings.DEFAULT_SETTINGS;
	}


    
}
