package net.judah.jack;

import java.awt.event.ActionEvent;
import java.util.Properties;

import net.judah.JudahZone;
import net.judah.Tab;
import net.judah.settings.Service;

public class JackUI extends Tab {
	private static final long serialVersionUID = 8233690415021867378L;

	boolean firsttime = true;

	@Override
	public String getTabName() {
		return "Jack";
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String text = super.parseInputBox(e);
		if (text.equalsIgnoreCase("help")) {
			addText("xrun");
		}


		if (text.startsWith("xruns") || text.equalsIgnoreCase("xrun")) {
			for (Service s : JudahZone.getServices())
				if (s instanceof BasicClient) {
					addText(((BasicClient)s).xrunsToString());
					return;
				}
		}

	}

}
