package net.judah.plugin;

import static net.judah.util.Constants.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import lombok.extern.log4j.Log4j;
import net.judah.settings.Command;
import net.judah.settings.Service;

/** ~/lib/mod-host$ ./mod-host -p 7897 -f 7898 */
@Log4j
public class Modhost implements Service {

	private static int instanceNumber = 0;
	static int getInstanceNumber() {
		return instanceNumber++;
	}
	
	final String shellCommand = "mod-host -i";
	private final Process process;
	private final ModhostListener listener;
    private OutputStream toModhost;
	private ModhostUI ui;
    
	public Modhost() throws IOException {
		
		process = Runtime.getRuntime().exec(shellCommand);
	    // read Mod-host's console

		ui = new ModhostUI(this);
	    listener = new ModhostListener(process.getInputStream(), false, ui);
	    new ModhostListener(process.getErrorStream(), true, ui).start();
	    listener.start();
	    toModhost = process.getOutputStream();
	    assert toModhost != null;

	    sendCommand("cpu_load");
	    log.info("-----------mod host started-----------");
	}
	
	void sendCommand(String string) {
		log.debug("sendCommand: " + string);
		if (false == string.endsWith(NL))
			string = string + NL;

		try {
		    toModhost.write((string).getBytes());
		    toModhost.flush();
		} catch(IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	
	@Override
	public String getServiceName() {
		return "Mod-Host";
	}

	@Override
	public List<Command> getCommands() {
		// lv2info http://moddevices.com/plugins/caps/AmpVTS
		return Collections.emptyList();
	}

	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		
	}

	@Override
	public void close() {
		try {
			if (process != null) {
				process.destroy();
				log.info("mod-host destroyed");
			}
		
  	  	} catch (Exception e) { log.warn(e); }

	}

}
