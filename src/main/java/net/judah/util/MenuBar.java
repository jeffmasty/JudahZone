package net.judah.util;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuBar extends JMenuBar {

    JMenu fileMenu = new JMenu("File");

	JMenuItem load = new JMenuItem("Load");
    JMenuItem create = new JMenuItem("New");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save As");
    JMenuItem exit = new JMenuItem("Exit");
	
	public MenuBar() {

		fileMenu.setMnemonic(KeyEvent.VK_F);
        
		exit.setMnemonic(KeyEvent.VK_E);
        exit.setToolTipText("Exit application");
        exit.addActionListener((event) -> System.exit(0));

        fileMenu.add(load);
        fileMenu.add(create);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(exit);
        
        add(fileMenu);
	}
	
}
