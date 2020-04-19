package net.judah.plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.illposed.osc.OSCBadDataEvent;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPacketEvent;
import com.illposed.osc.OSCPacketListener;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.extern.log4j.Log4j;
import net.judah.Tab;
import net.judah.settings.Command;
import net.judah.settings.Service;

// /home/judah/git/JudahZone/JudahZone/JudahZone.carxp
@Log4j
public class Carla implements Service {

	private Process process;
	public static final String SHELL_COMMAND = "carla ";
	public static final File DEFAULT_FILE = new File("/home/judah/git/JudahZone/JudahZone/JudahZone.carxp");
	String cmd;
	
	public Carla(File carlaSettings, int port) throws IOException, OSCSerializeException {
		
		cmd = SHELL_COMMAND + carlaSettings;
		process = Runtime.getRuntime().exec(cmd);

		OSCPortIn in = new OSCPortIn(port); // 11589
		// OSCPortIn in = new OSCPortIn(10086);
		
		in.addPacketListener(new OSCPacketListener() {
			@Override
			public void handlePacket(OSCPacketEvent event) {
				log.warn("CARLA: " + event.toString());
			}
			
			@Override
			public void handleBadData(OSCBadDataEvent event) {
				log.error("CARLA ERR: " + event);
			}
		});
		
		OSCPortOut out = new OSCPortOut(InetAddress.getLocalHost(), 11589);
		// OSCPortOut out = new OSCPortOut(InetAddress.getLocalHost(), 11589);
		List<Object> param = new ArrayList<>();
		param.add(0.1f);
		
		OSCMessage message = new OSCMessage("/Carla/1/set_volume", param);
		out.send(message);
	}
	
	public static void main(String[] args) {
		try {
			new Carla(DEFAULT_FILE, 11589);
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
	}

	@Override
	public String getServiceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public List<Command> getCommands() {
		return Collections.emptyList();
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		process.destroy();
	}

	@Override
	public Tab getGui() {
		return null;
	}
	
}
