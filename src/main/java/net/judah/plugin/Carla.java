package net.judah.plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.util.Tab;

// /home/judah/git/JudahZone/JudahZone/JudahZone.carxp
// /usr/local/share/applications

/**
<pre>
  	OSC Implemented:
/set_active					<i-value>
/set_parameter_value 		<i-index> <f-value>
/set_volume 		 		<f-value>

	TODO:
/set_drywet 				<f-value>
/set_balance_left	 		<f-value>
/set_balance_right   		<f-value>
/set_panning 		 		<f-value>
/set_parameter_midi_cc		<i-index> <i-cc>
/set_parameter_midi_channel	<i-index> <i-channel>
/set_program 				<i-index>
/set_midi_program 			<i-index>
/note_on   					<i-channel> <i-note> <i-velo>
/note_off  					<i-channel> <i-note
</pre>
 */
@Log4j
public class Carla implements Service {

    public static final String CARLA_SHELL_COMMAND = "/usr/local/bin/carla ";
    public static final File CARLA_SETTINGS = 
    		new File(JudahZone.class.getClassLoader().getResource("JudahZone.carxp").getFile());
    
    public static final int CARLA_PORT = 22753;
	
	private static final String PREFIX = "/Carla/";
	private static final String OSC = "OSC:"; //log
	
	private final Process process;
	private final String cmd;
	private final OSCPortOut out;

	public Carla(String shellCommand, File carlaSettings, int port) throws IOException {
			cmd = shellCommand + carlaSettings;
			log.info("Opening Carla with: " + cmd + " and using UDP port=" + port);
			process = Runtime.getRuntime().exec(cmd);
			out = new OSCPortOut(InetAddress.getLocalHost(), port);
			out.connect();
	}
	
	public Carla() throws IOException {
		this(CARLA_SHELL_COMMAND, CARLA_SETTINGS, CARLA_PORT);
	}

	/** @return true if message sent */
	public boolean setVolume(int pluginIdx, float gain) {
		assert out.isConnected();
		List<Object> param = new ArrayList<>();
		param.add(gain);
		String address = PREFIX + pluginIdx + "/set_volume";
		log.debug(OSC + address + " --> " + gain);
		try {
			out.send(new OSCMessage(address, param));
			return true;
		} catch (IOException | OSCSerializeException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	/** @return true if message sent */
	public boolean setActive(int pluginIdx, int tOrF) {
		assert out.isConnected();
		List<Object> param = new ArrayList<>();
		param.add(tOrF);
		String address = PREFIX + pluginIdx + "/set_active";
		log.info(OSC + address + " --> " + tOrF);
		try {
			out.send(new OSCMessage(address, param));
			return true;
		} catch (IOException | OSCSerializeException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}

	/** @return true if message sent */
	public boolean setParameterValue(int pluginIdx, int paramIdx, float value) {
		assert out.isConnected();
		String address = PREFIX + pluginIdx + "/set_parameter_value";
		List<Object> param = new ArrayList<>();
		param.add(paramIdx);
		param.add(value);
		log.info(OSC + address + " param " + paramIdx + " --> " + value);
		try {
			out.send(new OSCMessage(address, param));
			return true;
		} catch (IOException | OSCSerializeException e) {
			log.error(e.getMessage(), e);
			return false;
		}
	}
	
	public void send(String address, List<Object> params) throws OSCSerializeException, IOException {
		
		out.send(new OSCMessage(address, params));
		//		List<Object> param = new ArrayList<>();
		//		param.add(0.1f);
		//		OSCMessage message = new OSCMessage("/Carla/1/set_volume", param);
		//		out.send(message);
	}
	
	
	@Override
	public String getServiceName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public List<Command> getCommands() {
		return Collections.emptyList(); // TODO
	}

	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		if (out != null && out.isConnected()) {
			try {
				out.disconnect();
			} catch (IOException e) {
				log.error(e);
			}
		}
		process.destroy();
	}

	@Override
	public Tab getGui() {
		return null;
	}
	
}

