package net.judah.plugin;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Service;

// /usr/local/share/applications

/**
 * 
 * Use UDP Port
 * 
<pre>
  	OSC Implemented:
/set_active					<i-value>
/set_parameter_value 		<i-index> <f-value>
/set_volume 		 		<f-value>
/set_drywet 				<f-value>
 
	TODO:
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

	@Getter private static Carla instance; 
	
    // public static final String CARLA_SHELL_COMMAND = "/usr/local/bin/carla ";
    public static final String CARLA_SHELL_COMMAND = "carla";
	
	private static boolean isFirst = true; // adjust prefix
    
	@Override
	public String getServiceName() { return Carla.class.getSimpleName(); }
	
	private final String prefix;
	@Getter private final CarlaCommands commands = new CarlaCommands(this);
	
	private static final String OSC = "OSC:"; //log
	
	@Getter private final String settings;
	private final int port;
	private Process process;
	private OSCPortOut out;

	// default ports
	public Carla(String carlaSettings, boolean showGui) throws IOException {
		this(carlaSettings, 11176, 11177, showGui);
	}
	
	public Carla(String carlaSettings, int tcpPort, int udpPort, boolean showGui) throws IOException {
		// for Metronome: carla/FluidMetronome.carxp on ports 11198, 11199
		
		this.port = udpPort;
		this.settings = carlaSettings;
		
		ArrayList<String> cmds = new ArrayList<>();
		cmds.add(CARLA_SHELL_COMMAND);
		if (!showGui) cmds.add("--no-gui");
		cmds.add(carlaSettings);
    	ProcessBuilder builder = new ProcessBuilder(cmds);
    	builder.environment().put("CARLA_OSC_TCP_PORT", "" + tcpPort);
    	builder.environment().put("CARLA_OSC_UDP_PORT", "" + udpPort);
    	process = builder.start();
    	
    	prefix = (isFirst) ? "/Carla/" : "/Carla_01/";
    	isFirst = false;
    	
    	out = new OSCPortOut(InetAddress.getLocalHost(), udpPort);
    	log.debug("Carla created. " + carlaSettings);
	}

	/** @return true if message sent 
	 * @throws IOException 
	 * @throws OSCSerializeException */
	public void setVolume(int pluginIdx, float gain) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(gain);
		String address = prefix + pluginIdx + "/set_volume";
		send(address, param);
	}

	/** @return true if message sent 
	 * @throws IOException 
	 * @throws OSCSerializeException */
	public void setActive(int pluginIdx, int tOrF) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(tOrF);
		String address = prefix + pluginIdx + "/set_active";
		send(address, param);
	}

	public void setDryWet(int pluginIdx, float val) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(val);
		String address = prefix + pluginIdx + "/set_drywet";
		send(address, param);
	}
	
	/** @return true if message sent 
	 * @throws IOException 
	 * @throws OSCSerializeException */
	public void setParameterValue(int pluginIdx, int paramIdx, float value) throws OSCSerializeException, IOException {
		String address = prefix + pluginIdx + "/set_parameter_value";
		List<Object> param = new ArrayList<>();
		param.add(paramIdx);
		param.add(value);
		send(address, param);
	}
	
	public void send(String address, List<Object> params) throws OSCSerializeException, IOException {
		log.debug(OSC + port + " " + address + " --> " + Arrays.toString(params.toArray()));
		if (!out.isConnected())
			out.connect();
		out.send(new OSCMessage(address, params));
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
		if (process != null) process.destroyForcibly();
		log.debug("carla disposed.");
		instance = null;
		out = null;
	}

	@Override public void properties(HashMap<String, Object> props) { }

}

//public static void main(String args[]) {
//	try {
//		log.info("testing OSC server at: "  + InetAddress.getLocalHost().toString() + " : " + 11177);
//		OSCPortOut outport = new OSCPortOut(InetAddress.getLocalHost(), 11177);
//		outport.connect();
//		log.warn("connected : " + outport.isConnected());
//		List<Object> param = new ArrayList<>();
//		
//		int pluginIndex = 0;
//		param.add(1);
//		String address = "/Carla/" + pluginIndex + "/set_active";
//		
//		log.debug(OSC + address + " --> " + Arrays.toString(param.toArray()));
//		try {
//			outport.send(new OSCMessage(address, param));
//			log.warn("success.");
//		} catch (IOException | OSCSerializeException e) {
//			log.error(e.getMessage(), e);
//		}
//		outport.disconnect();
//	} catch (Exception e) {
//		log.error(e.getMessage(), e);
//	}}