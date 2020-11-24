package net.judah.fluid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import net.judah.util.Console;

class FluidListener extends Thread {
	static final Logger log = Logger.getLogger(FluidListener.class);

	final static String PREFIX = ">";
	final static String JACK = "Jack:";
	final static String PREFIX_JACK = "> Jack: ";
    final ArrayList<FluidInstrument> instruments = new ArrayList<FluidInstrument>();
    final ArrayList<FluidChannel> channels = new ArrayList<FluidChannel>();

	final InputStream is;
    final boolean isError;

    FluidCommand sysOverride;

    FluidListener(InputStream is, boolean isError) {
        this.is = is;
        this.isError = isError;
    }

    @Override
	public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ( (line = br.readLine()) != null) {
            	if (isError) line = "ERROR>> " + line;
            	if (line.startsWith(JACK) || line.startsWith(PREFIX_JACK))
            		continue;
            	if (sysOverride != null) {

            		// System.out.println("---SYS--- " + line);

            		if (line.startsWith("Jack:")) continue;
            		if (line.length() < 10 || line.startsWith(PREFIX)) {
            			if (channels.isEmpty() || instruments.isEmpty()) {
            				// System.out.println("--SYS-- taking no action on line: " + line);
            				continue;
            			}
            			sysOverride = null;
            			continue;
            		}

            		if (sysOverride == FluidCommand.INST)  {
            			try {
            				FluidInstrument instrument = new FluidInstrument(line);
            				instruments.add(instrument);
            			} catch (Throwable t) {
            				log.error("error reading instrument line " + line, t);
            				sysOverride = null;
            			}
            		}
            		if (sysOverride == FluidCommand.CHANNELS) {
            			if (line.contains(FluidCommand.CHANNELS.code)) continue;
            			if (line.contains(FluidCommand.PROG_CHANGE.code)) continue;
            			FluidChannel channel = new FluidChannel(line);
            			channels.add(channel);
            			if (channel.channel == 15) sysOverride = null;
            		}

            	}
            	else {
            		// log.debug("fluid: " + line);
            		Console.addText("fluid: " + line);
            	}
            }
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
    }

	void sysOverride(FluidCommand cmd) {
		this.sysOverride = cmd;
		if (cmd == FluidCommand.CHANNELS) channels.clear();
		if (cmd == FluidCommand.INST) instruments.clear();
	}

}
