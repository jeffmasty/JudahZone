package net.judah.plugin;

import static net.judah.util.Constants.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import net.judah.util.Console;

public class ModhostUI implements ActionListener {
	public static final String TAB_NAME = "Mod-Host";

	final Modhost modhost;
	
	private String history; // TODO add to parent Tab

	public ModhostUI(Modhost modhost) {
		this.modhost = modhost;
	}

	@Override
	public void actionPerformed(ActionEvent event) {

		if (event.getSource() instanceof JTextField) {
			JTextField widget = (JTextField)event.getSource();
			String text = widget.getText();
			if (text.isEmpty() && history != null) {
				Console.addText(history);

				modhost.sendCommand(text);
				
			}
			else {
				history = text;
				modhost.sendCommand(text);
			}
			widget.setText("");
		}
	}

	//** output to console */
	public void newLine() {
		Console.addText("" + NL);
	}


}
