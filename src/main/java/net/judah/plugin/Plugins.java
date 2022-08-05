package net.judah.plugin;

import java.util.ArrayList;

import javax.swing.JPanel;

public class Plugins extends ArrayList<Plugin> {

	private JPanel gui;

    public Plugin byName(String name) {
        for (Plugin p : this) if (p.getName().equals(name)) return p;
        return null;
    }
	
	public JPanel getGui() {
		if (gui == null)
			createGui();
		return gui;
	}

	private void createGui() {
		gui = new JPanel();
		// gui.setLayout(new GridLayout(0, 2));
		for (Plugin plugin : this) { 
			gui.add(new PluginGui(plugin));
		}
	}

}
