package net.judah.instruments;

import java.awt.event.ActionEvent;
import java.util.Properties;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.util.Tab;


public class Pedal extends Tab {

    public static final String NAME = "ActitioN MIDI Controller";

	private final JackPort pedalIn;

	public Pedal(JackPort pedalIn) {
		this.pedalIn = pedalIn;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTabName() {
		return "Pedal";
	}

	@Override
	public void setProperties(Properties p) {

	}

	public JackPort getPort() {
		return pedalIn;
	}

}



