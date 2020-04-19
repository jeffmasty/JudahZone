package net.judah.plugin;

import static net.judah.Constants.NL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JTextField;

import net.judah.Tab;

public class ModhostUI extends Tab implements ActionListener {
	private static final long serialVersionUID = -33505833140137161L;
	

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
				addText(history);

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
		addText("" + NL);
	}

	@Override
	public String getTabName() {
		return TAB_NAME;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub
	}


}
