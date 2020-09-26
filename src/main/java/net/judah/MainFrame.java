package net.judah;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import lombok.extern.log4j.Log4j;
import net.judah.sequencer.Sequencer;
import net.judah.util.Constants;
import net.judah.util.MenuBar;

@Log4j
public class MainFrame extends JFrame {
	
	private static MainFrame instance;
	private final JTabbedPane tabs;
	private final JPanel content;
	private final String prefix;
	
	MainFrame(String name) {
		super(name);
		instance = this;
		prefix = name;
		
        try { UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) { log.info(e.getMessage(), e); }

        //Create and set up the window.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(new MenuBar());
        
        content = (JPanel)getContentPane();
        content.setLayout(new GridLayout(1, 1));
        tabs = new JTabbedPane();
        
        // enables scrolling tabs.
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        //Display the window.
        content.add(tabs);
        setLocation(30, 30);
        setSize(1050, 620);  
        setVisible(true);
        // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
	}
	
	public void closeTab(Component c) {
		tabs.remove(c);
	
	}

	public void openPage(Page page) {
		for(int i = 0; i < tabs.getTabCount(); i++)
			if (tabs.getTitleAt(i).contains(Constants.CUTE_NOTE)) 
				tabs.setTitleAt(i, tabs.getTitleAt(i).replace(Constants.CUTE_NOTE, ""));
		String name = Constants.CUTE_NOTE + page.getName();
		tabs.add(name, page);
		tabs.setSelectedComponent(page);
		setTitle(prefix + " - " + page.getName());
		if (JudahZone.getCurrentSong() != null) 
			JudahZone.getCurrentSong().close();
		Sequencer sequencer = page.getSequencer();
		JudahZone.setCurrentSong(sequencer);
		log.debug("loaded song " + sequencer.getSongfile().getAbsolutePath());
		
	}

	public static MainFrame get() {
		return instance;
	}
}
