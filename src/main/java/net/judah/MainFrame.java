package net.judah;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.MenuBar;

@Log4j
public class MainFrame extends JFrame {
	
	private static MainFrame instance;
	private final JPanel left;
	@Getter private final MixerPane right;
	@Getter private final MenuBar menu;
	private final JTabbedPane tabs;
	private final JPanel content;
	private final String prefix;
	
	MainFrame(String name) {
		super(name);
        try { UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) { log.info(e.getMessage(), e); }
		instance = this;
		prefix = name;
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menu = new MenuBar(); 
        setJMenuBar(menu);
        content = (JPanel)getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.X_AXIS));
        
        left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        left.add(tabs);
        
        left.add(Console.getInstance().getOutput());
        left.add(Console.getInstance().getInput());
        
        content.add(left);
        right = new MixerPane();
        menu.setMixerPane(right);
        content.add(right);

        invalidate();
        setLocation(30, 30);
        setSize(1050, 600); // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);  
        setVisible(true);
	}
	
	public void closeTab(SongPane c) {
		c.getSequencer().stop();
		tabs.remove(c);
	}

	public void openPage(SongPane page) {
		for(int i = 0; i < tabs.getTabCount(); i++)
			if (tabs.getTitleAt(i).contains(Constants.CUTE_NOTE)) 
				tabs.setTitleAt(i, tabs.getTitleAt(i).replace(Constants.CUTE_NOTE, ""));
		String name = Constants.CUTE_NOTE + page.getName();
		tabs.add(name, page);
		tabs.setSelectedComponent(page);
		setTitle(prefix + " - " + page.getName());
		
		invalidate();
		
//		if (page.getSequencer().getSheetMusic() != null) {
//			new SheetMusicTest(page.getSequencer().getSheetMusic()).setVisible(true);
//		}

	}
	
	public static MainFrame get() {
		return instance;
	}
}
