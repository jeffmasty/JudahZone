package net.judah.plugin;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import net.judah.util.Console;

class ModhostListener extends Thread {
	static final Logger log = Logger.getLogger(ModhostListener.class);

	final static String PREFIX = ">";
	final static String JACK = "Jack:";
	final static String PREFIX_JACK = "> Jack: ";

	final InputStream is;
    final boolean isError;
    final ModhostUI ui;

    ModhostListener(InputStream is, boolean isError, ModhostUI ui) {
        this.is = is;
        this.isError = isError;
        this.ui = ui;
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
            	
        		Console.addText(line);
            	log.warn("MOD-HOST: " + line);
            }
//            30:35 WARN  ModhostListener:37 - MOD-HOST: mod-host> cpu_load
//            30:35 WARN  ModhostListener:37 - MOD-HOST: resp 0 5.0726
            
            
            
//            	if (sysOverride != null) {
//
//            		// System.out.println("---SYS--- " + line);
//
//            		if (line.startsWith("Jack:")) continue;
//            		if (line.length() < 10 || line.startsWith(PREFIX)) {
//            			if (channels.isEmpty() || instruments.isEmpty()) {
//            				// System.out.println("--SYS-- taking no action on line: " + line);
//            				continue;
//            			}
//            			sysOverride = null;
//            			continue;
//            		}
//
//            		if (sysOverride == FluidCommand.INST)  {
//            			try {
//            				FluidInstrument instrument = new FluidInstrument(line);
//            				instruments.add(instrument);
//            			} catch (Throwable t) {
//            				log.error("error reading instrument line " + line, t);
//            				sysOverride = null;
//            			}
//            		}
//            		if (sysOverride == FluidCommand.CHANNELS) {
//            			if (line.contains(FluidCommand.CHANNELS.code)) continue;
//            			FluidChannel channel = new FluidChannel(line);
//            			channels.add(channel);
//            			if (channel.channel == 15) sysOverride = null;
//            		}
//
//            	}
//            	else {
            		// log.debug("fluid: " + line);
//        		ui.addText(line);
//	            }
            	
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
    }

}
