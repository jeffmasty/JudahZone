package net.judah.sequencer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.song.Link;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahException;

public class Mappings {

	private final LinkedHashSet<Link> data;

	public Mappings(LinkedHashSet<Link> links) {
		this.data = links;
	}


	/** @return true if consumed */
	public boolean midiProcessed(Midi midiMsg) {
		boolean result = false;
		HashMap<String, Object> p;
		for (Link mapping : data) {

			if (midiMsg.getCommand() == Midi.CONTROL_CHANGE
					&& (byte)midiMsg.getData1() == mapping.getMidi().getData1()
					&& midiMsg.getPort().equals(mapping.getMidi().getPort())) {
			    Console.info(midiMsg.getPort() + " vs port " + mapping.getMidi().getPort());
				p = new HashMap<>();
				p.putAll(mapping.getProps());
				fire(mapping, midiMsg.getData2());
				result = true;
			}

			else if (Arrays.equals(mapping.getMidi().getMessage(), midiMsg.getMessage())) { // Prog Change
				fire(mapping, midiMsg.getData2());
				result = true;
			}
		}
		return result;
	}


	public void fire(Link mapping, int midiData2) {

		new Thread() {
			@Override public void run() {
				Command cmd = mapping.getCmd();
				try {
					if (cmd == null)
						throw new JudahException("Command not found for mapping. " + mapping);
					Console.info("cmdr@" + Sequencer.getCurrent().getCount() + " execute: "
							+ cmd + " " + midiData2 + " " + Constants.prettyPrint(mapping.getProps()));
					cmd.setSeq(Sequencer.getCurrent());
					cmd.execute(mapping.getProps(), midiData2);
				} catch (Exception e) {
					Console.warn(e.getMessage() + " for " + cmd + " with "
							+ Command.toString(mapping.getProps()), e);
				}
			}}.start();
	}

}
