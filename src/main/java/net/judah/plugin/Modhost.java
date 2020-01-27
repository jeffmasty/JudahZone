package net.judah.plugin;

import static net.judah.Constants.NL;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import lombok.extern.log4j.Log4j;
import net.judah.Tab;
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
	    log.info("-------------------------------mod host started---------------------------");
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
	public void execute(Command cmd, Properties props) throws Exception {
		
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

	@Override
	public Tab getGui() {
		return ui;
	}
	
	/*
private static LinuxClientSocket mySocket = new LinuxClientSocket("127.0.0.1", 7000);

	public static void main(String[] args)
	{
		mySocket.connectSocket	();
		System.out.println("Connected to the socket");

		String msg = mySocket.getMessage();
		System.out.println("Sent: HELLO");
		System.out.println("Got: " + msg);

		// Raw function; send and receive directly
		// (by default, it is disabled for safety. 
		// To test it, change visibility of sendStringToLinuxSocket() and receiveStringFromLinuxSocket() from public to private)

//		mySocket.sendStringToLinuxSocket("HELLO");
//		msg = mySocket.receiveStringFromLinuxSocket();
//		System.out.println("Sent: HELLO");
//		System.out.println("Got: " + msg);

		mySocket.disconnectSocket();
		System.out.println("DIsconnected from the socket");
	} 
	 */
	
	
}
