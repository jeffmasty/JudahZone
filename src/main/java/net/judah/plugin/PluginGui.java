package net.judah.plugin;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.util.Console;

public class PluginGui extends JPanel {

	public PluginGui(Plugin plugin) {
		JButton btn = new JButton(plugin.getName());
		btn.setToolTipText("Prog Change: " + plugin.getDefaultProgChange());
		btn.addActionListener(listener -> {Console.info(plugin.toString()); });
		add(btn);
		
		JCheckBox active = new JCheckBox();
		active.setSelected(plugin.isActive());
		active.addActionListener(listener -> { plugin.activate(JudahZone.getChannels().getGuitar());});
		
		add(active);
		
	}

}
