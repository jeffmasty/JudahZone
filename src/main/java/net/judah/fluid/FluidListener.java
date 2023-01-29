package net.judah.fluid;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.judah.util.RTLogger;

class FluidListener extends Thread {
	final static String PREFIX = ">";
	final static String JACK = "Jack:";
	final static String PREFIX_JACK = "> Jack: ";
    final ArrayList<FluidPatch> instruments = new ArrayList<FluidPatch>();
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
            				FluidPatch instrument = parseInstrument(line);
            				instruments.add(instrument);
            			} catch (Throwable t) {
            				RTLogger.warn(this, "error reading instrument line " + line + " " + t.getMessage());
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
            	else if (!line.contains("> gain ") && !line.contains("synth.reverb")) 
            		RTLogger.log(this, line);
            }
        } catch (Exception e) {
        	RTLogger.warn(this, e);
        }
    }

    /** @param fluidString <pre>
	  	000-124 Telephone
		000-125 Helicopter
		000-126 Applause
		000-127 Gun Shot
		008-004 Detuned EP 1 </pre>*/
    private FluidPatch parseInstrument(String fluidString) {
			String[] split = fluidString.split(" ");
			String[] numbers = split[0].split("-");
			int group = Integer.parseInt(numbers[0]);
			int index = Integer.parseInt(numbers[1]);
			String name = fluidString.replace(split[0], "").trim();
		return new FluidPatch(group, index, name);
	}

	void sysOverride(FluidCommand cmd) {
		this.sysOverride = cmd;
		if (cmd == FluidCommand.CHANNELS) channels.clear();
		if (cmd == FluidCommand.INST) instruments.clear();
	}

}
