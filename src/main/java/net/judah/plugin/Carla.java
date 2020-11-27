package net.judah.plugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.Getter;
import lombok.extern.log4j.Log4j;

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
public class Carla {

	@Getter private static Carla instance; 
	
    // public static final String CARLA_SHELL_COMMAND = "/usr/local/bin/carla ";
    public static final String CARLA_SHELL_COMMAND = "carla ";
	
    public static final int CARLA_PORT = 22752;  // 22753
	
    
    
	private static final String PREFIX = "/Carla/";
	
	private static final String OSC = "OSC:"; //log
	
	private String shellCommand;
	private int port;
	private Process process;
	private String cmd;
	private OSCPortOut out;
	

	public Carla(String shellCommand, File carlaSettings, int port) throws IOException {
		this.shellCommand = shellCommand;
		this.port = port;
		cmd = shellCommand + carlaSettings;
		log.info("Opening Carla with: " + cmd + " and using UDP port=" + port);
		process = Runtime.getRuntime().exec(cmd);
		instance = this;
		out = new OSCPortOut(InetAddress.getLocalHost(), port);
		out.connect();
	}
	
	public Carla(File file) throws IOException {
		this(CARLA_SHELL_COMMAND, file, CARLA_PORT);
	}

	/** closes any previous process and reloads Carla with the given project file */
	public void reload(File carlaSettings) throws IOException {
		close();
		cmd = shellCommand + carlaSettings;
		log.info("Opening Carla with: " + cmd + " and using UDP port=" + port);
		process = Runtime.getRuntime().exec(cmd);
		out = new OSCPortOut(InetAddress.getLocalHost(), port);
		out.connect();
		instance = this;
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
	
	
	public void close() {
		if (out != null && out.isConnected()) {
			try {
				out.disconnect();
			} catch (IOException e) {
				log.error(e);
			}
		}
		
		process.destroyForcibly();
		log.warn("-- CARLA DISPOSED --");
		instance = null;
	}
	
}

