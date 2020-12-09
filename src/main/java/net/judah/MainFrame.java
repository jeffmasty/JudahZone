package net.judah;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.MenuBar;

public class MainFrame extends JFrame {
	
	private static MainFrame instance;
	private final JTabbedPane tabs;
	private final JPanel content;
	private final String prefix;
	
	private final JPanel left;
	private final RightPane right;
	
	
	MainFrame(String name) {
		super(name);
		instance = this;
		prefix = name;
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(new MenuBar());
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
        right = new RightPane();
        content.add(right);
        
        setLocation(30, 30);
        setSize(1050, 600); // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);  
        setVisible(true);

	}
	
	public void closeTab(Page c) {
		c.getSequencer().stop();
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
		
		right.setSong(page.getSequencer());
	}
	
	public static MainFrame get() {
		return instance;
	}
}
