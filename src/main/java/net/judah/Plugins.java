package net.judah;

import java.util.ArrayList;

import javax.swing.JPanel;

import net.judah.mixer.Plugin;
import net.judah.plugin.PluginGui;

public class Plugins extends ArrayList<Plugin> {
	
	JPanel gui;
	
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
